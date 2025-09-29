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
package org.thingsboard.server.service.entitiy.group;

import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.common.data.GroupRelation;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.GroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface SbGroupService {

    Group saveGroup(TenantId tenantId, Group group)  throws Exception;

    void deleteGroup( TenantId tenantId ,Group group ) ;

    PageData<Group> getGroupByTenantId(TenantId tenantId , PageLink pageLink) throws ThingsboardException;

    PageData<Group> getGroupBySimpleTenantId(TenantId tenantId , UserId userId,  PageLink pageLink) throws ThingsboardException;

    boolean assignEntityToGroup(TenantId tenantId , GroupRelation groupRealtion);

    int unassignEntityToGroup(TenantId tenantId , GroupRelation groupRealtion);

    Group   assignGroupToUser(TenantId tenantId , GroupId groupId , UserId userId);

    List<Group> findGroupsByEntityId(TenantId tenantId , EntityId entityId);



}
