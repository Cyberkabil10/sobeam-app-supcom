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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.asset.BaseAssetService.INCORRECT_TENANT_ID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("RoleDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseRoleService implements RoleService {

  private final RoleDao roleDao;

  @Override
    public Role saveRole(TenantId tenantId, Role role) {
        return roleDao.save(tenantId, role);
    }

    @Override
    public void deleteRole(TenantId tenantId, RoleId roleId) {
        // validateId(roleId , uuidbased);
        deleteEntity(tenantId, roleId , false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force && roleDao.existsByRoleId(id.getId())){
        throw new DataValidationException("Role can't be deleted because it used by user !");
        }

        Role role = roleDao.findById(tenantId, id.getId());
        if (role == null) {
            return;
        }
        deleteRole(tenantId, role);
    }

    private void deleteRole(TenantId tenantId, Role role) {
        log.trace("Executing deleteRole [{}]", role.getId());
        roleDao.removeById(tenantId, role.getUuidId());

       // publishEvictEvent(new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), null));
       // countService.publishCountEntityEvictEvent(tenantId, EntityType.ASSET);
      //  eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(asset.getId()).build());
    }
    @Override
    public Role findRoleById(TenantId tenantId, RoleId roleId) {
        log.trace("Executing findAssetById [{}]", roleId);
       // validateId(roleId, id -> INCORRECT_ASSET_ID + id);
        return roleDao.findById(tenantId, roleId.getId());
    }

    @Override
    public Map<String, Map<String, Set<String>>> findRolesByUserId(User user) {
        List<Role> roles = roleDao.findRolesByUserId(user.getId());
        if (roles.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Set<String>>> permissions = Map.of(
                "menuPermission", new HashMap<>()
        );

        for (Role role : roles) {
            JsonNode permissionsNode = role.getPermissions();

            permissions.forEach((key, resourceMap) -> processPermission(permissionsNode.get(key), resourceMap));
        }
        return permissions;
    }

    @Override
    public List<String> findUsersByRoleId(RoleId roleId) {
        return  roleDao.findUsersByRoleId(roleId);
    }

    private void processPermission(JsonNode permissionNode, Map<String, Set<String>> resourceMap) {
        if (permissionNode == null || permissionNode.isNull()) {
            return;
        }

        JsonNode resourceIds = permissionNode.get("resource");
        JsonNode operations = permissionNode.get("operations");

        if (resourceIds != null && resourceIds.isArray()) {
            for (JsonNode resourceId : resourceIds) {
                Set<String> ops = resourceMap.computeIfAbsent(resourceId.asText(), k -> new HashSet<>());
                if (operations != null && operations.isArray()) {
                    operations.forEach(op -> ops.add(op.asText()));
                }
            }
        } else if (permissionNode.isArray()) { // Special case for menuPermission
            for (JsonNode menuItem : permissionNode) {
                String resource = menuItem.get("resource").asText();
                Set<String> ops = resourceMap.computeIfAbsent(resource, k -> new HashSet<>());
                JsonNode opsNode = menuItem.get("operations");
                if (opsNode != null && opsNode.isArray()) {
                    opsNode.forEach(op -> ops.add(op.asText()));
                }
            }
        }
    }


    @Override
    public PageData<Role> findRolesByTenantId(TenantId tenantId , PageLink pageLink){
        return roleDao.findRolesByTenantId(tenantId.getId() , pageLink );
    }

    @Override
    public RoleUser assignRoleToUser(RoleId roleId, UserId userId) {
        RoleUser roleUser = new RoleUser(userId ,roleId);
        return roleDao.assignRole(roleUser);
    }

    @Override
    public void remouveRoleToUser(RoleId roleId) {
         roleDao.removeRoleWithUser(roleId);
    }

    @Override
    public void remouveRoleWithUser(UserId userId) {
        roleDao.removeUserWithRole(userId);
    }


    @Override
    public Role findRoleByName(String name) {
        return roleDao.findRoleByName(name);
    }

    @Override
    public boolean hasUserRole(User user) {
        return roleDao.hasUserRole(user.getId());
    }

    @Override
    public List<Role> findRoleByUserId(UserId userId) {
        List<Role> roles = roleDao.findRolesByUserId(userId);
        return roles.stream()
                .filter(role -> !role.getType().equals("PRIVATE"))
                .collect(Collectors.toList());
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(TenantId tenantId, List<RoleId> roleIds) {
        log.trace("Executing findRolesByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, roleIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(roleIds, ids -> "Incorrect roleIds " + ids);
        return roleDao.findRolesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(roleIds));    }

    @Override
    public void deleteAllByTenantId(TenantId tenantId) {
        roleDao.removeByTeantId(tenantId.getId());
    }


    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.empty();
    }


    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }
}
