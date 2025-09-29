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
package org.thingsboard.server.service.scheduler.processing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.SchedulerEvent;

/**
 * Classe abstraite de base pour tous les jobs du planificateur
 */
@Slf4j
public abstract class AbstractSchedulerJob {

    /**
     * Exécute la tâche planifiée
     *
     * @param event L'événement du planificateur à exécuter
     * @throws Exception En cas d'erreur lors de l'exécution
     */
    public abstract void execute(SchedulerEvent event) throws Exception;

    /**
     * Retourne le type de la tâche
     *
     * @return Le type de tâche sous forme de chaîne
     */
    public abstract String getType();

    /**
     * Vérifie si cette implémentation peut gérer cet événement
     *
     * @param event L'événement à vérifier
     * @return true si ce job peut traiter l'événement, false sinon
     */
    public boolean supports(SchedulerEvent event) {
        return getType().equals(event.getType());
    }
}