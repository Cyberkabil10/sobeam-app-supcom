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

import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

/**
 * Interface pour le service de planification de ThingsBoard
 */
public interface TbSchedulerService {


    /**
     * Récupère les événements planifiés pour un tenant
     *
     * @param tenantId ID du tenant
     * @param pageLink Paramètres de pagination
     * @return Liste paginée des événements
     */
    PageData<SchedulerEvent> getScheduledEventsByTenant(TenantId tenantId, PageLink pageLink);

    /**
     * Récupère les événements planifiés pour un tenant
     *
     * @param tenantId ID du tenant
     * @param userId ID du  user
     * @param pageLink Paramètres de pagination
     * @return Liste paginée des événements
     */
    PageData<SchedulerEvent> getScheduledEventsByUser(TenantId tenantId, UserId userId , PageLink pageLink);


    /**
     * Enregistre un événement dans le planificateur
     *
     * @param tenantId ID du tenant
     * @param schedulerEvent Événement à enregistrer
     * @return Événement enregistré
     * @throws Exception En cas d'erreur lors de l'enregistrement
     */
    SchedulerEvent saveScheduler(TenantId tenantId, SchedulerEvent schedulerEvent) throws Exception;

    /**
     * Supprime un événement du planificateur
     *
     * @param schedulerEvent Événement à supprimer
     * @param user Utilisateur effectuant la suppression
     */
    void deleteScheduler(SchedulerEvent schedulerEvent, User user);

    // Dans TbSchedulerService.java

    /**
     * Planifie un événement avec une expression cron
     *
     * @param tenantId ID du tenant
     * @param entityId ID de l'entité
     * @param cronExpression Expression cron pour la planification
     * @param timezone Fuseau horaire pour l'évaluation de l'expression cron
     * @param endTime Timestamp de fin en millisecondes (0 pour aucune fin)
     * @param event Événement à planifier
     */
    void scheduleCron(TenantId tenantId, EntityId entityId, String cronExpression, String timezone,long startTime, long endTime, SchedulerEvent event);
}