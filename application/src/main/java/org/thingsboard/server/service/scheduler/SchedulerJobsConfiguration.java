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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.scheduler.processing.AttributeUpdateSchedulerJob;
import org.thingsboard.server.service.scheduler.processing.CronSchedulerJob;
import org.thingsboard.server.service.scheduler.processing.DeviceRpcSchedulerJob;

/**
 * Configuration des jobs du planificateur
 */
@Configuration
public class SchedulerJobsConfiguration {

    /**
     * Configuration du job pour les appels RPC aux appareils
     */
    @Bean
    public DeviceRpcSchedulerJob deviceRpcSchedulerJob(RuleEngineRpcService rpcService , TbClusterService clusterService)  {
        return new DeviceRpcSchedulerJob(rpcService,clusterService );
    }
    /**
     * Configuration du job pour la mise à jour des attributs
     */
    @Bean
    public AttributeUpdateSchedulerJob attributeUpdateSchedulerJob(
            AttributesService attributesService,
            TbClusterService clusterService,
            TimeseriesService timeseriesService) {
        return new AttributeUpdateSchedulerJob(attributesService, timeseriesService,clusterService) ;
    }

    /**
     * Configuration du job cron générique
     */
    @Bean
    public CronSchedulerJob cronSchedulerJob(
            AttributesService attributesService,
            TimeseriesService timeseriesService,
            TbClusterService clusterService,
            RuleEngineRpcService rpcService) {
        return new CronSchedulerJob(attributesService, timeseriesService,clusterService, rpcService);
    }
}