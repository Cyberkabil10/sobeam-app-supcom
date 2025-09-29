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
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { PageData, PageLink } from '@app/shared/public-api';
import { Observable } from 'rxjs';
import { RequestConfig, defaultHttpOptionsFromConfig } from './http-utils';
import { Scheduler } from '@app/shared/models/scheduler.models';


@Injectable({
    providedIn: 'root'
})
export class SchedulerService {

    constructor(
        private http: HttpClient,
        private translate: TranslateService

    ) { }

    public getTenantScheduler(pageLink: PageLink, config?: RequestConfig): Observable<PageData<Scheduler>> {
        return this.http.get<PageData<Scheduler>>(`/api/tenant/schedulers${pageLink.toQuery()}`,
            defaultHttpOptionsFromConfig(config));
    }

    public saveScheduler(scheduler: Scheduler, config?: RequestConfig): Observable<Scheduler> {
        return this.http.post<Scheduler>('/api/scheduler', scheduler, defaultHttpOptionsFromConfig(config));
    }

    public deleteScheduler(schedulerId: string, config?: RequestConfig) {
        return this.http.delete(`/api/scheduler/${schedulerId}`, defaultHttpOptionsFromConfig(config));
    }
}