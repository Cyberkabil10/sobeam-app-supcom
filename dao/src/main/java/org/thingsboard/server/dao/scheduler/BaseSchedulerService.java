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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

@Service("SchedulerDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseSchedulerService implements SchedulerService {

    private final SchedulerDao schedulerDao;


    @Override
    public PageData<SchedulerEvent> findSchedulerByTenantId(TenantId tenantId, PageLink pageLink) {
        return schedulerDao.findByTenantId(tenantId.getId() , pageLink );
    }

    @Override
    public PageData<SchedulerEvent> findSchedulerByUserId(TenantId tenantId, UserId userId,PageLink pageLink) {
        return schedulerDao.findByUserId(tenantId.getId(), userId.getId(), pageLink );
    }


    @Override
    public SchedulerEvent saveScheduler(TenantId tenantId, SchedulerEvent schedulerEvent) {
        return schedulerDao.save(tenantId , schedulerEvent);
    }

    @Override
    public SchedulerEvent findSchedulerById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerById [{}]", schedulerEventId);
        // validateId(roleId, id -> INCORRECT_ASSET_ID + id);
        return schedulerDao.findById(tenantId, schedulerEventId.getId());
   }

    @Override
    public void deleteScheduler(TenantId tenantId, SchedulerEventId schedulerEventId) {
     // validateId(roleId , uuidbased);
        deleteEntity(tenantId, schedulerEventId , false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
       /* if (!force && schedulerDao.existsBySchedulerId(id.getId())){
            throw new DataValidationException("Scheduler can't be deleted because it used by user !");
        }*/ // if use dans aure table

        SchedulerEvent schedulerEvent = schedulerDao.findById(tenantId, id.getId());
        if (schedulerEvent == null) {
            return;
        }
        deleteScheduler(tenantId, schedulerEvent);
    }

    private void deleteScheduler(TenantId tenantId, SchedulerEvent schedulerEvent) {
        log.trace("Executing deleteScheduler [{}]", schedulerEvent.getId());
        schedulerDao.removeById(tenantId, schedulerEvent.getUuidId());

        // publishEvictEvent(new AssetCacheEvictEvent(asset.getTenantId(), asset.getName(), null));
        // countService.publishCountEntityEvictEvent(tenantId, EntityType.ASSET);
        //  eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(asset.getId()).build());
    }

    @Override
    public List<SchedulerEvent> findAllSchedulerEvent() {
        log.trace("Executing findAllSchedulerEvent");
        return schedulerDao.findAllSchedulerEvent();
    }

    @Override
    public void deleteAllByTenantId(TenantId tenantId) {
     schedulerDao.removeByTenantId(tenantId.getId());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.empty();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULEREVENT;
    }
}
