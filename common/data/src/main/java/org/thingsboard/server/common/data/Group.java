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


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.GroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Group extends BaseData<GroupId> implements  HasTenantId , HasVersion , ExportableEntity<GroupId>{

    private static final long serialVersionUID = 2628320657987010348L;

    @Schema(description = "JSON object with Tenant Id that owns the Group.")
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "name", max = 50)
    @Schema(description = "Unique Group Name in scope of Tenant.", example = "Administrator")
    private String name;

    @NoXss
    @Length(fieldName = "description", max = 500)
    @Schema(description = "Group description.")
    private String description;

    @NoXss
    @Length(fieldName = "type", max = 50)
    @Schema(description = "Group type.")
    private String type;

    @Schema(description = "JSON object with the user-generated ID that owns the group.")
    private UserId userIdGenerated;

    @Schema(description = "JSON object with the user-affected ID that the group.")
    private UserId userIdAffected;

    public Group() {
        super();
    }

    public Group(GroupId id) {
        super(id);
    }


    public Group(Group group) {
        super(group);
        this.tenantId = group.getTenantId();
        this.name = group.getName();
        this.type = group.getType();
        this.description = group.getDescription();
        this.userIdAffected = group.getUserIdAffected();
        this.userIdGenerated = group.getUserIdGenerated();
    }


    @Schema(description = "JSON object with the group Id. " +
            "Specify this field to update the group. " +
            "Referencing non-existing group Id will cause error. " +
            "Omit this field to create new role.")
    @Override
    public GroupId getId() {
        return super.getId();
    }



    @Schema(description = "Timestamp of the group creation, in milliseconds",
            example = "1609459200000",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public GroupId getExternalId() {
        return null;
    }

    @Override
    public void setExternalId(GroupId externalId) {

    }

    @Override
    public Long getVersion() {
        return 0L;
    }
}
