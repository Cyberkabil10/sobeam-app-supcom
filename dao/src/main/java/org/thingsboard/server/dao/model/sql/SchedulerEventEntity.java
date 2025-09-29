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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.SCHEDULER_TABLE_NAME)
public class SchedulerEventEntity extends BaseVersionedEntity<SchedulerEvent> {

    @Column(name = ModelConstants.USER_SCHEDULER_USER_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.SCHEDULER_USER_ID_PROPERTY)
    private UUID userId;

    @Column(name = ModelConstants.SCHEDULER_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.SCHEDULER_TYPE_PROPERTY)
    private String type;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.SCHEDULER_CONFIGURATION_PROPERTY, columnDefinition = "jsonb")
    private JsonNode configuration;

     private boolean enable ;

    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.SCHEDULER_TIME_PROPERTY, columnDefinition = "jsonb")
    private JsonNode scheduler;

    public SchedulerEventEntity(SchedulerEvent schedulerEvent) {
        this.setUuid(schedulerEvent.getUuidId());
        this.name = schedulerEvent.getName();
        this.configuration = schedulerEvent.getConfiguration();
        this.tenantId = schedulerEvent.getTenantId().getId();
        this.userId = schedulerEvent.getUserId().getId();
        this.type = schedulerEvent.getType();
        this.scheduler = schedulerEvent.getScheduler();
        this.enable = schedulerEvent.getEnabled();

    }

    @Override
    public SchedulerEvent toData() {
        SchedulerEvent schedulerEvent = new SchedulerEvent(new SchedulerEventId(id));
        schedulerEvent.setName(name);
        schedulerEvent.setConfiguration(configuration);
        schedulerEvent.setTenantId(new TenantId(tenantId));
        schedulerEvent.setUserId( new UserId(userId) );
        schedulerEvent.setCreatedTime(createdTime);
        schedulerEvent.setType(type);
        schedulerEvent.setEnabled(enable);
        if (scheduler != null) {
            schedulerEvent.setScheduler(scheduler);
        }
        return schedulerEvent;
    }


}
