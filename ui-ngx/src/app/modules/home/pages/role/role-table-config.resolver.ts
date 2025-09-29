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


///
/// Copyright © 2016-2024 The Sobeam Authors
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
import { Resolve, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { RoleTabsComponent } from './role-tabs.component';
import { Role } from '@app/shared/models/role.models';
import { RoleService } from '@app/core/http/role.service';
import { SelectRoleTypeComponent } from './select-role-type.component';
import { MatDialog } from '@angular/material/dialog';
import { roleType as RoleDataType } from '@shared/models/role.models';
@Injectable()
export class RoleTableConfigResolver implements Resolve<EntityTableConfig<Role>> {
  private readonly config: EntityTableConfig<Role> = new EntityTableConfig<Role>();

  constructor(private translate: TranslateService,
    private dialog: MatDialog,
    private router: Router,
    private datePipe: DatePipe,
    private roleService: RoleService) {

    this.config.entityType = EntityType.ROLE;
    // this.config.entityComponent = RoleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ROLE);
    this.config.entityResources = entityTypeResources.get(EntityType.ROLE);
    this.config.addEnabled = false;
    this.config.detailsPanelEnabled = false;

    this.config.columns.push(
      new DateEntityTableColumn<Role>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Role>('name', 'role.name', '30%'),
      new EntityTableColumn<Role>('description', 'role.description', '40%'),
      new EntityTableColumn<Role>('type', 'role.type', '30%', (role) => this.translate.instant(`role.${role.type}`)));



    this.config.headerActionDescriptors = [{
      name: this.translate.instant('role.add'),
      icon: 'add',
      isEnabled: () => true,
      onAction: $event => this.onTargetAction($event),
    }];


    this.config.deleteEntityTitle = role => this.translate.instant('role.delete-role-title', { roleName: role.name });
    this.config.deleteEntityContent = () => this.translate.instant('role.delete-role-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('role.delete-roles-title', { count });
    this.config.deleteEntitiesContent = () => this.translate.instant('role.delete-roles-text');

    this.config.handleRowClick = ($event, role) => {
      if ($event) {
        $event.stopPropagation();
      }
      this.roleService.getTenantRole(role.id.id).subscribe(
        (role) => {
          this.dialog.open<RoleTabsComponent, any, RoleDataType>(RoleTabsComponent, {
            data: {
              status: false,
              role: role
            },
            disableClose: true,
            panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
          }).afterClosed().subscribe(
            (result) => {
              this.config.updateData();
            });
        }
      );
      return false;
    };
    this.config.entitiesFetchFunction = pageLink => this.roleService.getTenantRoles(pageLink);
    this.config.loadEntity = id => this.roleService.getTenantRole(id.id);
    //  this.config.saveEntity = role => this.roleService.saveRole(role);
    this.config.deleteEntity = id => this.roleService.deleteRole(id.id);
  }

  resolve(): EntityTableConfig<Role> {
    this.config.tableTitle = this.translate.instant('role.role');
    return this.config;
  }

  private onTargetAction($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SelectRoleTypeComponent, any, RoleDataType>(SelectRoleTypeComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe(
      (type) => {
        this.dialog.open(RoleTabsComponent, {
          data: {
            status: true,
            type: type,
          },
        }).afterClosed().subscribe(
          (result) => {
            this.config.updateData();
          });
      }
    );
  }

}

