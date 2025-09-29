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
package org.thingsboard.server.dao.sql.role;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.model.sql.RoleUserEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaRoleDao extends JpaAbstractDao<RoleEntity, Role> implements RoleDao {
    private final RoleRepository roleRepository;

    private final RoleUserRepository roleUserRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected JpaRepository<RoleEntity, UUID> getRepository() {
        return roleRepository;
    }




    @Override
    public PageData<Role> findRolesByTenantId(UUID tenantId , PageLink pageLink) {
        return  DaoUtil.toPageData(roleRepository.findByTenantId(tenantId ,
                                                    pageLink.getTextSearch(),
                                                    DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByRoleId(UUID roleId) {
        return roleUserRepository.existsByRoleId(roleId);
    }

    @Override
    public RoleUser assignRole(RoleUser roleUser) {
        RoleUserEntity roleUserEntity = new RoleUserEntity(roleUser);
        return DaoUtil.getData(roleUserRepository.save(roleUserEntity));
    }



    @Override
    public void removeRoleWithUser( RoleId roleId) {
     roleUserRepository.deleteByRoleId(roleId.getId());
    }

    @Override
    public void removeUserWithRole(UserId userId) {
        roleUserRepository.deleteByUserId(userId.getId());

    }

    @Override
    public List<Role> findRolesByUserId(UserId userId) {
        return DaoUtil.convertDataList(roleRepository.findRolesIdsByUserId(userId.getId()));
    }

    @Override
    public List<String> findUsersByRoleId(RoleId roleId) {
        return Optional.ofNullable(roleUserRepository.findUserIdsByRoleId(roleId.getId()))
                .orElse(List.of()) // Return an empty list if null
                .stream() // Convert List<UUID> to Stream<UUID>
                .map(UUID::toString) // Convert each UUID to String
                .collect(Collectors.toList()); // Collect as List<String>
    }

    @Override
    public Role findRoleByName(String name) {
        return DaoUtil.getData(roleRepository.findRoleByName(name));
    }

    @Override
    public boolean hasUserRole(UserId userId) {
        return roleUserRepository.existsByUserId(userId.getId());
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(roleRepository.findByTenantIdAndIdIn(tenantId, roleIds)));
    }

    @Override
    public void removeByTeantId(UUID tenantId) {
      roleRepository.deleteByTenantId(tenantId);

    }

}
