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
package org.thingsboard.server.dao.sql.group;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.common.data.GroupRelation;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.group.GroupDao;
import org.thingsboard.server.dao.model.sql.DomainOauth2ClientEntity;
import org.thingsboard.server.dao.model.sql.GroupEntity;
import org.thingsboard.server.dao.model.sql.GroupRelationEntity;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaGroupDao  extends JpaAbstractDao<GroupEntity, Group> implements GroupDao {

    private final GroupRepository groupRepository;
    private final GroupRelationRepository groupRelationRepository;
    @Override
    public PageData<Group> findGroupsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(groupRepository.findByTenantId(tenantId ,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Group> findGroupsByTenantSimpleId(UUID tenantId, UUID userId,PageLink pageLink) {
        return DaoUtil.toPageData(groupRepository.findByTenantSimpleId(tenantId , userId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }


    @Override
    public boolean addGroupRelation(GroupRelation groupRealtion) {
        groupRelationRepository.save(new GroupRelationEntity(groupRealtion));
        return true;
    }

    @Override
    public int remouveGroupRelation(GroupRelation groupRealtion) {
        return groupRelationRepository.removeByEntityIdAndGroupId(groupRealtion.getEntityId().getId(), groupRealtion.getGroupId().getId());
    }

    @Override
    public boolean checkRelationExists(GroupRelation groupRelation) {
        return groupRelationRepository.existsByEntityIdAndGroupId(groupRelation.getEntityId().getId() , groupRelation.getGroupId().getId());
    }

    @Override
    public boolean checkEntityExists(UserId userId, String type, EntityId entityId) {
        return groupRelationRepository.existsByUserIdAndTypeAndEntityId( userId.getId() ,  type ,  entityId.getId());
    }

    @Override
    public List<Group> listGroupUser(UserId userId, String type) {
        return  DaoUtil.convertDataList( groupRepository.findByUserIdAffectedAndType(userId.getId(),type));
    }

    @Override

    public List<Group> findGroupsByEntityId(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE:
            case DASHBOARD:
            case ASSET:
            case ENTITY_VIEW:
            case  CUSTOMER:
                return DaoUtil.convertDataList(groupRelationRepository.findGroupsByEntityId(entityId.getId()));
            case USER:
                return DaoUtil.convertDataList(groupRepository.findGroupsByUserId(entityId.getId()));
            default:
                throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
        }
    }

    @Override
    public ListenableFuture<List<Group>> findGroupsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> groupIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(groupRepository.findByTenantIdAndIdIn(tenantId, groupIds)));
    }

    @Override
    public boolean existsByGroupId(UUID groupId) {
        return groupRelationRepository.existsByGroupId(groupId);
    }


    @Override
    protected Class<GroupEntity> getEntityClass() {
        return GroupEntity.class;
    }

    @Override
    protected JpaRepository<GroupEntity, UUID> getRepository() {
        return groupRepository;
    }
}
