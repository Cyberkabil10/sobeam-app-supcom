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
package org.thingsboard.server.service.entitiy.role;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Set;

public interface SbRoleService {

    Role saveRole(TenantId tenantId, Role role)  throws Exception;

    void deleteRole(Role role , User user ) ;

    PageData<Role> getRolesByTenantId(TenantId tenantId , PageLink pageLink) throws ThingsboardException;

    RoleUser assignRoleToUsers(RoleId roleId , List<UserId> userIds) throws ThingsboardException;

    RoleUser assignRolesToUser(List<RoleId> roleIds , UserId userId) throws ThingsboardException;
}
