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
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;


@Data
@NoArgsConstructor
@Entity
@Table(name = ModelConstants.ROLE_TABLE_NAME)
public class RoleEntity extends BaseVersionedEntity<Role> {


    @Column(name = ModelConstants.USER_ROLE_USER_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.ROLE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.ROLE_DESCRIPTION_PROPERTY)
    private String description;

    @Column(name = ModelConstants.ROLE_TYPE_PROPERTY)
    private String type;


    @Convert(converter = JsonConverter.class)
    @JdbcType(PostgreSQLJsonPGObjectJsonbType.class)
    @Column(name = ModelConstants.ROLE_PERMISSION_PROPERTY, columnDefinition = "jsonb")
    private JsonNode permissions;

    public RoleEntity(Role role) {
        this.setUuid(role.getUuidId());
        this.name = role.getName();
        this.description = role.getDescription();
        this.tenantId = role.getTenantId().getId();
        this.type = role.getType();
        if (role.getPermissions() != null) {
            this.permissions = role.getPermissions();
        }
    }

    @Override
    public Role toData() {
        Role role = new Role(new RoleId(id));
        role.setName(name);
        role.setDescription(description);
        role.setTenantId(new TenantId(tenantId));
        role.setCreatedTime(createdTime);
        role.setType(type);
        if (permissions != null) {
            role.setPermissions(permissions);
        }
        return role;
    }


}
