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
package org.thingsboard.server.dao.role;


import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface RoleService extends EntityDaoService {

    Role saveRole( TenantId tenantId, Role role);

    void deleteRole (TenantId tenantId,  RoleId roleId);

    Role findRoleById(TenantId tenantId, RoleId roleId);

    Map<String, Map<String, Set<String>>> findRolesByUserId(User user);

    List<String> findUsersByRoleId(RoleId roleId);

    PageData<Role> findRolesByTenantId(TenantId tenantId , PageLink pageLink);

    RoleUser assignRoleToUser(RoleId roleId , UserId userId);

    void  remouveRoleToUser (RoleId roleId);

    void  remouveRoleWithUser (UserId userId);

    Role findRoleByName(String name);

    boolean hasUserRole(User user);

    List<Role> findRoleByUserId(UserId userId);

    ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync (TenantId tenantId , List<RoleId> roleIds);

    void deleteAllByTenantId(TenantId tenantId);
}
