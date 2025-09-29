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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.common.data.id.GroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.ENTITY_GROUP_TABLE_NAME)
public class GroupEntity extends BaseVersionedEntity<Group> {


    @Column(name = ModelConstants.USER_ROLE_USER_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ROLE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ROLE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.ROLE_TYPE_PROPERTY)
    private String type;

    @Column(name = ModelConstants.ENTITY_GROUP_USER_ID_GENERATED)
    private UUID userIdGenerated;

    @Column(name = ModelConstants.ENTITY_GROUP_USER_ID_AFFECTED)
    private UUID userIdAffected;


    public GroupEntity(Group group) {
        this.setUuid(group.getUuidId());
        this.name = group.getName();
        this.description = group.getDescription();
        this.tenantId = group.getTenantId().getId();
        this.type = group.getType();
        this.userIdAffected = group.getUserIdAffected().getId();
        this.userIdGenerated = group.getUserIdGenerated().getId();


    }

    @Override
    public Group toData() {
        Group group = new Group(new GroupId(id));
        group.setName(name);
        group.setDescription(description);
        group.setTenantId(new TenantId(tenantId));
        group.setCreatedTime(createdTime);
        group.setType(type);
        group.setUserIdAffected(new UserId(userIdAffected));
        group.setUserIdGenerated(new UserId(userIdGenerated));
        return group;
    }

}
