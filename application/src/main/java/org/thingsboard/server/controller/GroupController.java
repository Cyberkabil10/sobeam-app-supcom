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
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.group.SbGroupService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class GroupController extends  BaseController{

    private final SbGroupService sbGroupService;

    @ApiOperation(value = "Get Group by Tenant (getGroupByTenant)",
            notes = "Returns a list of Groupd associated with a specific tenant. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/groups", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Group> getTenantGroups(
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
        if (getCurrentUser().getStatus()){
            checkTenantId(tenantId, Operation.READ);
            return checkNotNull(sbGroupService.getGroupByTenantId(tenantId , pageLink));
        }else{
            return checkNotNull(sbGroupService.getGroupBySimpleTenantId(tenantId , getCurrentUser().getId(),  pageLink));

        }
    }


    @ApiOperation(value = "Save Or update Group (saveGroup)",
            notes = "Create or update the Group. When creating group, platform generates Group Id as " + UUID_WIKI_LINK +
                    "The newly created Group Id will be present in the response. " +
                    "Specify existing Group Id to update the Group. " +
                    "Referencing non-existing Group Id will cause 'Not Found' error." +
                    "\n\nGroup name is unique for entire platform setup." +
                    "Remove 'id' and 'tenantId' from the request body example (below) to create new Role entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/group", method = RequestMethod.POST)
    @ResponseBody
    public Group saveGroup(
            @Parameter(description = "A JSON value representing the Group.")
            @RequestBody Group group) throws Exception {
        group.setTenantId(getCurrentUser().getTenantId());
        group.setUserIdGenerated(getCurrentUser().getId());
        group.setUserIdAffected(new UserId(NULL_UUID));
        checkEntity(group.getId(), group, Resource.GROUP);
        return sbGroupService.saveGroup(getCurrentUser().getTenantId(), group);
    }

    @ApiOperation(value = "Assign Entity To Group")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/assign/entity", method = RequestMethod.POST)
    @ResponseBody
    public boolean assignEntityToGroup( @RequestBody GroupRelation groupRealtion) throws ThingsboardException {
       //  checkParameter("customerId", strCustomerId);
       // checkParameter(ASSET_ID, strAssetId);

       //  Customer customer = checkCustomerId(customerId, Operation.READ);
       // checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);

        return sbGroupService.assignEntityToGroup(getTenantId(), groupRealtion);
    }

    @ApiOperation(value = "Unassign Entity To Group")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/unassign/entity", method = RequestMethod.POST)
    @ResponseBody
    public int unassignEntityToGroup( @RequestBody GroupRelation groupRealtion) throws ThingsboardException {
        return sbGroupService.unassignEntityToGroup(getTenantId(), groupRealtion);
    }

    @ApiOperation(value = "Assign Group To User")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/group/{groupId}/user/{userId}", method = RequestMethod.POST)
    @ResponseBody
    public Group  assignGroupToUser(  @PathVariable("groupId") String strGroupId, @PathVariable("userId") String strUserId) throws ThingsboardException {
        //  checkParameter("customerId", strCustomerId);
        // checkParameter(ASSET_ID, strAssetId);
        GroupId groupId = new GroupId(toUUID(strGroupId));
        UserId newUsertId = new UserId(toUUID(strUserId));

       /* Tenant newTenant = userService.findUserById(getTenantId(), newUsertId);
        if (newTenant == null) {
            throw new ThingsboardException("Could not find the specified Tenant!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
*/
        return sbGroupService.assignGroupToUser(getTenantId(), groupId , newUsertId);
    }


    @ApiOperation(value = "Get groups by entity ID", notes = "Returns the list of groups associated with the given entity ID and type.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/groups/{type}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public List<Group> getGroupsByEntityId(
            @Parameter(description = "Type of the entity (e.g. DEVICE)") @PathVariable("type") String strType,
            @Parameter(description = "Entity ID") @PathVariable("entityId") String strEntityId
    ) throws ThingsboardException {
        EntityId entityId = switch (strType.toUpperCase()) {
            case "DEVICE" -> new DeviceId(toUUID(strEntityId));
            case "ASSET" -> new AssetId(toUUID(strEntityId));
            case "DASHBOARD" -> new DashboardId(toUUID(strEntityId));
            case "CUSTOMER" -> new CustomerId(toUUID(strEntityId));
            case "USER" -> new UserId(toUUID(strEntityId));
            default -> throw new IllegalArgumentException("Unsupported entity type: " + strType);
        };
        return sbGroupService.findGroupsByEntityId(getTenantId(), entityId);
    }

    @ApiOperation(value = "Get Group By Ids (getGroupsByIds)",
            notes = "Requested group must be owned by tenant or assigned to customer which user is performing the request. ")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/groups", params = {"groupIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Group> getGroupsByIds(
            @Parameter(description = "A list of groups ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")))
            @RequestParam("groupIds") String[] strGroupIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("groupIds", strGroupIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        //   CustomerId customerId = user.getCustomerId();
        List<GroupId> groupsIds = new ArrayList<>();
        for (String strGroup : strGroupIds) {
            groupsIds.add(new GroupId(toUUID(strGroup)));
        }
        ListenableFuture<List<Group>> groups;
        groups = groupService.findGroupByTenantIdAndIdsAsync(tenantId , groupsIds);
        return checkNotNull(groups.get());
    }


    @ApiOperation(value = "Delete Group (deleteGroup)",
            notes = "Removes the Group from the system. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteGroup(
            @Parameter(description = "Group ID")
            @PathVariable("groupId") String strGroupId) throws ThingsboardException {
        checkParameter("groupId", strGroupId);
        GroupId groupId = new GroupId(toUUID(strGroupId));
        Group group = checkGroupId(groupId, Operation.DELETE);
        sbGroupService.deleteGroup(getCurrentUser().getTenantId(), group);    }

}
