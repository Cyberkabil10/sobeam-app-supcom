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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.group.GroupService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.List;

import static com.google.api.client.util.Preconditions.checkNotNull;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultSbGroupService implements SbGroupService{
    private final GroupService groupService;

    @Override
    public Group saveGroup(TenantId tenantId, Group group) throws Exception {
        ActionType actionType = group.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {

            Group savedGroup = checkNotNull(groupService.saveGroup(tenantId ,group));

                        // logEntityActionService.logEntityAction(tenantId, savedRole.getId(), savedRole, role.getCustomerId(), actionType, user);
            return savedGroup;
        } catch (Exception e) {
            // logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ROLE), role, actionType, user, e);
            throw e;
        }    }

    @Override
    public void deleteGroup(TenantId tenantId,Group group) {
        ActionType actionType = ActionType.DELETED;
        groupService.deleteGroup(tenantId, group.getId());
    }

    @Override
    public PageData<Group> getGroupByTenantId(TenantId tenantId , PageLink pageLink) throws ThingsboardException {
        //  log.trace("Executing findRolesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        // Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        // Validator.validatePageLink(pageLink);
        PageData<Group> listRole = groupService.getGroupByTenantId(tenantId, pageLink);
        return listRole;

    }

    @Override
    public PageData<Group> getGroupBySimpleTenantId(TenantId tenantId, UserId userId, PageLink pageLink) throws ThingsboardException {
        PageData<Group> listRole = groupService.getGroupByTenantSimpleId(tenantId,  userId, pageLink);
        return listRole;    }

    @Override
    public boolean assignEntityToGroup(TenantId tenantId, GroupRelation groupRealtion) {
        return   groupService.assignEntityToGroup( tenantId, groupRealtion);

    }

    @Override
    public int unassignEntityToGroup(TenantId tenantId, GroupRelation groupRealtion) {
        return   groupService.unassignEntityToGroup( tenantId, groupRealtion);
    }

    @Override
    public Group assignGroupToUser(TenantId tenantId, GroupId groupId, UserId userId) {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        try {
            Group savedGroup = checkNotNull(groupService.assignGroupToUser(tenantId, userId , groupId));

            return savedGroup;
        } catch (Exception e) {
            throw e;
        }    }

    @Override
    public List<Group> findGroupsByEntityId(TenantId tenantId, EntityId entityId) {
        return groupService.findGroupsByEntityId(tenantId ,entityId);
    }
}
