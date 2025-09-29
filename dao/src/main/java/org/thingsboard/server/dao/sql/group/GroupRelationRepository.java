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

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.Group;
import org.thingsboard.server.dao.model.sql.GroupEntity;
import org.thingsboard.server.dao.model.sql.GroupRelationEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRelationRepository extends JpaRepository<GroupRelationEntity, UUID> {
    boolean existsByEntityIdAndGroupId(UUID entityId, UUID groupId);

    @Query("SELECT g FROM GroupEntity g JOIN GroupRelationEntity gr ON g.id = gr.groupId WHERE gr.entityId = :entityId")
    List<GroupEntity> findGroupsByEntityId(UUID entityId);

    @Modifying
    @Transactional
    @Query("DELETE FROM GroupRelationEntity gr WHERE gr.entityId = :entityId AND gr.groupId = :groupId")
    int removeByEntityIdAndGroupId(UUID entityId, UUID groupId);

    @Query("""
    SELECT CASE WHEN COUNT(gr) > 0 THEN true ELSE false END
    FROM GroupRelationEntity gr
    JOIN GroupEntity g ON gr.groupId = g.id
    WHERE gr.entityId = :entityId
      AND g.type = :type
      AND g.userIdAffected = :userId
""")
    boolean existsByUserIdAndTypeAndEntityId(@Param("userId") UUID userId,
                                             @Param("type") String type,
                                             @Param("entityId") UUID entityId);

    boolean existsByGroupId(UUID groupId);

}
