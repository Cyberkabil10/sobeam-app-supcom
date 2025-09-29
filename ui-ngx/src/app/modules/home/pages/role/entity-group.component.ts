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

import { ChangeDetectorRef, Component, Inject, Optional } from '@angular/core';
import { Group } from '@app/shared/models/group.models';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { AppState } from '@app/core/core.state';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '../../models/entity/entities-table-config.models';

import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';
import { EntityType } from '@app/shared/public-api';

@Component({
  selector: 'tb-entity-group',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslateModule,
  ],
  templateUrl: './entity-group.component.html',
  styleUrls: ['./entity-group.component.scss']
})
export class EntityGroupComponent extends EntityComponent<Group> {
  typeGroup = [
    { value: EntityType.DEVICE, label: this.translate.instant('entity.DEVICE') },
    { value: EntityType.ASSET, label: this.translate.instant('entity.ASSET') },
    { value: EntityType.DASHBOARD, label:  this.translate.instant('entity.DASHBOARD') },
    { value: EntityType.CUSTOMER, label:  this.translate.instant('entity.CUSTOMER') },
   // { value: EntityType.ENTITY_VIEW, label: 'Entity View'},
  ]; 

  constructor(
    protected store: Store<AppState>,
    protected translate: TranslateService,
    @Optional() @Inject('entity') protected entityValue: Group,
    @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Group>,
    protected fb: UntypedFormBuilder,
    protected cd: ChangeDetectorRef,
  ) { 
    super(store, fb, entityValue, entitiesTableConfigValue, cd); 
  }

  buildForm(entity: Group): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      type: [entity ? entity.type : [Validators.required]],
    });
    return form;
  }

  updateForm(entity: Group) {
    throw new Error('Method not implemented.');
  }

  onRoleTypeChange(event: any) {
    console.log('Role type changed:', event.value);
  }
}
