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

import jakarta.persistence.*;
import lombok.Data;
import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.UserId;

import org.thingsboard.server.dao.model.ToData;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = ROLE_USER_TABLE_NAME)
@IdClass(RoleUserCompositeKey.class)
public class RoleUserEntity implements ToData<RoleUser> {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "role_id", columnDefinition = "uuid")
    private UUID roleId;



    public RoleUserEntity() {
        super();
    }

    public RoleUserEntity(RoleUser roleUser) {
        userId = roleUser.getUserId().getId();
        roleId = roleUser.getRoleId().getId();
    }



    @Override
    public RoleUser toData() {
        RoleUser result = new RoleUser();
        result.setUserId(new UserId(userId));
        result.setRoleId(new RoleId(roleId));
        return result;
    }
}
