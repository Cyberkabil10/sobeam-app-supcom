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
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { MatDialog } from '@angular/material/dialog';
import {
  ArgumentType,
  CalculatedField,
  CalculatedFieldEventArguments,
  CalculatedFieldType,
  CalculatedFieldTypeTranslations,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights
} from '@shared/models/calculated-field.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  CalculatedFieldDebugDialogComponent,
  CalculatedFieldDebugDialogData,
  CalculatedFieldDialogComponent,
  CalculatedFieldDialogData,
  CalculatedFieldScriptTestDialogComponent,
  CalculatedFieldTestScriptDialogData
} from '@home/components/calculated-fields/components/public-api';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { isObject } from '@core/utils';
import { Store } from '@ngrx/store';
import { Observable, filter, tap } from 'rxjs';
import { Direction } from '@shared/models/page/sort-order';
import { ImportExportService } from '@app/shared/import-export/import-export.service';

@Injectable()
export class CalculatedTableConfigResolver implements Resolve<EntityTableConfig<CalculatedField>> {
  private readonly config: EntityTableConfig<CalculatedField> = new EntityTableConfig<CalculatedField>();
  private authUser = getCurrentAuthUser(this.store);

  additionalDebugActionConfig = {
    title: this.translate.instant('calculated-fields.see-debug-events'),
    action: (calculatedField: CalculatedField) => this.openDebugEventsDialog(calculatedField),
  };

  constructor(
    private translate: TranslateService,
    private datePipe: DatePipe,
    private store: Store<AppState>,
    private dialog: MatDialog,
    private importExportService: ImportExportService,
    private calculatedFieldsService: CalculatedFieldsService
  ) {
    this.config.addEnabled = false;
    this.config.pageMode = false;
    this.config.detailsPanelEnabled = false;

    this.config.entityType = EntityType.CALCULATED_FIELD;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CALCULATED_FIELD);
    this.config.entityResources = entityTypeResources.get(EntityType.CALCULATED_FIELD);

    this.config.defaultSortOrder = { property: 'createdTime', direction: Direction.DESC };

    this.config.headerActionDescriptors = [{
      name: this.translate.instant('calculated-fields.add-fields'),
      icon: 'add',
      isEnabled: () => true,
      onAction: $event => this.onTargetAction({
        action: 'add',
        event: $event,
        entity: null
      })
    }];

    // Configure expression column with proper formatting
    const expressionColumn = new EntityTableColumn<CalculatedField>('expression', 'calculated-fields.expression', '300px');
    expressionColumn.sortable = false;
    expressionColumn.cellContentFunction = entity => {
      const expressionLabel = this.getExpressionLabel(entity);
      return expressionLabel.length < 45 ? expressionLabel : `<span style="display: inline-block; width: 45ch">${expressionLabel.substring(0, 44)}…</span>`;
    };
    expressionColumn.cellTooltipFunction = entity => {
      const expressionLabel = this.getExpressionLabel(entity);
      return expressionLabel.length < 45 ? null : expressionLabel;
    };

    this.config.columns.push(
      new DateEntityTableColumn<CalculatedField>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<CalculatedField>('name', 'common.name', '33%'),
      new EntityTableColumn<CalculatedField>('type', 'common.type', '50px', entity => this.translate.instant(CalculatedFieldTypeTranslations.get(entity.type))),
      expressionColumn
    );

    this.config.addEntity = this.getCalculatedFieldDialog.bind(this);
    this.config.deleteEntityTitle = (field: CalculatedField) => this.translate.instant('calculated-fields.delete-title', { title: field.name });
    this.config.deleteEntityContent = () => this.translate.instant('calculated-fields.delete-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('calculated-fields.delete-multiple-title', { count });
    this.config.deleteEntitiesContent = () => this.translate.instant('calculated-fields.delete-multiple-text');
    this.config.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);

    this.config.entitiesFetchFunction = pageLink => this.calculatedFieldsService.getTenantCalculatedField(pageLink);

