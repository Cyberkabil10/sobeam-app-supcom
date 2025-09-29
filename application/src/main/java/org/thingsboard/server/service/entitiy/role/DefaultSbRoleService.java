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
package org.thingsboard.server.service.entitiy.role;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultSbRoleService  extends AbstractTbEntityService implements SbRoleService {
    private final RoleService roleService;


    @Override
    public Role saveRole( TenantId tenantId , Role role) throws ThingsboardException {
        ActionType actionType = role.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        try {
             // if role.getId() deffirente de null et role.getAssignedUserIds() est not empty
            // remouver les role
            if(actionType == ActionType.UPDATED){
                roleService.remouveRoleToUser(role.getId());
            }
            Role savedRole = checkNotNull(roleService.saveRole(tenantId ,role));
            // if  role.getAssignedUserIds() not empty save les  assigenment les role to user
           if(role.getAssignedUserIds() != null && !role.getAssignedUserIds().isEmpty())
           {
               List<UserId> userIds = userIdFromStr(role.getAssignedUserIds());

               this.assignRoleToUsers(savedRole.getId(), userIds);

           }

            //  autoCommit(user, savedRole.getId());
           // logEntityActionService.logEntityAction(tenantId, savedRole.getId(), savedRole, role.getCustomerId(), actionType, user);
            return savedRole;
        } catch (Exception e) {
           // logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.ROLE), role, actionType, user, e);
            throw e;
        }
    }



    @Override
    public PageData<Role> getRolesByTenantId(TenantId tenantId , PageLink pageLink) throws ThingsboardException {
      //  log.trace("Executing findRolesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
       // Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
       // Validator.validatePageLink(pageLink);
        PageData<Role> listRole = roleService.findRolesByTenantId(tenantId, pageLink);

        // Filtrage des rôles pour exclure ceux de type "private"
        List<Role> filteredRoles = listRole.getData().stream()
                .filter(role -> !"PRIVATE".equalsIgnoreCase(role.getType())) // Supposons que getType() retourne une String
                .collect(Collectors.toList());

        // Mise à jour du nombre total d'éléments après filtrage
        int totalElementsFiltered = filteredRoles.size();
        boolean hasNext = totalElementsFiltered > pageLink.getPageSize(); // Vérifie si une autre page existe

        return new PageData<>(filteredRoles, listRole.getTotalPages(), totalElementsFiltered, hasNext);

    }
    @Override
    public RoleUser assignRoleToUsers(RoleId roleId, List<UserId> userIds) throws ThingsboardException {
       // TenantId tenantId = role.getTenantId();
      //  RoleId roleId = role.getId();
       /* try {
            Set<UserId> addedUserIds = new HashSet<>();
            for (UserId userId : userIds) {
                if (!isAssignedToUser(userId,roleId)) {
                    addedUserIds.add(userId);
                }
            }
            if (addedUserIds.isEmpty()) {
                return null ;
            } else {*/
                RoleUser savedRoleUser = null;
                for (UserId userId : userIds) {
                    savedRoleUser = checkNotNull(roleService.assignRoleToUser(roleId ,userId));
                    //  ShortUserInfo Info = savedRole.getAssignedCustomerInfo(userId);
                  //  logEntityActionService.logEntityAction();
                }
                return savedRoleUser;/*
            }
        } catch (Exception e) {
         //   logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, "Dashboard ID");
            throw e;
        }*/
    }

    @Override
    public RoleUser assignRolesToUser(List<RoleId> roleIds, UserId userId) throws ThingsboardException {
        // TenantId tenantId = role.getTenantId();
        //  RoleId roleId = role.getId();
       /* try {
            Set<UserId> addedUserIds = new HashSet<>();
            for (UserId userId : userIds) {
                if (!isAssignedToUser(userId,roleId)) {
                    addedUserIds.add(userId);
                }
            }
            if (addedUserIds.isEmpty()) {
                return null ;
            } else {*/
        roleService.remouveRoleWithUser(userId);
        RoleUser savedRoleUser = null;
        for (RoleId roleId : roleIds) {

            savedRoleUser = checkNotNull(roleService.assignRoleToUser(roleId ,userId));
            //  ShortUserInfo Info = savedRole.getAssignedCustomerInfo(userId);
            //  logEntityActionService.logEntityAction();
        }
        return savedRoleUser;/*
            }
        } catch (Exception e) {
         //   logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DASHBOARD), actionType, user, e, "Dashboard ID");
            throw e;
        }*/    }

    @Override
    public void deleteRole(Role role , User user)   {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = role.getTenantId();
        RoleId roleId = role.getId();
        roleService.deleteRole(tenantId , roleId);

    }

    private List<UserId> userIdFromStr(List<String> strUserIds) {
        List<UserId> userIds = new ArrayList<>();
        if (strUserIds != null) {
            for (String strUserId : strUserIds) {
                userIds.add(new UserId(UUID.fromString(strUserId)));
            }
        }
        return userIds;
    }
}
