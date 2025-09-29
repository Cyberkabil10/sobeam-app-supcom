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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Schema
@Data
@ToString(exclude = {"permissionsBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Role extends BaseData<RoleId> implements  HasTenantId, HasVersion , ExportableEntity<RoleId>{

    private static final long serialVersionUID = 2628320657987010348L;

    @Schema(description = "JSON object with Tenant Id that owns the role.")
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name", max = 50)
    @Schema(description = "Unique Role Name in scope of Tenant.", example = "Administrator")
    private String name;

    @NoXss
    @Length(fieldName = "description", max = 500)
    @Schema(description = "Role description.")
    private String description;

    @NoXss
    @Length(fieldName = "type", max = 50)
    @Schema(description = "Role type.")
    private String type;

    @Schema(description = "JSON object with role permissions", implementation = JsonNode.class)
    private  JsonNode permissions;


    @Getter
    @Setter
    private RoleId externalId;

    private  List<String> assignedUserIds;

    @JsonIgnore
    private byte[] permissionsBytes;

    public Role() {
        super();
    }

    public Role(RoleId id) {
        super(id);
    }


    public Role(Role role) {
        super(role);
        this.tenantId = role.getTenantId();
        this.name = role.getName();
        this.type = role.getType();
        this.description = role.getDescription();
        this.setPermissions(role.getPermissions());
        this.setAssignedUserIds(role.getAssignedUserIds());
    }


    @Schema(description = "JSON object with the role Id. " +
            "Specify this field to update the role. " +
            "Referencing non-existing role Id will cause error. " +
            "Omit this field to create new role.")
    @Override
    public RoleId getId() {
        return super.getId();
    }



    @Schema(description = "Timestamp of the role creation, in milliseconds",
            example = "1609459200000",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    public JsonNode getPermissions() {
        if (permissions != null) {
            return permissions;
        } else {
            if (permissionsBytes != null) {
                try {
                    permissions = mapper.readTree(new ByteArrayInputStream(permissionsBytes));
                } catch (IOException e) {
                    log.warn("Can't deserialize role permissions: ", e);
                    return mapper.createObjectNode();
                }
                return permissions;
            } else {
                return mapper.createObjectNode();
            }
        }
    }

    public void setPermissions(JsonNode data) {
        this.permissions = data;
        try {
            this.permissionsBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize role permissions: ");
        }
    }



    @Override
    public Long getVersion() {
        return 0L;
    }
}