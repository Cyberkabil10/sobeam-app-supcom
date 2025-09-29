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
package org.thingsboard.server.controller;
import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.RoleUser;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.role.SbRoleService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.*;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class RoleController extends  BaseController{

    private final SbRoleService sbRoleService;

    @ApiOperation(value = "Get Roles by Tenant (getRoleByTenant)",
            notes = "Returns a list of roles associated with a specific tenant. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/roles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Role> getTenantRoles(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "The requested")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
             TenantId tenantId = getCurrentUser().getTenantId();
             PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        checkTenantId(tenantId, Operation.READ);
        return checkNotNull(sbRoleService.getRolesByTenantId(tenantId , pageLink));
    }

    @ApiOperation(value = "Get Roles by current user (getUserRoles)",
            notes = "Returns a map of roles associated with the current user making the request. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN' ,'CUSTOMER_USER')")
    @RequestMapping(value = "/user/role/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Map<String, Set<String>>> getCurrentUserRoles(
            @Parameter(description = "User ID")
            @PathVariable("userId") String strUserId) throws ThingsboardException {
        return roleService.findRolesByUserId(getCurrentUser());
    }


    @ApiOperation(value = "Save Or update Role (saveRole)",
            notes = "Create or update the Role. When creating role, platform generates Role Id as " + UUID_WIKI_LINK +
                    "The newly created Role Id will be present in the response. " +
                    "Specify existing Role Id to update the role. " +
                    "Referencing non-existing Role Id will cause 'Not Found' error." +
                    "\n\nRole name is unique for entire platform setup." +
                    "Remove 'id' and 'tenantId' from the request body example (below) to create new Role entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/role", method = RequestMethod.POST)
    @ResponseBody
    public Role saveRole(
            @Parameter(description = "A JSON value representing the Role.")
            @RequestBody Role role) throws Exception {
            role.setTenantId(getCurrentUser().getTenantId());
         checkEntity(role.getId(), role, Resource.ROLE);
        return sbRoleService.saveRole(getCurrentUser().getTenantId(), role);
    }

    @ApiOperation(value = "Delete Role (deleteRole)",
            notes = "Removes the Role from the system. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteRole(
            @Parameter(description = "Role ID")
            @PathVariable("roleId") String strRoleId) throws ThingsboardException {
            checkParameter(ROLE_ID, strRoleId);
            RoleId roleId = new RoleId(toUUID(strRoleId));
            Role role = checkRoleId(roleId, Operation.DELETE);
            sbRoleService.deleteRole(role , getCurrentUser());
    }



    @ApiOperation(value = "Get Role  (getRoleById)",
            notes = "Fetch the Role object based on the provided Role Id. " +
                    "The server checks that the role  is owned by the same tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN' , 'CUSTOMER_USER')")
    @RequestMapping(value = "/role/{roleId}", method = RequestMethod.GET)
    @ResponseBody
    public Role getRoleById(
            @PathVariable("roleId") String strRoleId ) throws ThingsboardException {
            checkParameter(ROLE_ID, strRoleId);
            RoleId roleId = new RoleId(toUUID(strRoleId));
            var result = checkRoleId(roleId, Operation.READ);
           Role userRole = roleService.findRoleById(getCurrentUser().getTenantId() , roleId);
           List<String> listUsers = roleService.findUsersByRoleId(roleId);
           userRole.setAssignedUserIds(listUsers);
        return userRole;
    }

    @ApiOperation(value = "Get Roles by user ",
            notes = "Fetch the Role object based on the provided user Id. " +
                    "The server checks that the role  is owned by the same tenant. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN' , 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public List<Role> getRolesByUser(
            @PathVariable("userId") String strUserId ) throws ThingsboardException {
        //checkParameter(ASSET_PROFILE_ID, strAssetProfileId);
        //   AssetProfileId assetProfileId = new AssetProfileId(toUUID(strAssetProfileId));
        //   var result = checkAssetProfileId(assetProfileId, Operation.READ);
        UserId userId = new UserId(toUUID(strUserId));
        // get the user associated with the role
        List<Role> userRole = roleService.findRoleByUserId(userId);
        return userRole;
    }


    @ApiOperation(value = "Assign Roles to User (assignRolesToUsers)",
            notes = "Assigns the specified roles to the given user(s). " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/role/{userId}/assign", method = RequestMethod.POST)
    @ResponseBody
    public RoleUser assignRolesToUsers(
            @Parameter(description = "..")
            @PathVariable("userId") String strUserId,
            @Parameter(description = "JSON array with the list of users ids")
            @RequestBody String[] strRoleIds) throws ThingsboardException {
          // checkParameter(DASHBOARD_ID, strDashboardId);
          UserId userId = new UserId(toUUID(strUserId));
          //  Role role = checkRoleId(roleId, Operation.ASSIGN_TO_CUSTOMER);
           List<RoleId> roleIds = roleIdFromStr(strRoleIds);
        return sbRoleService.assignRolesToUser(roleIds, userId);
    }

    @ApiOperation(value = "Get role By Ids (getRolesByIds)",
            notes = "Requested assets must be owned by tenant or assigned to customer which user is performing the request. ")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/roles", params = {"roleIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Role> getRolesByIds(
            @Parameter(description = "A list of roles ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam("roleIds") String[] strRoleIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("roleIds", strRoleIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
     //   CustomerId customerId = user.getCustomerId();
        List<RoleId> roleIds = new ArrayList<>();
        for (String strRoleId : strRoleIds) {
            roleIds.add(new RoleId(toUUID(strRoleId)));
        }
        ListenableFuture<List<Role>> roles;
        roles = roleService.findRolesByTenantIdAndIdsAsync(tenantId , roleIds);
        return checkNotNull(roles.get());
    }


    private List<RoleId> roleIdFromStr(String[] strRoleIds) {
        List<RoleId> roleIds = new ArrayList<>();
        if (strRoleIds != null) {
            for (String strRoleId : strRoleIds) {
                roleIds.add(new RoleId(UUID.fromString(strRoleId)));
            }
        }
        return roleIds;
    }
}
