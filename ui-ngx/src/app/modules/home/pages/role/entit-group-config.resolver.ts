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
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Group } from '@app/shared/models/group.models';
import { EntityGroupComponent } from './entity-group.component';
import { GroupService } from '@app/core/http/group.service';
import { GroupId } from '@app/shared/models/id/group-id';
import { AssignToCustomerDialogComponent, AssignToCustomerDialogData } from '../../dialogs/assign-to-customer-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Injectable()
export class EntityGroupConfigResolver implements Resolve<EntityTableConfig<Group>> {
  private readonly config: EntityTableConfig<Group> = new EntityTableConfig<Group>();

  constructor(private translate: TranslateService,
    private datePipe: DatePipe,
    private dialog: MatDialog,
    private groupService: GroupService) {
    this.config.detailsPanelEnabled = false;

    this.config.entityType = EntityType.GROUP;
    this.config.entityComponent = EntityGroupComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.GROUP);
    this.config.entityResources = entityTypeResources.get(EntityType.GROUP);

    this.config.columns.push(
      new DateEntityTableColumn<Group>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Group>('name', 'common.name', '30%'),
      new EntityTableColumn<Group>('description', 'common.description', '40%'),
      new EntityTableColumn<Group>('type',  this.translate.instant('common.type'), '30%',  (group) => this.translate.instant(`entity.${group.type}`) ));



    this.config.deleteEntityTitle = group => this.translate.instant('group.delete-group-title', { groupTitle: group.name });
    this.config.deleteEntityContent = () => this.translate.instant('group.delete-group-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('group.delete-groups-title', { count });
    this.config.deleteEntitiesContent = () => this.translate.instant('group.delete-groups-text');

    this.config.entitiesFetchFunction = pageLink => this.groupService.getTenantGroups(pageLink);
    this.config.handleRowClick = ($event, group) => { return true; };
    this.config.saveEntity = group => this.groupService.saveGroup(group);
    this.config.deleteEntity = id => this.groupService.deleteGroup(id.id);

    this.config.headerComponent = null;
    this.config.cellActionDescriptors = this.configureCellActions();

  }

  configureCellActions(): Array<CellActionDescriptor<Group>> {
    const actions: Array<CellActionDescriptor<Group>> = [];
    actions.push(
      {
        name: this.translate.instant('group.assign-group-to-user'),
        icon: 'assignment_ind',
        isEnabled: (entity) => (true),
        onAction: ($event, entity) => this.assignToCustomer($event, [entity.id])
      }
    );

    return actions;
  }

  assignToCustomer($event: Event, deviceIds: Array<GroupId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData,
      boolean>(AssignToCustomerDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entityIds: deviceIds,
          entityType: EntityType.GROUP,
          assignTargetType: 'user'
        }
      }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }
  resolve(): EntityTableConfig<Group> {
    this.config.tableTitle = this.translate.instant('group.groups');
    return this.config;
  }

}

