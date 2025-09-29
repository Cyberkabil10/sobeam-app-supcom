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
    package org.thingsboard.server.dao.sql.role;

    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.transaction.annotation.Transactional;
    import org.thingsboard.server.dao.model.sql.AssetEntity;
    import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
    import org.thingsboard.server.dao.model.sql.RoleEntity;

    import java.util.List;
    import java.util.UUID;

    public interface RoleRepository  extends JpaRepository<RoleEntity, UUID> {

        @Query("SELECT di FROM RoleEntity di WHERE di.tenantId = :tenantId " +
                "AND (:searchText IS NULL OR ilike(di.name, CONCAT('%', :searchText, '%')) = true)")
        Page<RoleEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);
        @Query("SELECT r FROM RoleEntity r " +
                "INNER JOIN RoleUserEntity ru ON r.id = ru.roleId " +
                "WHERE ru.userId = :userId")
        List<RoleEntity> findRolesIdsByUserId(@Param("userId") UUID userId);

        RoleEntity findRoleByName(@Param("name") String name);

        List<RoleEntity> findByTenantIdAndIdIn(UUID tenantId, List<UUID> roleIds);

        @Modifying
        @Transactional
        @Query("DELETE FROM RoleEntity r WHERE r.tenantId = :tenantId")
        void deleteByTenantId(@Param("tenantId") UUID tenantId);


    }