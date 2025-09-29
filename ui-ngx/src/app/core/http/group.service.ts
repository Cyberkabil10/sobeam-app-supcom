///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Role } from '@shared/models/role.models';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EntityId, UserId } from '@app/shared/public-api';
import { Group, GroupRelation } from '@app/shared/models/group.models';

@Injectable({
  providedIn: 'root'
})
export class GroupService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantGroups(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Group>> {
    return this.http.get<PageData<Group>>(`/api/tenant/groups${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public assignEntityToGroup(target: GroupRelation, config?: RequestConfig): Observable<Group> {
    return this.http.post<Group>('/api/assign/entity', target, defaultHttpOptionsFromConfig(config));
  }

   public unassignDeviceFromGroup(target: GroupRelation, config?: RequestConfig): Observable<Group> {
    return this.http.post<Group>('/api/unassign/entity', target, defaultHttpOptionsFromConfig(config));
  }
  public assignGroupToUser(userId: string, groupId: string, config?: RequestConfig): Observable<Group> {
    return this.http.post<Group>(`/api/group/${groupId}/user/${userId}`, null, defaultHttpOptionsFromConfig(config));
  }
  public getTenantGroup(groupId: string, config?: RequestConfig): Observable<Group> {
    return this.http.get<Group>(`/api/group/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveGroup(group: Group, config?: RequestConfig): Observable<Group> {
    return this.http.post<Group>('/api/group', group, defaultHttpOptionsFromConfig(config));
  }
  public deleteGroup(groupId: string, config?: RequestConfig) {
    return this.http.delete(`/api/group/${groupId}`, defaultHttpOptionsFromConfig(config));
  }

  public getGroupsByEntityId(entityId: EntityId, config?: RequestConfig): Observable<Group[]> {
    return this.http.get<Group[]>(`/api/groups/${entityId.entityType}/${entityId.id}`, defaultHttpOptionsFromConfig(config));

  }


 public getGroups(GroupIds: Array<string>, config?: RequestConfig): Observable<Array<Group>> {
    return this.http.get<Array<Group>>(`/api/groups?groupIds=${GroupIds.join(',')}`, defaultHttpOptionsFromConfig(config));
  }


}
