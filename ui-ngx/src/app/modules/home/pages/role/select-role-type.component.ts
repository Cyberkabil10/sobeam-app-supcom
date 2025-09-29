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

import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppState } from '@app/core/core.state';
import { roleType, RoleTypesData } from '@app/shared/models/role.models';
import { DialogComponent } from '@app/shared/public-api';
import { Store } from '@ngrx/store';

@Component({
  selector: 'tb-select-role-type',
  templateUrl: './select-role-type.component.html',
  styleUrl: './select-role-type.component.scss'
})
export class SelectRoleTypeComponent extends DialogComponent<SelectRoleTypeComponent, roleType> {
  roleTypes = roleType;

  allWidgetTypes = Object.keys(roleType);

  roleTypesDataMap = RoleTypesData;

  constructor(protected store: Store<AppState>, protected router: Router, public dialogRef: MatDialogRef<SelectRoleTypeComponent, roleType>) {
    super(store, router, dialogRef);
  }

  cancel(): void {
  this.dialogRef.close();
  }

  typeSelected(type: roleType) {
    this.dialogRef.close(type);
  }
}
