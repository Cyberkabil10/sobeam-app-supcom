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

import {
  ChangeDetectorRef,
  Component,
  Inject,
  OnDestroy,
  OnInit,
  Optional
} from '@angular/core';
import {
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators,
  FormControl,
  FormGroup
} from '@angular/forms';
import { Scheduler } from '@app/shared/models/scheduler.models';
import { AppState } from '@app/core/core.state';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '../../models/entity/entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import {
  ContentType,
  DatasourceType,
  DialogComponent
} from '@app/shared/public-api';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { deepTrim, isDefinedAndNotNull } from '@core/utils';
import { SchedulerService } from '@app/core/http/scheduler.service';
import { Router } from '@angular/router';
import { Subject, Observable, of } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

interface Attribute {
  key: string;
  value: any;
}

@Component({
  selector: 'tb-scheduler',
  templateUrl: './scheduler.component.html',
  styleUrls: ['./scheduler.component.scss']
})
export class SchedulerComponent extends DialogComponent<SchedulerComponent> implements OnInit, OnDestroy {
  entityType = EntityType;
  datasourceTypes: Array<DatasourceType> = [];
  contentTypes = ContentType;
  attributes: Array<Attribute> = [];
  schedulerForm: FormGroup;
  isLoading$: Observable<boolean> = of(false);
  attributeFormGroup: UntypedFormGroup;
  private readonly destroy$ = new Subject<void>();
  isAdd = true;

  constructor(
    protected store: Store<AppState>,
    protected translate: TranslateService,
    private schedulerService: SchedulerService,
    protected router: Router,
    protected dialogRef: MatDialogRef<SchedulerComponent>,
    @Optional() @Inject('entity') protected entityValue: Scheduler,
    @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Scheduler>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private fb: UntypedFormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(store, router, dialogRef);
    if (isDefinedAndNotNull(data?.isAdd)) {
      this.isAdd = data.isAdd;
    }
  }

  ngOnInit(): void {
    this.initForms();
    this.setupFormListeners();

    const schedulerData = this.data?.scheduler;
    if (schedulerData) {
      const patchData = { ...schedulerData };

      if (typeof patchData.scheduler?.startTime === 'number') {
        patchData.scheduler.startTime = new Date(patchData.scheduler.startTime);
      }

      this.schedulerForm.patchValue(patchData, { emitEvent: false });

      if (patchData.type) {
        this.handleTypeChange(patchData.type);
      }

      if(patchData.type ==='updateAttribute' && !patchData.configuration?.msgBody) {
      if (patchData.configuration?.msgBody) {
        this.initializeAttributes(patchData.configuration.msgBody);
      }}

      if (patchData.scheduler?.repeatEnabled) {
        this.handleRepeatEnabledChange(true);
      }
    }
  }

  private initForms(): void {
    this.schedulerForm = this.fb.group({
      name: ['', Validators.required],
      type: ['', Validators.required],
      enabled: [true],
      configuration: this.fb.group({
        originatorId: [null, Validators.required],
        msgType: [''],
        msgBody: this.fb.group({}),
        metadata: this.fb.group({
          oneway: [{ value: false, disabled: true }],
          timeout: [{ value: 5000, disabled: true }, [Validators.min(1000)]],
          persistent: [{ value: false, disabled: true }],
          scope: [{ value: '', disabled: true }]
        })
      }),
      scheduler: this.fb.group({
        startTime: [new Date(), Validators.required],
        timezone: [Intl.DateTimeFormat().resolvedOptions().timeZone],
        repeatEnabled: [false],
        repeat: this.fb.group({
          type: [{ value: 'TIMER', disabled: true }, Validators.required],
          endsOn: [{ value: new Date(), disabled: true }],
          repeatInterval: [{ value: null, disabled: true }, [Validators.required, Validators.min(1)]],
          timeUnit: [{ value: 'minutes', disabled: true }, Validators.required]
        })
      })
    });

    this.attributeFormGroup = this.fb.group({
      key: ['', [Validators.required, Validators.maxLength(255)]],
      value: ['', Validators.required]
    });
  }

