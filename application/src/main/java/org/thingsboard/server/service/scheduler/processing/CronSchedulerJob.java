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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

/**
 * Job pour exécuter différentes tâches en fonction du type d'action
 */
@Slf4j
public class CronSchedulerJob extends AbstractSchedulerJob {

    private final AttributeUpdateSchedulerJob attributeUpdateJob;
    private final DeviceRpcSchedulerJob deviceRpcJob;

    public CronSchedulerJob(AttributesService attributesService,
                            TimeseriesService timeseriesService,
                            TbClusterService clusterService,
                            RuleEngineRpcService rpcService) {
        this.attributeUpdateJob = new AttributeUpdateSchedulerJob(attributesService, timeseriesService,clusterService);
        this.deviceRpcJob = new DeviceRpcSchedulerJob(rpcService , clusterService);
    }

    @Override
    public void execute(SchedulerEvent event) throws Exception {
        JsonNode config = event.getConfiguration();
        String actionType = config.has("actionType") ? config.get("actionType").asText() : null;

        if (actionType == null) {
            log.error("No actionType specified in cron job configuration");
            return;
        }

        log.debug("Executing cron job with action type: {}", actionType);

        // Exécuter l'action appropriée selon le type
        switch (actionType) {
            case "POST_ATTRIBUTES_REQUEST":
                attributeUpdateJob.execute(event);
                break;
            case "RPC_CALL_FROM_SERVER_TO_DEVICE":
                deviceRpcJob.execute(event);
                break;
            default:
                log.error("Unknown action type: {}", actionType);
        }
    }

    @Override
    public String getType() {
        return "CRON_JOB";
    }
}