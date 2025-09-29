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
import { isDefined } from '@core/utils';
import { UserId } from '@app/shared/public-api';
import { RoleId } from '@app/shared/models/id/role-Id';

@Injectable({
    providedIn: 'root'
})
export class RoleService {

    constructor(
        private http: HttpClient
    ) { }

     public getTenantRoles( pageLink:PageLink , config?: RequestConfig): Observable<PageData<Role>> {
        return this.http.get<PageData<Role>>(`/api/tenant/roles${pageLink.toQuery()}`,
          defaultHttpOptionsFromConfig(config));
      }

       public getTenantRole(roleId: string, config?: RequestConfig): Observable<Role> {
          return this.http.get<Role>(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
        }

       public getUserTenantRole(userId: string, config?: RequestConfig): Observable<any> {
        return this.http.get<any>(`/api/user/role/${userId}`, defaultHttpOptionsFromConfig(config));
      }

      public getUserRole(userId: string, config?: RequestConfig): Observable<any> {
        return this.http.get<any>(`/api/roles/user/${userId}`, defaultHttpOptionsFromConfig(config));
      }
     public saveRole(role: Role, config?: RequestConfig): Observable<Role> {
      console.log("this sve ou update role " , role)
        return this.http.post<Role>('/api/role', role, defaultHttpOptionsFromConfig(config));
      }
    public deleteRole(roleId: string, config?: RequestConfig) {
        return this.http.delete(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
    }
     public getRoles(roleIds: Array<string>, config?: RequestConfig): Observable<Array<Role>> {
        return this.http.get<Array<Role>>(`/api/roles?roleIds=${roleIds.join(',')}`, defaultHttpOptionsFromConfig(config));
      }

      public assignRolesToUser(roleIds: RoleId[], userId: UserId, config?: RequestConfig): Observable<any> {
        return this.http.post<any>(`/api/role/${userId.id}/assign`,roleIds, defaultHttpOptionsFromConfig(config));
      }

}