  private setupFormListeners(): void {
    this.schedulerForm.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: string) => this.handleTypeChange(type));

    this.schedulerForm.get('scheduler.repeatEnabled').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((enabled: boolean) => this.handleRepeatEnabledChange(enabled));
  }

  private handleTypeChange(type: string): void {
    const msgBodyGroup = this.schedulerForm.get('configuration.msgBody') as FormGroup;
    const metadata = this.schedulerForm.get('configuration.metadata') as FormGroup;

    this.clearFormGroup(msgBodyGroup);
    this.disableFormGroup(metadata);

    switch (type) {
      case 'sendRpcRequest':
        msgBodyGroup.addControl('method', new FormControl('', Validators.required));
        msgBodyGroup.addControl('params', new FormControl(''));
        this.schedulerForm.get('configuration.msgType').setValue('RPC_CALL_FROM_SERVER_TO_DEVICE', { emitEvent: false });

        ['timeout', 'oneway', 'persistent'].forEach(field => metadata.get(field).enable({ emitEvent: false }));

        const msgBody = this.data?.scheduler?.configuration?.msgBody;
        if (msgBody) {
          if (msgBody.method) msgBodyGroup.get('method').setValue(msgBody.method, { emitEvent: false });
          if (msgBody.params) msgBodyGroup.get('params').setValue(msgBody.params, { emitEvent: false });
        }
        break;

      case 'updateAttribute':
        this.schedulerForm.get('configuration.msgType').setValue('POST_ATTRIBUTES_REQUEST', { emitEvent: false });
        metadata.get('scope').enable({ emitEvent: false });
        break;
    }
  }

  private handleRepeatEnabledChange(enabled: boolean): void {
    const repeatGroup = this.schedulerForm.get('scheduler.repeat') as FormGroup;
    const repeatData = this.data?.scheduler?.scheduler?.repeat;

    Object.entries(repeatGroup.controls).forEach(([key, control]) => {
      if (enabled) {
        control.enable({ emitEvent: false });
        if (repeatData?.[key]) {
          const value = key === 'endsOn' && typeof repeatData[key] === 'number'
            ? new Date(repeatData[key])
            : repeatData[key];
          control.setValue(value, { emitEvent: false });
        }
      } else {
        control.reset({ value: null, disabled: true }, { emitEvent: false });
      }
    });
  }

  private clearFormGroup(group: FormGroup) {
    Object.keys(group.controls).forEach(control => group.removeControl(control));
  }

  private disableFormGroup(group: FormGroup) {
    Object.keys(group.controls).forEach(control => group.get(control).disable({ emitEvent: false }));
  }

  private initializeAttributes(msgBody: any): void {
    if (!msgBody) return;
    this.attributes = Object.entries(msgBody).map(([key, value]) => ({ key, value }));
    this.updateMsgBodyInForm();
  }

  private updateMsgBodyInForm(): void {
    const msgBodyGroup = this.schedulerForm.get('configuration.msgBody') as FormGroup;
    this.clearFormGroup(msgBodyGroup);
    this.attributes.forEach(attr => msgBodyGroup.addControl(attr.key, new FormControl(attr.value)));
  }

  addAttribute() {
    if (this.attributeFormGroup.valid) {
      const { key, value } = this.attributeFormGroup.value;
      const existing = this.attributes.find(attr => attr.key === key);
      if (existing) {
        existing.value = value;
      } else {
        this.attributes.push({ key, value });
      }
      this.updateMsgBodyInForm();
      this.attributeFormGroup.reset();
      this.cd.markForCheck();
    }
  }

  removeAttribute(index: number) {
    if (index >= 0 && index < this.attributes.length) {
      this.attributes.splice(index, 1);
      this.updateMsgBodyInForm();
      this.cd.markForCheck();
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    if (this.schedulerForm.invalid) return;
    let formValue = this.prepareFormData();
    if (isDefinedAndNotNull(this.data.scheduler)) {
      formValue = { ...this.data.scheduler, ...formValue };
    }
    this.schedulerService.saveScheduler(formValue).subscribe(
      result => this.dialogRef.close(result)
    );
  }

  private prepareFormData(): any {
    const formValue = deepTrim(this.schedulerForm.value);
    formValue.scheduler.startTime = new Date(this.schedulerForm.value.scheduler.startTime).getTime();
    if (formValue.scheduler?.repeat?.endsOn) {
      formValue.scheduler.repeat.endsOn = new Date(this.schedulerForm.value.scheduler.repeat.endsOn).getTime();
    }
    return this.entityValue ? { ...this.entityValue, ...formValue } : formValue;
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
