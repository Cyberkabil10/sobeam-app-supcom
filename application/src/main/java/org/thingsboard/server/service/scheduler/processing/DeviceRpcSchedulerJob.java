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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcRequest;
import org.thingsboard.rule.engine.api.RuleEngineDeviceRpcResponse;
import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.device.profile.TypeDataMethod;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Job pour envoyer des requêtes RPC aux appareils
 */
@Slf4j
public class DeviceRpcSchedulerJob extends AbstractSchedulerJob {

    private final RuleEngineRpcService rpcService;
    private  final TbClusterService clusterService;

    public DeviceRpcSchedulerJob(RuleEngineRpcService rpcService , TbClusterService clusterService)  {
        this.rpcService = rpcService;
        this.clusterService = clusterService;
    }

    @Override
    public void execute(SchedulerEvent event) throws Exception {
        JsonNode config = event.getConfiguration();

        // ----- Extraction du message -----
        JsonNode msgBody = config.get("msgBody");
        String method = msgBody.get("method").asText();

        // ----- Extraction des métadonnées -----
        JsonNode metadata = config.get("metadata");
        long timeout = metadata.has("timeout") ? metadata.get("timeout").asLong() : 5000;
        boolean persistent = metadata.has("persistent") && metadata.get("persistent").asBoolean();
        boolean oneway = metadata.has("oneway") && metadata.get("oneway").asBoolean();

        // ----- Originator -----
        JsonNode originatorIdNode = config.path("originatorId").path("singleEntity").path("id");
        DeviceId deviceId = new DeviceId(UUID.fromString(originatorIdNode.asText()));

        TenantId tenantId = event.getTenantId();

        // ----- RPC Request -----
        UUID requestUUID = UUID.randomUUID();
        int requestId = requestUUID.hashCode();



        // ----- Notification Rule Engine -----
        try {
            ObjectNode entityNode = JacksonUtil.newObjectNode();
            entityNode.put("method", method);
            entityNode.put("params",  msgBody.get("params"));

            TbMsgMetaData md = new TbMsgMetaData();
            md.putValue("requestUUID", requestUUID.toString());
            md.putValue("originServiceId", getServiceId());
            md.putValue("oneway", Boolean.toString(oneway));
            md.putValue("persistent", Boolean.toString(persistent));
            md.putValue("timeout", String.valueOf(timeout));

            TbMsg tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.RPC_CALL_FROM_SERVER_TO_DEVICE)
                    .originator(deviceId)
                    .metaData(md)
                    .dataType(TbMsgDataType.JSON)
                    .data(JacksonUtil.toString(entityNode))
                    .build();

            clusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.trace("[{}][{}] Pushed RPC message to Rule Engine: {}", tenantId, deviceId, tbMsg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to push RPC message to Rule Engine", tenantId, deviceId, t);
                }
            });
        } catch (Exception e) {
            log.warn("[{}][{}] Exception while pushing RPC message to Rule Engine", tenantId, deviceId, e);
        }
    }

    private String getServiceId() {
        // À implémenter selon votre contexte, par exemple:
        return "scheduler-service-" + UUID.randomUUID();
    }

    @Override
    public String getType() {
        return "sendRpcRequest";
    }
}