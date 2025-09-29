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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.asset.BaseAssetService.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("GroupDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseGroupService implements GroupService {
    private final GroupDao groupDao;

    @Override
    public PageData<Group> getGroupByTenantId(TenantId tenantId, PageLink pageLink) throws ThingsboardException {
        return groupDao.findGroupsByTenantId(tenantId.getId() , pageLink );
    }

    @Override
    public PageData<Group> getGroupByTenantSimpleId(TenantId tenantId, UserId userId, PageLink pageLink) throws ThingsboardException {
        return groupDao.findGroupsByTenantSimpleId(tenantId.getId() ,  userId.getId() ,pageLink );
    }

    @Override
    public Group saveGroup(TenantId tenantId, Group group) {
        return groupDao.save(tenantId, group);    }

    @Override
    public void deleteGroup(TenantId tenantId, GroupId groupId) {
        deleteEntity(tenantId, groupId, false);

    }

    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if ( !force && groupDao.existsByGroupId(id.getId())){
            throw new DataValidationException("Group can't be deleted because it used by user !");
        }

        Group group = groupDao.findById(tenantId, id.getId());
        if (group == null) {
            return;
        }
        deleteGroup(tenantId, group);
    }
    private void deleteGroup(TenantId tenantId, Group group) {
        log.trace("Executing deleteGroup [{}]", group.getId());
        groupDao.removeById(tenantId, group.getUuidId());
    }

    @Override
    public Group findGroupById(TenantId tenantId, GroupId groupId) {
        log.trace("Executing findRoleById [{}]", groupId);
        // validateId(roleId, id -> INCORRECT_ASSET_ID + id);
        return groupDao.findById(tenantId, groupId.getId());    }

    @Override
    public boolean assignEntityToGroup(TenantId tenantId, GroupRelation groupRealtion) {

        boolean exists = groupDao.checkRelationExists(groupRealtion);
        if(exists){
            // delete puis add
            return  false ; // le relation est deja existe
        }
           return  groupDao.addGroupRelation(groupRealtion);
    }

    @Override
    public boolean checkEntityExist(UserId userId, String resource, EntityId entityId) {
        return groupDao.checkEntityExists( userId, resource , entityId);
    }

    @Override
    public int unassignEntityToGroup(TenantId tenantId, GroupRelation groupRealtion) {
        boolean exists = groupDao.checkRelationExists(groupRealtion);
        if(exists){
            // delete puis add
            groupDao.remouveGroupRelation(groupRealtion);
        }
        return  0;   }

    @Override
    public Group assignGroupToUser(TenantId tenantId, UserId userId, GroupId groupId) {
        Group group = findGroupById(tenantId , groupId);
        if(userId.equals(group.getUserIdAffected())){
            return  group;
        }
        group.setUserIdAffected(userId);
        return  saveGroup( tenantId , group);
    }

    @Override
    public List<Group> listGroupUser(TenantId tenantId, UserId userId, String type) {
        return groupDao.listGroupUser(userId, type);
    }

    @Override
    public List<Group> findGroupsByEntityId(TenantId tenantId, EntityId entityId) {
        return  groupDao.findGroupsByEntityId(entityId);
    }

    @Override
    public ListenableFuture<List<Group>> findGroupByTenantIdAndIdsAsync(TenantId tenantId, List<GroupId> groupsIds) {
        log.trace("Executing findGroupsByTenantIdAndIdsAsync, tenantId [{}], groupsIds [{}]", tenantId, groupsIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(groupsIds, ids -> "Incorrect roleIds " + ids);
        return groupDao.findGroupsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(groupsIds));    }



    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.empty();
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }
}
