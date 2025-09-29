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
package org.thingsboard.server.dao.sql.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.dao.model.sql.GroupEntity;
import org.thingsboard.server.dao.model.sql.RoleEntity;

import java.util.List;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {
    @Query("SELECT di FROM GroupEntity di WHERE di.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(di.name, CONCAT('%', :searchText, '%')) = true)")
    Page<GroupEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    @Query("SELECT di FROM GroupEntity di " +
            "WHERE di.tenantId = :tenantId " +
            "AND di.userIdAffected = :userId " +
            "AND (:searchText IS NULL OR ilike(di.name, CONCAT('%', :searchText, '%')) = true)")
    Page<GroupEntity> findByTenantSimpleId(@Param("tenantId") UUID tenantId,
                                           @Param("userId") UUID userId,
                                           @Param("searchText") String searchText,
                                           Pageable pageable);



    @Query("SELECT g FROM GroupEntity g WHERE g.userIdAffected = :userId AND g.type = :type")
    List<GroupEntity> findByUserIdAffectedAndType(@Param("userId") UUID userId, @Param("type") String type);

    List<GroupEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> groupsIds);

    @Query("SELECT g FROM GroupEntity g WHERE g.userIdAffected = :userId")
    List<GroupEntity> findGroupsByUserId(@Param("userId") UUID userId);
}
