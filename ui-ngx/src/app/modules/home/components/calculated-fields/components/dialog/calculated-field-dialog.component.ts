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

import { Component, DestroyRef, Inject, ViewEncapsulation } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  ActionType,
  ActionTypeTranslations,
  ArgumentEntityType,
  ArgumentEntityTypeParamsMap,
  ArgumentEntityTypeTranslations,
  CalculatedField,
  CalculatedFieldConfiguration,
  calculatedFieldDefaultScript,
  CalculatedFieldTestScriptFn,
  CalculatedFieldType,
  CalculatedFieldTypeTranslations,
  CalculatedFieldTypeV2,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  OutputType,
  OutputTypeTranslations
} from '@shared/models/calculated-field.models';
import { digitsRegex, oneSpaceInsideRegex } from '@shared/models/regex.constants';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { EntityType } from '@shared/models/entity-type.models';
import { map, startWith, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';
import { Device, NotificationTarget, NotificationType } from '@app/shared/public-api';
import { DeviceProfileService, DeviceService } from '@app/core/public-api';
import { MatButton } from '@angular/material/button';
import { RecipientNotificationDialogComponent, RecipientNotificationDialogData } from '@app/modules/home/pages/notification/recipient/recipient-notification-dialog.component';

export interface CalculatedFieldDialogData {
  value?: CalculatedField;
  buttonTitle: string;
  entityId: EntityId;
  tenantId: string;
  entityName?: string;
  additionalDebugActionConfig: AdditionalDebugActionConfig<(calculatedField: CalculatedField) => void>;
  getTestScriptDialogFn: CalculatedFieldTestScriptFn;
  isDirty?: boolean;
}

@Component({
  selector: 'tb-calculated-field-dialog',
  templateUrl: './calculated-field-dialog.component.html',
  styleUrls: ['./calculated-field-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldDialogComponent extends DialogComponent<CalculatedFieldDialogComponent, CalculatedField> {
  entityType = EntityType;
  rpcSet: any;
  profileId: string;
  notificationType = NotificationType;
  ArgumentEntityType = ArgumentEntityType;
  argumentEntityTypes = Array.from(ArgumentEntityTypeTranslations.keys());
  argumentEntityTypeTranslations = ArgumentEntityTypeTranslations;
  readonly ArgumentEntityTypeParamsMap = ArgumentEntityTypeParamsMap;

  private currentOutputValues: any = {};

  sourceForm = new FormGroup({
    type: new FormControl(this.argumentEntityTypes[0] || ''),
    entity: new FormControl(''),
  });
  fieldFormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
    type: [CalculatedFieldType.SIMPLE],
    debugSettings: [],
    configuration: this.fb.group({
      arguments: this.fb.control({}),
      expressionSIMPLE: ['', [Validators.required, Validators.pattern(oneSpaceInsideRegex), Validators.maxLength(255)]],
      expressionSCRIPT: [calculatedFieldDefaultScript],
      output: this.fb.group({}), // Start with empty group
      action: ['', [Validators.required]],
    }),
  });

  functionArgs$ = this.configFormGroup.get('arguments').valueChanges
    .pipe(
      startWith(this.data.value?.configuration?.arguments ?? {}),
      map(argumentsObj => ['ctx', ...Object.keys(argumentsObj)])
    );

  argumentsEditorCompleter$ = this.configFormGroup.get('arguments').valueChanges
    .pipe(
      startWith(this.data.value?.configuration?.arguments ?? {}),
      map(argumentsObj => getCalculatedFieldArgumentsEditorCompleter(argumentsObj))
    );

  argumentsHighlightRules$ = this.configFormGroup.get('arguments').valueChanges
    .pipe(
      startWith(this.data.value?.configuration?.arguments ?? {}),
      map(argumentsObj => getCalculatedFieldArgumentsHighlights(argumentsObj))
    );

  additionalDebugActionConfig = this.data.value?.id ? {
    ...this.data.additionalDebugActionConfig,
    action: () => this.data.additionalDebugActionConfig.action({ id: this.data.value.id, ...this.fromGroupValue }),
  } : null;

  readonly OutputTypeTranslations = OutputTypeTranslations;
  readonly ActionTypeTranslations = ActionTypeTranslations;
  readonly OutputType = OutputType;
  readonly ActionType = ActionType;
  readonly AttributeScope = AttributeScope;
  readonly EntityType = EntityType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly ScriptLanguage = ScriptLanguage;
  readonly fieldTypes = Object.values(CalculatedFieldType) as CalculatedFieldType[];
  readonly fieldTypesV2 = Object.values(CalculatedFieldTypeV2) as CalculatedFieldTypeV2[];
  readonly outputTypes = Object.values(OutputType) as OutputType[];
  readonly actionTypes = Object.values(ActionType) as ActionType[];
  readonly CalculatedFieldTypeTranslations = CalculatedFieldTypeTranslations;

  constructor(protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldDialogData,
    protected dialogRef: MatDialogRef<CalculatedFieldDialogComponent, CalculatedField>,
    private calculatedFieldsService: CalculatedFieldsService,
    private destroyRef: DestroyRef,
    private dialog: MatDialog,
    private deviceProfileService: DeviceProfileService,
    private deviceService: DeviceService,
    private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.observeIsLoading();
    this.applyDialogData();
    this.observeTypeChanges();
    this.observeActionChanges();
  }

  get configFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration') as FormGroup;
  }

  get outputFormGroup(): FormGroup {
    return this.fieldFormGroup.get('configuration').get('output') as FormGroup;
  }
  get sourceEntityType(): EntityType | null {
    const sourceType = this.sourceForm.get('type')?.value;
    if (!sourceType) return null;

    const params = ArgumentEntityTypeParamsMap.get(sourceType as ArgumentEntityType);
    return params?.entityType || null;
  }


  get fromGroupValue(): CalculatedField {
    const { configuration, type, name, ...rest } = this.fieldFormGroup.value;
    const { expressionSIMPLE, expressionSCRIPT, output, ...restConfig } = configuration;
    return {
      configuration: {
        ...restConfig,
        type,
        expression: configuration['expression' + type]?.trim?.() || '',
        output: this.sanitizeOutputValue(output)
      },
      name: name?.trim?.() || '',
      type,
      ...rest,
    } as CalculatedField;
  }

  private sanitizeOutputValue(output: any): any {
    if (!output) return {};

    const sanitized: any = {};
    Object.keys(output).forEach(key => {
      const value = output[key];
      if (typeof value === 'string') {
        sanitized[key] = value.trim();
      } else {
        sanitized[key] = value;
      }
    });
    return sanitized;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.fieldFormGroup.valid) {
      if (this.data.entityId.entityType === EntityType.USER) {
        const sourceEntity = this.sourceForm.get('entity')?.value;
        const sourceType = this.sourceForm.get('type')?.value;

        if (sourceType !== "CURRENT") {
          this.data.entityId = {
            id: sourceEntity,
            entityType: ArgumentEntityTypeParamsMap.get(sourceType as ArgumentEntityType)?.entityType
          }
        }
      }
      this.calculatedFieldsService.saveCalculatedField({
        entityId: this.data.entityId,
        ...(this.data.value ?? {}),
        ...this.fromGroupValue
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: calculatedField => this.dialogRef.close(calculatedField),
          error: err => {
            console.error('Error saving calculated field', err, this.data.value, this.fromGroupValue);
          }
        });
    }
  }

  onTestScript(): void {
    const calculatedFieldId = this.data.value?.id?.id;
    let testScriptDialogResult$: Observable<string>;

    if (calculatedFieldId) {
      testScriptDialogResult$ = this.calculatedFieldsService.getLatestCalculatedFieldDebugEvent(calculatedFieldId)
        .pipe(
          switchMap(event => {
            const args = event?.arguments ? JSON.parse(event.arguments) : null;
            return this.data.getTestScriptDialogFn(this.fromGroupValue, args, false);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
    } else {
      testScriptDialogResult$ = this.data.getTestScriptDialogFn(this.fromGroupValue, null, false);
    }

    testScriptDialogResult$.subscribe(expression => {
      this.configFormGroup.get('expressionSCRIPT').setValue(expression);
      this.configFormGroup.get('expressionSCRIPT').markAsDirty();
    });
  }

  private applyDialogData(): void {
    const {
      configuration = {},
      type = CalculatedFieldType.SIMPLE,
      debugSettings = { failuresEnabled: true, allEnabled: true },
      ...value
    } = this.data.value ?? {};

    const { expression, output = {}, action, ...restConfig } = configuration as CalculatedFieldConfiguration;

    // Store output values for later use
    this.currentOutputValues = { ...output };

    // Prepare configuration with expressions
    const updatedConfig = {
      ...restConfig,
      ['expression' + type]: expression || (type === CalculatedFieldType.SCRIPT ? calculatedFieldDefaultScript : ''),
      action: action || ''
    };

    // Patch the form without output first
    this.fieldFormGroup.patchValue({
      configuration: updatedConfig,
      type,
      debugSettings,
      ...value
    }, { emitEvent: false });
    if (this.data.value) {
      this.sourceForm.patchValue({
        type: this.data.value.entityId?.entityType,
        entity: this.data.value.entityId.id || ''
      }, { emitEvent: false });
    }
    // Setup output form based on action type
    if (action) {
      this.updateOutputFormGroup(action, this.currentOutputValues);
    } else {
      this.setupDefaultOutputFormGroup();
    }
  }

  private observeActionChanges(): void {
    this.configFormGroup.get('action').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(action => {
        // Preserve current output values before changing structure
        this.preserveCurrentOutputValues();
        this.updateOutputFormGroup(action, this.currentOutputValues);
      });
  }

  private preserveCurrentOutputValues(): void {
    const currentOutputFormValue = this.outputFormGroup.value || {};
    this.currentOutputValues = { ...this.currentOutputValues, ...currentOutputFormValue };
  }

  private setupDefaultOutputFormGroup(): void {
    const outputFormGroup = this.configFormGroup.get('output') as FormGroup;

    // Clear existing controls
    Object.keys(outputFormGroup.controls).forEach(key => {
      outputFormGroup.removeControl(key);
    });

    // Add default output controls for UpdateAttribute
    outputFormGroup.addControl('name', this.fb.control('', [
      Validators.required,
      Validators.pattern(oneSpaceInsideRegex),
      Validators.maxLength(255)
    ]));
    outputFormGroup.addControl('scope', this.fb.control({
      value: AttributeScope.SERVER_SCOPE,
      disabled: true
    }));
    outputFormGroup.addControl('type', this.fb.control(OutputType.Timeseries));
    outputFormGroup.addControl('decimalsByDefault', this.fb.control(null, [
      Validators.min(0),
      Validators.max(15),
      Validators.pattern(digitsRegex)
    ]));

    // Apply preserved values
    this.applyPreservedValues(outputFormGroup, this.currentOutputValues);
  }

  private updateOutputFormGroup(action: string, preservedValues: any = {}): void {
    const outputFormGroup = this.configFormGroup.get('output') as FormGroup;

    // Clear existing controls
    Object.keys(outputFormGroup.controls).forEach(key => {
      outputFormGroup.removeControl(key);
    });

    if (!action) {
      return;
    }

    switch (action) {
      case ActionType.SendRpc:

        outputFormGroup.addControl('profileId', this.fb.control('', [Validators.required]));
        outputFormGroup.addControl('id', this.fb.control('', [Validators.required]));
        outputFormGroup.addControl('method', this.fb.control('', [Validators.required]));

        // Load RPC configuration
        this.loadRpcConfiguration();
        break;

      case ActionType.UpdateAttribute:
        outputFormGroup.addControl('name', this.fb.control('', [
          Validators.required,
          Validators.pattern(oneSpaceInsideRegex),
          Validators.maxLength(255)
        ]));
        outputFormGroup.addControl('scope', this.fb.control({
          value: AttributeScope.SERVER_SCOPE,
          disabled: true
        }));
        outputFormGroup.addControl('type', this.fb.control(OutputType.Timeseries));
        outputFormGroup.addControl('decimalsByDefault', this.fb.control(null, [
          Validators.min(0),
          Validators.max(15),
          Validators.pattern(digitsRegex)
        ]));
        break;

      case ActionType.SendSms:
        outputFormGroup.addControl('phone', this.fb.control('', [Validators.required]));
        outputFormGroup.addControl('bodySms', this.fb.control('', [
          Validators.required,
          Validators.maxLength(160)
        ]));
        break;

      case ActionType.SendEmail:
        outputFormGroup.addControl('email', this.fb.control('', [Validators.required]));
        outputFormGroup.addControl('subject', this.fb.control('', [Validators.required]));
        outputFormGroup.addControl('bodyEmail', this.fb.control('', [Validators.required]));
        break;

      case ActionType.sendNotification:
        outputFormGroup.addControl('template', this.fb.control(null, [Validators.required]));
        outputFormGroup.addControl('targets', this.fb.control(null, [Validators.required]));
        break;

      default:
        break;
    }

    // Apply preserved values after controls are created
    this.applyPreservedValues(outputFormGroup, preservedValues);
  }

  private applyPreservedValues(formGroup: FormGroup, preservedValues: any): void {
    if (!preservedValues || typeof preservedValues !== 'object') {
      return;
    }

    Object.keys(preservedValues).forEach(key => {
      const control = formGroup.get(key);
      if (control && preservedValues[key] !== undefined && preservedValues[key] !== null) {
        control.setValue(preservedValues[key], { emitEvent: false });
      }
    });
  }

  private loadRpcConfiguration(): void {
    if (this.data.entityId?.entityType === EntityType.DEVICE || this.sourceForm.get('type')?.value === EntityType.DEVICE) {
      this.deviceService.getDevice(this.sourceForm.get('entity').value || this.data.entityId.id).subscribe((device: Device) => {
        this.profileId = device.deviceProfileId?.id;
        if (this.profileId) {
          this.deviceProfileService.getDeviceProfile(this.profileId).subscribe((deviceProfile) => {
            this.rpcSet = deviceProfile.profileData?.declarationConfiguration?.actionFields || [];
          });
        }
      });
    }
  }

  onRpcMethodSelected(id: string): void {
    if (!this.rpcSet || !Array.isArray(this.rpcSet)) {
      console.warn('RPC set not available or not an array');
      return;
    }

    const selected = this.rpcSet.find(opt => opt.id === id);
    const paramsMethod = this.outputFormGroup.get('method');
    const paramsId = this.outputFormGroup.get('id');
    const paramsProfilId = this.outputFormGroup.get('profileId');



    if (paramsMethod && paramsId) {
      paramsMethod.setValue(selected.method);
      paramsId.setValue(selected.id);
      paramsProfilId.setValue(this.profileId);

    } else {
      console.warn('Controls or selected method not found', {
        id,
        selected,
      });
    }
  }

  private observeTypeChanges(): void {
    const currentType = this.fieldFormGroup.get('type').value;
    const currentOutputType = this.outputFormGroup.get('type')?.value;

    this.toggleKeyByCalculatedFieldType(currentType);
    if (currentOutputType) {
      this.toggleScopeByOutputType(currentOutputType);
    }

    this.outputFormGroup.get('type')?.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => this.toggleScopeByOutputType(type));

    this.fieldFormGroup.get('type').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(type => this.toggleKeyByCalculatedFieldType(type));
  }

  private toggleScopeByOutputType(type: OutputType): void {
    const scopeControl = this.outputFormGroup.get('scope');
    if (scopeControl) {
      if (type === OutputType.Attribute) {
        scopeControl.enable({ emitEvent: false });
      } else {
        scopeControl.disable({ emitEvent: false });
      }
    }
  }

  private toggleKeyByCalculatedFieldType(type: CalculatedFieldType): void {
    const nameControl = this.outputFormGroup.get('name');
    const expressionSimpleControl = this.configFormGroup.get('expressionSIMPLE');
    const expressionScriptControl = this.configFormGroup.get('expressionSCRIPT');

    if (type === CalculatedFieldType.SIMPLE) {
      nameControl?.enable({ emitEvent: false });
      expressionSimpleControl?.enable({ emitEvent: false });
      expressionScriptControl?.disable({ emitEvent: false });
    } else {
      nameControl?.disable({ emitEvent: false });
      expressionSimpleControl?.disable({ emitEvent: false });
      expressionScriptControl?.enable({ emitEvent: false });
    }
  }

  private observeIsLoading(): void {
    this.isLoading$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(loading => {
      if (loading) {
        this.fieldFormGroup.disable({ emitEvent: false });
      } else {
        this.fieldFormGroup.enable({ emitEvent: false });

        // Re-apply type-specific toggles
        const currentType = this.fieldFormGroup.get('type').value;
        const currentOutputType = this.outputFormGroup.get('type')?.value;

        this.toggleKeyByCalculatedFieldType(currentType);
        if (currentOutputType) {
          this.toggleScopeByOutputType(currentOutputType);
        }

        if (this.data.isDirty) {
          this.fieldFormGroup.markAsDirty();
        }
      }
    });
  }

  createTarget($event: Event, button: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    button._elementRef.nativeElement.blur();

    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData, NotificationTarget>(
      RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }
    ).afterClosed().subscribe((res) => {
      if (res) {
        const targetsControl = this.outputFormGroup.get('targets');
        if (targetsControl) {
          let formValue: string[] = targetsControl.value || [];
          formValue.push(res.id.id);
          targetsControl.patchValue(formValue);
        }
      }
    });
  }
}