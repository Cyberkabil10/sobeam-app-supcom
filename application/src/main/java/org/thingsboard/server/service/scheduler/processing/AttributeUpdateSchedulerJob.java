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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;


/**
 * Job pour mettre à jour les attributs d'un appareil
 */
@Slf4j
public class AttributeUpdateSchedulerJob extends AbstractSchedulerJob {

    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private  final TbClusterService clusterService;

    public AttributeUpdateSchedulerJob(AttributesService attributesService, TimeseriesService timeseriesService , TbClusterService clusterService) {
        this.attributesService = attributesService;
        this.timeseriesService = timeseriesService;
        this.clusterService = clusterService;
    }

    @Override
    public void execute(SchedulerEvent event) throws Exception {
        JsonNode config = event.getConfiguration();

        // Extraction du corps du message
        JsonNode msgBody = config.get("msgBody");

        // Extraction du scope (s'il existe)
        String scope = config.has("metadata") && config.get("metadata").has("scope")
                ? config.get("metadata").get("scope").asText()
                : AttributeScope.SERVER_SCOPE.name(); // fallback par défaut

        // Extraction de l'ID de l'appareil à partir de originatorId -> singleEntity -> id
        JsonNode originatorIdNode = config.path("originatorId").path("singleEntity").path("id");
        JsonNode entityTypeNode = config.path("originatorId").path("singleEntity").path("entityType");

        EntityId entityId = null;
        String entityType = entityTypeNode.asText();
        switch (entityType) {
            case "DEVICE":
                entityId = DeviceId.fromString(originatorIdNode.asText());
                break;
            case "ASSET":
                entityId = AssetId.fromString(originatorIdNode.asText());
                break;
            default:
                log.warn("Unsupported entity type: {}", entityType);
                return;
        }

        // Extraction de l'ID du tenant
        TenantId tenantId = event.getTenantId();

        // Création de la liste d'attributs à mettre à jour
        List<AttributeKvEntry> attributes = new ArrayList<>();
        long ts = System.currentTimeMillis();
        msgBody.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            AttributeKvEntry kvEntry;
            if (value.isTextual()) {
                kvEntry = new BaseAttributeKvEntry(new StringDataEntry(key, value.asText()), ts);
            } else if (value.isBoolean()) {
                kvEntry = new BaseAttributeKvEntry(new BooleanDataEntry(key, value.asBoolean()), ts);
            } else if (value.isInt() || value.isLong()) {
                kvEntry = new BaseAttributeKvEntry(new LongDataEntry(key, value.asLong()), ts);
            } else if (value.isDouble() || value.isFloat()) {
                kvEntry = new BaseAttributeKvEntry(new DoubleDataEntry(key, value.asDouble()), ts);
            } else {
                kvEntry = new BaseAttributeKvEntry(new StringDataEntry(key, value.toString()), ts);
            }

            attributes.add(kvEntry);
        });


        // Notifier le Rule Engine
        try {
            TbMsgType msgType = TbMsgType.POST_ATTRIBUTES_REQUEST;
            TbMsgMetaData md = new TbMsgMetaData();
            md.putValue("scope", scope);
            TbMsg msg = TbMsg.newMsg()
                    .type(msgType)
                    .originator(entityId).metaData(md)
                    .data(JacksonUtil.writeValueAsString(msgBody))
                    .build();

            clusterService.pushMsgToRuleEngine(tenantId, entityId, msg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.trace("[{}][{}] Pushed message to rule engine: {}", tenantId, msg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("[{}][{}] Failed to push message to rule engine", tenantId,t);
                }
            });
        } catch (Exception e) {
            log.warn("[{}][{}] Exception while pushing message to rule engine", tenantId, entityId, e);
        }
    }



    @Override
    public String getType() {
        return "updateAttribute";
    }
}