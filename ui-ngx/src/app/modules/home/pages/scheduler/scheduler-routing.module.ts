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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MenuId } from '@app/core/public-api';
import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { SchedulerTableConfigResolver } from './scheduler-table-config.resolver';

const routes: Routes = [
  {
    path: 'scheduler',
    data: {
      breadcrumb: {
        menuId: MenuId.scheduler
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          title: 'scheduler.title'
        },
        resolve: {
          entitiesTableConfig: SchedulerTableConfigResolver
        }
      },

    ]
  }

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    SchedulerTableConfigResolver
  ]
})
export class SchedulerRoutingModule { }
