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
import org.thingsboard.server.common.data.Role;

import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleDao extends Dao<Role> {
     /**
      * Find Roles by tenantId and page link.
      *
      * @param tenantId the tenantId
      * @param pageLink the page link
      * @return the list of role objects
      */

     PageData<Role> findRolesByTenantId(UUID tenantId , PageLink pageLink);

     boolean existsByRoleId(UUID roleId);

     RoleUser assignRole(RoleUser roleUser);

     void removeRoleWithUser (RoleId roleId) ;

     void removeUserWithRole (UserId userId) ;

     List<Role> findRolesByUserId(UserId userId);

     List<String> findUsersByRoleId(RoleId roleId);

     Role findRoleByName(String name);

     boolean hasUserRole(UserId userId);

     ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds);

     void removeByTeantId(UUID tenantId);
}
