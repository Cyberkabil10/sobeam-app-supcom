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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.scheduler.TbSchedulerService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import static org.thingsboard.server.controller.ControllerConstants.*;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class SchedulerController extends  BaseController{
    @Autowired
    private TbSchedulerService tbschedulerService;

    @ApiOperation(value = "Get Scheduler by Tenant (getTenantSchedulerEvent)",
            notes = "Returns a list of scheduler associated with a specific tenant. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN' , 'CUSTOMER_USER'  )")
    @RequestMapping(value = "/tenant/schedulers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<SchedulerEvent> getTenantSchedulerEvent(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = "The requested")
            @RequestParam(required = false) String textSearch,
            @Parameter(description = "User Id")
            @RequestParam(required = false) String userId,
            @Parameter(description = "MOBILE flag to indicate mobile app request")
            @RequestParam(required = false) Boolean mobile,
             @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "title"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if(mobile != null && mobile){
            UserId userId1 = new UserId(toUUID(userId));
            return checkNotNull(tbschedulerService.getScheduledEventsByUser(tenantId, userId1, pageLink));
        }else {
            if(getCurrentUser().getStatus()==false){
                return checkNotNull(tbschedulerService.getScheduledEventsByUser(tenantId, getCurrentUser().getId(), pageLink));
            }
            return checkNotNull(tbschedulerService.getScheduledEventsByTenant(tenantId , pageLink));
        }
    }
    @ApiOperation(value = "Save Or update Scheduler (saveScheduler)",
            notes = "Create or update the SchedulerEvent . When creating Scheduler, platform generates Scheduler Id as " + UUID_WIKI_LINK +
                    "The newly created Scheduler Id will be present in the response. " +
                    "Specify existing Scheduler Id to update the Scheduler. " +
                    "Referencing non-existing Scheduler Id will cause 'Not Found' error." +
                    "\n\nScheduler name is unique for entire platform setup." +
                    "Remove 'id' and 'tenantId' from the request body example (below) to create new Scheduler entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN'  )")
    @RequestMapping(value = "/scheduler", method = RequestMethod.POST)
    @ResponseBody
    public SchedulerEvent saveScheduler(
            @Parameter(description = "A JSON value representing the Scheduler.")
            @RequestBody SchedulerEvent schedulerEvent) throws Exception {
        schedulerEvent.setTenantId(getCurrentUser().getTenantId());
        if (schedulerEvent.getUuidId()== null){
            schedulerEvent.setUserId(schedulerEvent.getUserId() != null  ? schedulerEvent.getUserId() : getCurrentUser().getId() );}
        checkEntity(schedulerEvent.getId(), schedulerEvent, Resource.SCHEDULEREVENT);
        return tbschedulerService.saveScheduler(getCurrentUser().getTenantId(), schedulerEvent);
    }
        @ApiOperation(value = "Delete Scheduler (deleteScheduler)",
            notes = "Removes the Scheduler from the system. " +
                    "The scope depends on the authority of the user that performs the request.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/scheduler/{schedulerId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteScheduler(
            @Parameter(description = "Scheduler ID")
            @PathVariable("schedulerId") String strSchedulerId) throws ThingsboardException {
        checkParameter("schedulerId", strSchedulerId);
        SchedulerEventId schedulerId = new SchedulerEventId(toUUID(strSchedulerId));
        SchedulerEvent schedulerEvent = checkSchedulerId(schedulerId, Operation.DELETE);
        tbschedulerService.deleteScheduler(schedulerEvent , getCurrentUser());
    }

}
