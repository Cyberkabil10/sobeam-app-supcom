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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Schema
@Data
@ToString(exclude = {"schedulerBytes", "configurationBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class SchedulerEvent extends BaseData<SchedulerEventId> implements HasTenantId, HasVersion, ExportableEntity<SchedulerEventId> {

    private static final long serialVersionUID = 2628320657987010348L;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Schema(description = "JSON object with Tenant Id that owns the scheduler.")
    private TenantId tenantId;

    @Schema(description = "JSON object with User Id that owns the scheduler.")
    private UserId userId;

    @NoXss
    @Length(fieldName = "name", max = 50)
    @Schema(description = "Unique scheduler name in scope of Tenant.", example = "new scheduler")
    private String name;

    @NoXss
    @Length(fieldName = "configuration", max = 500)
    @Schema(description = "JSON object with scheduler configuration", implementation = JsonNode.class)
    private JsonNode configuration;

    @Schema(description = "JSON object with scheduler time", implementation = JsonNode.class)
    private JsonNode scheduler;



    @Schema(description = "Indicates if the scheduler is enabled")
    private boolean enabled;

    @NoXss
    @Length(fieldName = "type", max = 50)
    @Schema(description = "Scheduler type.")
    private String type;

    private SchedulerEventId externalId;

    @JsonIgnore
    private byte[] configurationBytes;

    @JsonIgnore
    private byte[] schedulerBytes;

    public SchedulerEvent() {
        super();
    }

    public SchedulerEvent(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEvent(SchedulerEvent event) {
        super(event);
        this.tenantId = event.getTenantId();
        this.userId = event.getUserId();
        this.name = event.getName();
        this.type = event.getType();
        this.enabled = event.getEnabled();
        this.configuration = event.getConfiguration();
        this.scheduler = event.getScheduler();
        this.externalId = event.getExternalId();
    }

    @Override
    public SchedulerEventId getId() {
        return super.getId();
    }

    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public JsonNode getConfiguration() {
        if (configuration != null) {
            return configuration;
        } else if (configurationBytes != null) {
            try {
                configuration = mapper.readTree(new ByteArrayInputStream(configurationBytes));
                return configuration;
            } catch (IOException e) {
                log.warn("Can't deserialize configuration: ", e);
                return mapper.createObjectNode();
            }
        }
        return mapper.createObjectNode();
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
        try {
            this.configurationBytes = configuration != null ? mapper.writeValueAsBytes(configuration) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize configuration: ", e);
        }
    }

    public JsonNode getScheduler() {
        if (scheduler != null) {
            return scheduler;
        } else if (schedulerBytes != null) {
            try {
                scheduler = mapper.readTree(new ByteArrayInputStream(schedulerBytes));
                return scheduler;
            } catch (IOException e) {
                log.warn("Can't deserialize scheduler: ", e);
                return mapper.createObjectNode();
            }
        }
        return mapper.createObjectNode();
    }

    public void setScheduler(JsonNode scheduler) {
        this.scheduler = scheduler;
        try {
            this.schedulerBytes = scheduler != null ? mapper.writeValueAsBytes(scheduler) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize scheduler: ", e);
        }
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Long getVersion() {
        return 0L;
    }
}