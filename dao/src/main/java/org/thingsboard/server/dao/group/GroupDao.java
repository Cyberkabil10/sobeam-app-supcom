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
package org.thingsboard.server.dao.group;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.common.data.GroupRelation;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface GroupDao extends Dao<Group> {
    PageData<Group> findGroupsByTenantId(UUID tenantId , PageLink pageLink);

    PageData<Group> findGroupsByTenantSimpleId(UUID tenantId , UUID userId ,PageLink pageLink);


    boolean addGroupRelation(GroupRelation groupRealtion);
    int remouveGroupRelation(GroupRelation groupRealtion);

    boolean checkRelationExists(GroupRelation groupRelation);

    boolean checkEntityExists (UserId userId , String type , EntityId entityId);

    List<Group> listGroupUser(UserId userId , String type);

    List<Group> findGroupsByEntityId (EntityId entityId);

    ListenableFuture<List<Group>> findGroupsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> groupIds);

    boolean existsByGroupId(UUID groupId);


}
