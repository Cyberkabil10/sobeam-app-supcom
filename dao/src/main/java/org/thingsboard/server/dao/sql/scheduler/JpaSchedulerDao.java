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
package org.thingsboard.server.dao.sql.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.SchedulerEvent;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.scheduler.SchedulerDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaSchedulerDao  extends JpaAbstractDao<SchedulerEventEntity, SchedulerEvent> implements SchedulerDao {

    private final SchedulerRepository schedulerRepository;
    @Override
    public PageData<SchedulerEvent> findByTenantId(UUID tenantId, PageLink pageLink) {
        return  DaoUtil.toPageData(schedulerRepository.findByTenantId(tenantId ,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));    }
    @Override
    public PageData<SchedulerEvent> findByUserId(UUID tenantId, UUID userId ,PageLink pageLink) {
        return  DaoUtil.toPageData(schedulerRepository.findByUserId( userId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));    }

    @Override
    public boolean existsBySchedulerId(UUID schedulerId) {
        return schedulerRepository.existsById(schedulerId);
    }

    @Override
    public List<SchedulerEvent> findAllSchedulerEvent() {
        log.trace("Executing findAllSchedulerEvent");
        List<SchedulerEventEntity> entities = schedulerRepository.findAll();
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public void removeByTenantId(UUID tenantId) {
     schedulerRepository.deleteByTenantId(tenantId);
    }

    @Override
    protected Class<SchedulerEventEntity> getEntityClass() {
        return SchedulerEventEntity.class;
    }

    @Override
    protected JpaRepository<SchedulerEventEntity, UUID> getRepository() {
        return schedulerRepository;
    }
}
