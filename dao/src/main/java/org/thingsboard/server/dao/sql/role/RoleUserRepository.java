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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.model.sql.RoleUserCompositeKey;
import org.thingsboard.server.dao.model.sql.RoleUserEntity;

import java.util.List;
import java.util.UUID;

public interface RoleUserRepository  extends JpaRepository<RoleUserEntity, RoleUserCompositeKey> {

    boolean existsByRoleId (UUID roleId);

    @Query("SELECT ru.userId FROM RoleUserEntity ru WHERE ru.roleId = :roleId")
    List<UUID> findUserIdsByRoleId(UUID roleId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RoleUserEntity ru WHERE ru.roleId = :roleId")
    void deleteByRoleId(@Param("roleId") UUID roleId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RoleUserEntity rue WHERE rue.userId = :userId AND rue.roleId IN " +
            "(SELECT r.id FROM RoleEntity r WHERE r.type != 'PRIVATE')")
    void deleteByUserId(@Param("userId") UUID userId);


    boolean existsByUserId (UUID userId);

}