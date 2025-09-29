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
package org.thingsboard.server.service.scheduler.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.service.scheduler.processing.AbstractSchedulerJob;
import org.thingsboard.server.service.scheduler.processing.AttributeUpdateSchedulerJob;
import org.thingsboard.server.service.scheduler.processing.CronSchedulerJob;
import org.thingsboard.server.service.scheduler.processing.DeviceRpcSchedulerJob;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SchedulerCommandQueue {

    private final BlockingQueue<SchedulerEvent> queue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor executor;

    private final List<AbstractSchedulerJob> jobs;

    public SchedulerCommandQueue(
            @Autowired AttributeUpdateSchedulerJob attributeUpdateJob,
            @Autowired DeviceRpcSchedulerJob deviceRpcJob,
            @Autowired CronSchedulerJob cronJob) {

        this.jobs = List.of(attributeUpdateJob, deviceRpcJob, cronJob);

        // Configuration du pool de threads pour l'exécution des tâches
        this.executor = new ThreadPoolExecutor(
                5, // Nombre de threads de base
                20, // Nombre max de threads
                10, // Temps de vie des threads inactifs
                TimeUnit.SECONDS, // Unité de temps
                new LinkedBlockingQueue<>(), // File d'attente
                r -> {
                    Thread t = new Thread(r, "scheduler-worker-" + r.hashCode());
                    t.setDaemon(true);
                    return t;
                }
        );

        // Démarrer le thread de traitement
        new Thread(this::processQueue, "scheduler-queue-processor").start();
    }

    public void put(SchedulerEvent event) throws InterruptedException {
        queue.put(event);
        logThreadPoolStats();
    }
    public void clear() throws InterruptedException {
        queue.clear();
    }

    private void processQueue() {
        while (true) {
            try {
                SchedulerEvent event = queue.take(); // Attendre un événement
                executor.execute(() -> processEvent(event));
            } catch (InterruptedException e) {
                log.error("Queue processor interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing queue", e);
            }
        }
    }

    private void processEvent(SchedulerEvent event) {
        try {
            // Trouver le job approprié pour cet événement
            AbstractSchedulerJob job = findSupportingJob(event);
            if (job != null) {
                log.debug("Processing event [{}] with job [{}]", event.getId(), job.getClass().getSimpleName());
                job.execute(event);
            } else {
                log.warn("No job found for event type [{}]", event.getType());
            }
        } catch (Exception e) {
            log.error("Error executing scheduled event [{}]", event, e);
        }
    }

    private AbstractSchedulerJob findSupportingJob(SchedulerEvent event) {
        return jobs.stream()
                .filter(job -> job.supports(event))
                .findFirst()
                .orElse(null);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void logThreadPoolStats() {
        log.info("[SchedulerCommandQueue] Pool size: {}, Active threads: {}, Completed tasks: {}, Total tasks: {}, Queue size: {}",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                executor.getQueue().size());
    }

}