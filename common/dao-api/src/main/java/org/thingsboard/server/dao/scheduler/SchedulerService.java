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
package org.thingsboard.server.dao.scheduler;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Optional;

public interface SchedulerService extends EntityDaoService{

  PageData<SchedulerEvent> findSchedulerByTenantId (TenantId tenantId , PageLink pageLink);
    PageData<SchedulerEvent> findSchedulerByUserId (TenantId tenantId , UserId userId, PageLink pageLink);

    SchedulerEvent saveScheduler (TenantId tenantId , SchedulerEvent schedulerEvent);
  SchedulerEvent findSchedulerById(TenantId tenantId, SchedulerEventId schedulerEventId);
  void deleteScheduler (TenantId tenantId,  SchedulerEventId schedulerEventId);
  List<SchedulerEvent> findAllSchedulerEvent();
  void deleteAllByTenantId( TenantId tenantId);
}
