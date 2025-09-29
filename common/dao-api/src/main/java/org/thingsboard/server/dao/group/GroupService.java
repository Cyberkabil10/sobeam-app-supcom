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
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;

public interface GroupService  extends EntityDaoService {
    Group saveGroup(TenantId tenantId, Group group);

    void deleteGroup (TenantId tenantId,  GroupId groupId);

    PageData<Group> getGroupByTenantId(TenantId tenantId , PageLink pageLink) throws ThingsboardException;

    PageData<Group> getGroupByTenantSimpleId(TenantId tenantId ,UserId userId ,PageLink pageLink) throws ThingsboardException;

    Group findGroupById(TenantId tenantId, GroupId groupId);

    boolean assignEntityToGroup (TenantId tenantId , GroupRelation groupRealtion);

    boolean checkEntityExist(UserId userId, String resource, EntityId entityId);

    int unassignEntityToGroup (TenantId tenantId , GroupRelation groupRealtion);

    Group assignGroupToUser (TenantId tenantId ,  UserId userId ,  GroupId groupId);

    List<Group> listGroupUser(TenantId tenantId , UserId userId , String type);

    List<Group> findGroupsByEntityId(TenantId tenantId , EntityId entityId);

    ListenableFuture<List<Group>> findGroupByTenantIdAndIdsAsync (TenantId tenantId , List<GroupId> groupsIds);


}
