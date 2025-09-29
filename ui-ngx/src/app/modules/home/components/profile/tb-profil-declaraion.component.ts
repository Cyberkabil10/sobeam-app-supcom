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


import { Component, forwardRef, Input, OnInit, OnDestroy } from '@angular/core';
import {
  FormArray,
  FormGroup,
  Validators,
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  UntypedFormControl,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { guid } from '@app/core/utils';
import { DataType, valueTypesMap, ContentType, EntityId } from '@app/shared/public-api';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-profil-declaraion',
  templateUrl: './tb-profil-declaraion.component.html',
  styleUrls: ['./tb-profil-declaraion.component.scss'],
  providers: [
   
       {
          provide: NG_VALUE_ACCESSOR,
          useExisting: forwardRef(() => TbProfilDeclaraionComponent),
          multi: true
        },
        {
          provide: NG_VALIDATORS,
          useExisting: forwardRef(() => TbProfilDeclaraionComponent),
          multi: true,
        }
  ]
})
export class TbProfilDeclaraionComponent implements ControlValueAccessor, OnInit, OnDestroy ,Validator{

  form: UntypedFormGroup;

  valueTypeKeys = Object.keys(DataType);
  valueTypeEnum = DataType;
  valueTypes = valueTypesMap;
  contentTypes = ContentType;

  @Input()
  deviceProfileId: EntityId;

  private destroy$ = new Subject<void>();

  private onChange: (value: any) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(private fb: UntypedFormBuilder) {
    this.form = this.fb.group({
      telemetryFields: this.fb.array([]),
      actionFields: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(value => {
      this.onChange(value);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ControlValueAccessor
writeValue(value: any): void {
  // Réinitialise les tableaux
  this.clearFormArrays();

  if (value) {
    // Patch telemetryFields s’ils existent
    if (Array.isArray(value.telemetryFields)) {
      value.telemetryFields.forEach(field => {
        const telemetryGroup = this.fb.group({
          key: [field.key || '', Validators.required],
          dataType: [field.dataType || '', Validators.required],
          units: [field.units || '']
        });
        this.telemetryFields.push(telemetryGroup);
      });
    }

    // Patch actionFields s’ils existent
    if (Array.isArray(value.actionFields)) {
      value.actionFields.forEach(action => {
        const actionGroup = this.fb.group({
          id: action.id || guid(),
          method: [action.method || '', Validators.required],
          params: [action.params || null],
          timeout: [action.timeout || null, Validators.min(0)]
        });
        this.actionFields.push(actionGroup);
      });
    }

    // Important : ne pas déclencher valueChanges ici
    this.form.updateValueAndValidity({ emitEvent: false });
  }

  // Marquer comme touché si nécessaire
  this.onTouched();
}


  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.form.disable({ emitEvent: false });
    } else {
      this.form.enable({ emitEvent: false });
    }
  }

  // Getters
  get telemetryFields(): FormArray {
    return this.form.get('telemetryFields') as FormArray;
  }

  get actionFields(): FormArray {
    return this.form.get('actionFields') as FormArray;
  }

  // Validation helpers
  private areAllFieldsValid(array: FormArray, requiredKeys: string[]): boolean {
    return array.controls.every(control => {
      const group = control as FormGroup;
      return requiredKeys.every(k => {
        const value = group.get(k)?.value;
        return value !== null && value !== undefined && value !== '';
      });
    });
  }

   validate(c: UntypedFormControl): ValidationErrors | null {
      return (this.form.valid) ? null : {
        declarationConfiguration: {
          valid: false,
        },
      };
    }

  areAllTelemetryFieldsValid(): boolean {
    return this.areAllFieldsValid(this.telemetryFields, ['key', 'dataType']);
  }

  areAllActionFieldsValid(): boolean {
    return this.areAllFieldsValid(this.actionFields, ['method']);
  }

  // Touch helpers
  private markAllFieldsAsTouched(array: FormArray): void {
    array.controls.forEach(control => {
      const group = control as FormGroup;
      Object.keys(group.controls).forEach(key => {
        group.get(key)?.markAsTouched();
      });
    });
  }

  private markAllTelemetryFieldsAsTouched(): void {
    this.markAllFieldsAsTouched(this.telemetryFields);
  }

  private markAllActionFieldsAsTouched(): void {
    this.markAllFieldsAsTouched(this.actionFields);
  }

  // Add methods
  addTelemetryField(): void {
    if (!this.areAllTelemetryFieldsValid()) {
      this.markAllTelemetryFieldsAsTouched();
      return;
    }

    this.telemetryFields.push(this.fb.group({
      key: ['', Validators.required],
      dataType: ['', Validators.required],
      units: ['']
    }));
    this.onTouched();
  }

  addActionField(): void {
    if (!this.areAllActionFieldsValid()) {
      this.markAllActionFieldsAsTouched();
      return;
    }

    this.actionFields.push(this.fb.group({
      id: guid(),
      method: ['', Validators.required],
      params: [null],
      timeout: [null, Validators.min(0)]
    }));
    this.onTouched();
  }

  // Remove methods
  removeTelemetryField(index: number): void {
    if (index >= 0 && index < this.telemetryFields.length) {
      this.telemetryFields.removeAt(index);
      this.onTouched();
    }
  }

  removeActionField(index: number): void {
    if (index >= 0 && index < this.actionFields.length) {
      this.actionFields.removeAt(index);
      this.onTouched();
    }
  }

  // Reset method
  private clearFormArrays(): void {
    this.telemetryFields.clear();
    this.actionFields.clear();
  }
}