    // Configure cell action descriptors
    this.config.cellActionDescriptors = [
      {
        name: this.translate.instant('calculated-fields.toggle-status'),
        nameFunction: (entity) =>
          this.translate.instant(entity.status ? 'calculated-fields.disable' : 'calculated-fields.enable'),
        icon: 'mdi:toggle-switch',
        isEnabled: () => true,
        iconFunction: (entity) => entity.status ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
        onAction: ($event, entity) => this.toggleEnableMode($event, entity)
      },
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: (event$, entity) => this.exportCalculatedField(event$, entity),
      },
      {
        name: this.translate.instant('entity-view.events'),
        icon: 'mdi:clipboard-text-clock',
        isEnabled: () => true,
        onAction: (_, entity) => this.openDebugEventsDialog(entity),
      },
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: (_, entity) => this.editCalculatedField(entity),
      },

    ];


  }
  private toggleEnableMode($event: Event, cf: CalculatedField): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifyCf: CalculatedField = {
      ...cf,
      status: !cf.status
    };

    this.calculatedFieldsService.saveCalculatedField(modifyCf, { ignoreLoading: true })
      .subscribe((result) => {
        cf.status = result.status;
        cf.version = result.version;
        this.config.getTable().detectChanges();
      });
  }


  resolve(): EntityTableConfig<CalculatedField> {
    this.config.tableTitle = this.translate.instant('calculated-fields.fields');
    return this.config;
  }

  private getExpressionLabel(entity: CalculatedField): string {
    if (entity.type === CalculatedFieldType.SCRIPT) {
      return 'function calculate(ctx, ' + Object.keys(entity.configuration.arguments).join(', ') + ')';
    } else {
      return entity.configuration.expression;
    }
  }

  private onTargetAction(action: EntityAction<CalculatedField>): boolean {
    switch (action.action) {
      case 'add':
        this.getCalculatedFieldDialog(action.entity, 'action.add', false).subscribe((res) => {
          if (res) {
            this.updateData();
          }
        });
        return true;
    }
    return false;
  }

  private getCalculatedFieldDialog(value?: CalculatedField, buttonTitle = 'action.add', isDirty = false): Observable<CalculatedField> {
    return this.dialog.open<CalculatedFieldDialogComponent, CalculatedFieldDialogData, CalculatedField>(CalculatedFieldDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle,
        entityId: { id: this.authUser.userId, entityType: EntityType.USER },
        tenantId: this.authUser.tenantId,
        entityName: 'tenant',
        additionalDebugActionConfig: this.additionalDebugActionConfig,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
        isDirty,
      },
      enterAnimationDuration: isDirty ? 0 : null,
    }).afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog(calculatedField: CalculatedField): void {
    this.dialog.open<CalculatedFieldDebugDialogComponent, CalculatedFieldDebugDialogData, null>(CalculatedFieldDebugDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId: this.authUser.tenantId,
        value: calculatedField,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
      }
    })
      .afterClosed()
      .subscribe();
  }

  private exportCalculatedField($event: Event, calculatedField: CalculatedField): void {
    if ($event) {
      $event.stopPropagation();
    }
    // Note: This would require ImportExportService to be injected
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private getTestScriptDialog(calculatedField: CalculatedField, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit = true): Observable<string> {
    const resultArguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
      const type = calculatedField.configuration.arguments[key].refEntityKey.type;
      acc[key] = isObject(argumentsObj) && argumentsObj.hasOwnProperty(key)
        ? { ...argumentsObj[key], type }
        : type === ArgumentType.Rolling ? { values: [], type } : { value: '', type, ts: new Date().getTime() };
      return acc;
    }, {});

    return this.dialog.open<CalculatedFieldScriptTestDialogComponent, CalculatedFieldTestScriptDialogData, string>(CalculatedFieldScriptTestDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        arguments: resultArguments,
        expression: calculatedField.configuration.expression,
        argumentsEditorCompleter: getCalculatedFieldArgumentsEditorCompleter(calculatedField.configuration.arguments),
        argumentsHighlightRules: getCalculatedFieldArgumentsHighlights(calculatedField.configuration.arguments),
        openCalculatedFieldEdit
      }
    }).afterClosed()
      .pipe(
        filter(Boolean),
        tap(expression => {
          if (openCalculatedFieldEdit) {
            this.editCalculatedField({
              ...calculatedField,
              configuration: { ...calculatedField.configuration, expression }
            }, true);
          }
        }),
      );
  }

  private editCalculatedField(calculatedField: CalculatedField, isDirty = false): void {
    this.getCalculatedFieldDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }


  private updateData(closeDetails = false): void {
    if (this.config.getTable()) {
      this.config.updateData(closeDetails);
    } else if (this.config.getEntityDetailsPage()) {
      this.config.getEntityDetailsPage().reload();
    }
  }
}