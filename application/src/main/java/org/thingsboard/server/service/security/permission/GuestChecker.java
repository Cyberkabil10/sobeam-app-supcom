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
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.group.GroupService;
import org.thingsboard.server.dao.role.RoleService;
import java.util.*;

@Service
@Slf4j
public class GuestChecker {


    private final RoleService roleService;
    private  final GroupService groupService;
    private Map<String, Map<String ,Set<String>>> permissionsMap = new HashMap<>();

    public GuestChecker(RoleService roleService , GroupService groupService) {
        this.roleService = roleService;
        this.groupService = groupService;
    }

    public boolean doCheck(User user, Resource resource, Operation operation , EntityId entityId) throws ThingsboardException {

        permissionsMap = roleService.findRolesByUserId(user);

        if (Authority.SYS_ADMIN.equals(user.getAuthority())
                || Boolean.TRUE.equals(user.getStatus())
                || (Authority.CUSTOMER_USER.equals(user.getAuthority()) && permissionsMap.isEmpty())) {
            return true;
        }

        if(user.getStatus().equals(true)) {
           return  true;
        }
        if (!hasResourceAccess(resource , user)) {
            permissionDenied();
        }

        if (!hasOperationAccess(resource, operation , user)) {
            permissionDenied();
        }

        if (entityId != null && !permissionsMap.isEmpty()) {
            // Define resources that require entity access
            Set<String> restrictedResources = Set.of(
                    Resource.DASHBOARD.name(),
                    Resource.DEVICE.name(),
                    Resource.ASSET.name(),
                    Resource.CUSTOMER.name()
            );

            if (restrictedResources.contains(resource.name())) {
                if (Authority.CUSTOMER_USER.equals(user.getAuthority())) {
                    return true;
                }
                if (!hasEntityAccess(user.getId(), resource, entityId)) {
                    permissionDenied();
                }
            }
        }

        return true;
    }

    private boolean hasResourceAccess(Resource resource , User user) {
        if(permissionsMap.get("menuPermission").isEmpty() && Authority.CUSTOMER_USER.equals(user.getAuthority())) return  true  ;
        else return permissionsMap.get("menuPermission").containsKey(resource.name());
    }

    private boolean hasOperationAccess(Resource resource, Operation operation ,User user) {
        if(permissionsMap.get("menuPermission").isEmpty() && Authority.CUSTOMER_USER.equals(user.getAuthority())) return  true  ;
        Map<String, Set<String>> menuPermissions = permissionsMap.get("menuPermission");
        Set<String> allowedOperations = menuPermissions.get(resource.name());
        return allowedOperations != null && (allowedOperations.contains(Operation.ALL.name()) || allowedOperations.contains(operation.name()));
    }

    private boolean hasEntityAccess(UserId userId , Resource resource, EntityId entityId) {
        // Correct access check
        return groupService.checkEntityExist( userId , resource.name(), entityId);
    }

    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
    }
}