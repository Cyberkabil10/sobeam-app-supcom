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
import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  CellActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Scheduler } from '@app/shared/models/scheduler.models';
import { SchedulerService } from '@app/core/http/scheduler.service';
import { SchedulerComponent } from './scheduler.component';
import { EntityAction } from '../../models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';

@Injectable()
export class SchedulerTableConfigResolver implements Resolve<EntityTableConfig<Scheduler>> {
  private readonly config: EntityTableConfig<Scheduler> = new EntityTableConfig<Scheduler>();

  constructor(private translate: TranslateService,
    private datePipe: DatePipe,
    private dialog: MatDialog,
    private schedulerService: SchedulerService) {
    this.config.detailsPanelEnabled = false;
    this.config.addEnabled = false;

    this.config.entityType = EntityType.SCHEDULER;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.SCHEDULER);
    this.config.entityResources = entityTypeResources.get(EntityType.SCHEDULER);
    this.config.cellActionDescriptors = this.configureCellActions();
    this.config.headerActionDescriptors = [{
      name: this.translate.instant('scheduler.add-scheduler'),
      icon: 'add',
      isEnabled: () => true,
      onAction: $event => this.onTargetAction({
        action: 'add',
        event: $event,
        entity: null
      })
    }];
    this.config.columns.push(
      new DateEntityTableColumn<Scheduler>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Scheduler>('name', 'scheduler.name', '30%'),
      new EntityTableColumn<Scheduler>('type', 'scheduler.type', '40%'),
    );

    this.config.deleteEntityTitle = scheduler => this.translate.instant('scheduler.delete-scheduler-title', { schedulerName: scheduler.name });
    this.config.deleteEntityContent = () => this.translate.instant('scheduler.delete-scheduler-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('scheduler.delete-schedulers-title', { count });
    this.config.deleteEntitiesContent = () => this.translate.instant('scheduler.delete-schedulers-text');

    this.config.entitiesFetchFunction = pageLink => this.schedulerService.getTenantScheduler(pageLink);
    this.config.deleteEntity = id => this.schedulerService.deleteScheduler(id.id);
    this.config.handleRowClick = ($event, target) => {
      this.editTarget($event, target);
      return true;
    };
  }

  resolve(): EntityTableConfig<Scheduler> {
    this.config.tableTitle = this.translate.instant('scheduler.scheduler');
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<Scheduler>> {
    return [{
      name: this.translate.instant('scheduler.toggle-status'),
      nameFunction: (entity) =>
        this.translate.instant(entity.enabled ? 'scheduler.disable' : 'scheduler.enable'),
      icon: 'mdi:toggle-switch',
      isEnabled: () => true,
      iconFunction: (entity) => entity.enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
      onAction: ($event, entity) => this.toggleEnableMode($event, entity)
    }];
  }

  private toggleEnableMode($event: Event, scheduler: Scheduler): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifyScheduler: Scheduler = {
      ...scheduler,
      enabled: !scheduler.enabled
    };

    this.schedulerService.saveScheduler(modifyScheduler, { ignoreLoading: true })
      .subscribe((result) => {
        scheduler.enabled = result.enabled;
        this.config.getTable().detectChanges();
      });
  }

  private onTargetAction(action: EntityAction<Scheduler>): boolean {
    switch (action.action) {
      case 'add':
        this.editTarget(action.event, action.entity, true);
        return true;
    }
    return false;
  }

  private editTarget($event: Event, target: Scheduler, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SchedulerComponent, any, Scheduler>(SchedulerComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          isAdd,
          scheduler: target
        }
      }).afterClosed()
      .subscribe((result) => {
        if (result) {
          this.config.updateData();
        }
      });
  }
}