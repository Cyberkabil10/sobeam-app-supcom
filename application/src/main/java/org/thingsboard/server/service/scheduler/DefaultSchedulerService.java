/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.scheduler.SchedulerService;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.scheduler.queue.SchedulerCommandQueue;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Calendar;
import java.util.TimeZone;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.thingsboard.server.dao.user.UserServiceImpl.INCORRECT_TENANT_ID;

/**
 * Implémentation du service de planification pour ThingsBoard
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultSchedulerService extends AbstractTbEntityService implements TbSchedulerService {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private SchedulerCommandQueue schedulerCommandQueue;

    @Value("${scheduler.pool_size:3}")
    private int schedulerPoolSize;

    @Value("${scheduler.enabled:true}")
    private boolean schedulerEnabled;

    private ThreadPoolTaskScheduler scheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        if (schedulerEnabled) {
            this.scheduler = new ThreadPoolTaskScheduler();
            this.scheduler.setPoolSize(schedulerPoolSize);
            this.scheduler.setThreadNamePrefix("tb-scheduler-");
            this.scheduler.setRemoveOnCancelPolicy(true);
            this.scheduler.initialize();
            log.info("Started scheduler with pool size [{}]", schedulerPoolSize);
            loadScheduledEvents();
        } else {
            log.info("Scheduler is disabled");
        }
    }

    @PreDestroy
    public void destroy() {
        if (schedulerEnabled && scheduler != null) {
            for (ScheduledFuture<?> task : scheduledTasks.values()) {
                task.cancel(false);
            }
            scheduler.shutdown();
            log.info("Stopped scheduler");
        }
    }

    @Override
    public PageData<SchedulerEvent> getScheduledEventsByTenant(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findSchedulerByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return schedulerService.findSchedulerByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<SchedulerEvent> getScheduledEventsByUser(TenantId tenantId, UserId userId, PageLink pageLink) {
        log.trace("Executing findSchedulerByUserID, UserId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return schedulerService.findSchedulerByUserId(tenantId, userId, pageLink);    }

    @Override
    public SchedulerEvent saveScheduler(TenantId tenantId, SchedulerEvent schedulerEvent) throws Exception {
        ActionType actionType = schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
            // Si c'est une mise à jour, supprimer d'abord la tâche existante
            if (actionType == ActionType.UPDATED) {
                cancelExistingTask(tenantId, schedulerEvent);
            }

            SchedulerEvent savedEvent = checkNotNull(schedulerService.saveScheduler(tenantId, schedulerEvent));

            // Planifier la nouvelle tâche
            scheduleEvent(savedEvent);

            return savedEvent;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void deleteScheduler(SchedulerEvent schedulerEvent, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = schedulerEvent.getTenantId();
        SchedulerEventId schedulerEventId = schedulerEvent.getId();

        // Annuler la tâche avant de supprimer de la base
        cancelExistingTask(tenantId, schedulerEvent);

        schedulerService.deleteScheduler(tenantId, schedulerEventId);
    }

    private void cancelExistingTask(TenantId tenantId, SchedulerEvent event) {
        JsonNode originatorIdNode = event.getConfiguration().path("originatorId").path("singleEntity").path("id");
        EntityId entityId = DeviceId.fromString(originatorIdNode.asText());
        String taskId = getTaskId(tenantId, entityId, event.getId());

        ScheduledFuture<?> existingTask = scheduledTasks.get(taskId);
        if (existingTask != null) {
            // Annuler la tâche sans interrompre si elle est en cours d'exécution
            existingTask.cancel(true);
            scheduledTasks.remove(taskId);
            log.debug("Cancelled existing task [{}]", taskId);
        }
    }

    private void loadScheduledEvents() {
        log.info("Loading scheduled events");
        try {
            schedulerService.findAllSchedulerEvent().forEach(event -> {
                try {

                    if (!event.getEnabled()) {
                        log.info("Skipping disabled event [{}]", event.getId());
                        return; // Ne pas exécuter cet événement désactivé
                    }
                    TenantId tenantId = event.getTenantId();
                    JsonNode originatorIdNode = event.getConfiguration().path("originatorId").path("singleEntity").path("id");
                    EntityId entityId = DeviceId.fromString(originatorIdNode.asText());
                    if (event.getScheduler() != null) {
                        JsonNode schedulerConfig = event.getScheduler();
                        String timezone = schedulerConfig.has("timezone") ? schedulerConfig.get("timezone").asText() : null;
                        long startTime = schedulerConfig.has("startTime") ? schedulerConfig.get("startTime").asLong() : 0;
                        long endTime = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("endsOn").asLong() : 0;
                        long cronExpressionBrut = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("repeatInterval").asLong() : null;
                        String repeatUnit = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("timeUnit").asText() : "ans";
                        String   cronExpression = convertToCronExpression(startTime, cronExpressionBrut, repeatUnit);

                        boolean repeatEnabled = schedulerConfig.has("repeatEnabled") && schedulerConfig.get("repeatEnabled").asBoolean();

                        // Si une expression cron est directement fournie
                        if (cronExpression != null && !cronExpression.isEmpty()) {
                            scheduleCron(tenantId, entityId, cronExpression, timezone, startTime,endTime, event);
                        }
                        // Si répétition est activée, convertir en expression cron
                        else if (repeatEnabled && cronExpressionBrut > 0 && repeatUnit != null) {
                            if (cronExpression != null) {
                                scheduleCron(tenantId, entityId, cronExpression, timezone,startTime, endTime, event);
                            }
                        }
                        // Sinon, planification ponctuelle
                        else if (startTime > 0) {
                                // L'heure est déjà passée, exécution immédiate si nécessaire
                                schedulerCommandQueue.put(event);
                            }
                           }
                } catch (Exception e) {
                    log.error("Failed to schedule event [{}]: {}", event.getId(), e.getMessage(), e);
                }
            });
            log.info("Loaded scheduled events");
        } catch (Exception e) {
            log.error("Failed to load scheduled events", e);
        }
    }

    private String convertToCronExpression(long startTime, long interval, String unit) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);

        switch (unit.toLowerCase()) {
            case "seconds":
                return String.format("*/%d * * * * ?", interval);
            case "minutes":
                return String.format("0 */%d * * * ?", interval);
            case "hours":
                return String.format("0 0 */%d * * ?", interval);
            case "days":
                return String.format("0 0 0 */%d * ?", interval);
            case "weeks":
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                // Convertir en format cron (1=Dimanche, 7=Samedi)
                return String.format("0 0 0 ? * %d", dayOfWeek);
            case "ans":
                int second = cal.get(Calendar.SECOND);
                int minute = cal.get(Calendar.MINUTE);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based

                // Génère une CRON annuelle
                return String.format("%d %d %d %d %d ? *", second, minute, hour, dayOfMonth, month);

            default:
                log.error("Unknown repeat unit: {}", unit);
                return null;
        }
    }

    private boolean isMyPartition(TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId).isMyPartition();
    }

    private String getTaskId(TenantId tenantId, EntityId entityId, EntityId eventId) {
        return tenantId + "_" + entityId + "_" + eventId;
    }

    @Override
    public void scheduleCron(TenantId tenantId, EntityId entityId, String cronExpression, String timezone,long startTime, long endTime, SchedulerEvent event) {
        if (!schedulerEnabled) {
            return;
        }
/*
        if (!isMyPartition(tenantId, entityId)) {
            log.trace("Event for entity [{}] is not on this partition. Ignoring.", entityId);
            return;
        }
*/
        String taskId = getTaskId(tenantId, entityId, event.getId());
        event.setTenantId(tenantId);

        // Sauvegarde de l'événement
        //  schedulerService.saveScheduler(tenantId, event);

        // Création d'un déclencheur cron avec le fuseau horaire spécifié
        TimeZone tz = timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault();
        CronTrigger cronTrigger = new CronTrigger(cronExpression, tz);

        // Planification de la tâche avec l'expression cron
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            long now = System.currentTimeMillis();
            // Vérifier si l'heure de fin est dépassée
            if (endTime > 0 && now > endTime) {
                //       schedulerService.deleteScheduler(tenantId, event.getId());
                scheduledTasks.remove(taskId);
                return;
            }

            try {
                if(startTime<=now) {

                    schedulerCommandQueue.put(event);
                }
            } catch (Exception e) {
                log.error("Failed to process cron scheduled event [{}]", event, e);
            }
        }, cronTrigger);

        scheduledTasks.put(taskId, task);
        log.debug("Scheduled cron task [{}] for tenant [{}], entity [{}] with expression [{}], timezone [{}], end time [{}]",
                event.getId(), tenantId, entityId, cronExpression, timezone,
                endTime > 0 ? Instant.ofEpochMilli(endTime) : "indefinite");
    }

    private void scheduleEvent(SchedulerEvent event) {
        if (!event.getEnabled()) {
            log.info("Not scheduling disabled event [{}]", event.getId());
            return;
        }

        TenantId tenantId = event.getTenantId();
        JsonNode originatorIdNode = event.getConfiguration().path("originatorId").path("singleEntity").path("id");
        EntityId entityId = DeviceId.fromString(originatorIdNode.asText());

        if (event.getScheduler() != null) {
            JsonNode schedulerConfig = event.getScheduler();
            String timezone = schedulerConfig.has("timezone") ? schedulerConfig.get("timezone").asText() : null;
            long startTime = schedulerConfig.has("startTime") ? schedulerConfig.get("startTime").asLong() : 0;
            long endTime = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("endsOn").asLong() : 0;
            long cronExpressionBrut = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("repeatInterval").asLong() : 0;
            String repeatUnit = schedulerConfig.has("repeat") ? schedulerConfig.get("repeat").get("timeUnit").asText() : "months";
            String cronExpression = convertToCronExpression(startTime, cronExpressionBrut, repeatUnit);

            boolean repeatEnabled = schedulerConfig.has("repeatEnabled") && schedulerConfig.get("repeatEnabled").asBoolean();

            if (cronExpression != null && !cronExpression.isEmpty()) {
                scheduleCron(tenantId, entityId, cronExpression, timezone, startTime,endTime, event);
            } else if (repeatEnabled && cronExpressionBrut > 0 && repeatUnit != null) {
                if (cronExpression != null) {
                    scheduleCron(tenantId, entityId, cronExpression, timezone,startTime, endTime, event);
                }
            } else if (startTime > 0) {

                    // Exécution immédiate si l'heure est passée
                    try {
                        schedulerCommandQueue.put(event);
                    } catch (InterruptedException e) {
                        log.error("Failed to immediately execute event [{}]", event.getId(), e);
                        Thread.currentThread().interrupt();
                    }
                            }
        }
    }

}

