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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable } from 'rxjs';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EdgeService } from '@core/http/edge.service';
import { GroupService } from '@app/core/http/group.service';
import { GroupRelation } from '@app/shared/models/group.models';
import { TranslateService } from '@ngx-translate/core';

export interface AssignToCustomerDialogData {
  entityIds: Array<EntityId>;
  entityType: EntityType;
  assignTargetType: string;
}

@Component({
  selector: 'tb-assign-to-customer-dialog',
  templateUrl: './assign-to-customer-dialog.component.html',
  providers: [{ provide: ErrorStateMatcher, useExisting: AssignToCustomerDialogComponent }],
  styleUrls: []
})
export class AssignToCustomerDialogComponent extends
  DialogComponent<AssignToCustomerDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  assignToCustomerFormGroup: UntypedFormGroup;

  submitted = false;

  entityType = EntityType;
  assignToCustomerTitle: string;
  assignToCustomerText: string;
  assignTargetType: string;

  constructor(protected store: Store<AppState>,
    protected router: Router,
    @Inject(MAT_DIALOG_DATA) public data: AssignToCustomerDialogData,
    private deviceService: DeviceService,
    private assetService: AssetService,
    private edgeService: EdgeService,
    private groupService: GroupService,
    private entityViewService: EntityViewService,
    @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
    public dialogRef: MatDialogRef<AssignToCustomerDialogComponent, boolean>,
    public fb: UntypedFormBuilder,
    private translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.assignToCustomerFormGroup = this.fb.group({
      customerId: [null, [Validators.required]]
    });


    switch (this.data.entityType) {
      case EntityType.DEVICE:
        this.assignToCustomerTitle = `device.assign-device-to-${this.data.assignTargetType}`;
        this.assignToCustomerText = `device.assign-to-${this.data.assignTargetType}-text`;
        break;

      case EntityType.ASSET:
        this.assignToCustomerTitle = `asset.assign-asset-to-${this.data.assignTargetType}`;
        this.assignToCustomerText = `asset.assign-to-${this.data.assignTargetType}-text`;
        break;

      case EntityType.GROUP:
        this.assignToCustomerTitle =  `group.assign-group-to-${this.data.assignTargetType}`;
        this.assignToCustomerText =  `group.assign-to-${this.data.assignTargetType}-text`;
        break;

      case EntityType.DASHBOARD:
        this.assignToCustomerTitle =this.translate.instant( `dashboard.assign-dashboard-to-${this.data.assignTargetType}`);
        this.assignToCustomerText = this.translate.instant(`dashboard.assign-to-${this.data.assignTargetType}-text`);
        break;

      case EntityType.EDGE:
        this.assignToCustomerTitle = `edge.assign-edge-to-${this.data.assignTargetType}`;
        this.assignToCustomerText = `edge.assign-to-${this.data.assignTargetType}-text`;
        break;

      case EntityType.CUSTOMER:
        this.assignToCustomerTitle = `customer.assign-customer-to-${this.data.assignTargetType}`;
        this.assignToCustomerText = `customer.assign-to-${this.data.assignTargetType}-text`;
        break;
      case EntityType.ENTITY_VIEW:
        this.assignToCustomerTitle = `entity-view.assign-entity-view-to-${this.data.assignTargetType}`;
        this.assignToCustomerText = `entity-view.assign-to-${this.data.assignTargetType}-text`;
        break;
    }

  }


  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  assign(): void {
    this.submitted = true;
    const customerId: string = this.assignToCustomerFormGroup.get('customerId').value;
    const tasks: Observable<any>[] = [];
    this.data.entityIds.forEach(
      (entityId) => {
        tasks.push(this.getAssignToCustomerTask(customerId, entityId.id));
      }
    );
    forkJoin(tasks).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }


  private getAssignToCustomerTask(customerId: string, entityId: string): Observable<any> {
    switch (this.data.entityType) {
      case EntityType.DEVICE:
        if (this.data.assignTargetType === 'group') {
          const groupRelation: GroupRelation = {
            groupId: { entityType: EntityType.GROUP, id: customerId },
            entityId: { entityType: EntityType.DEVICE, id: entityId }
          };
          return this.groupService.assignEntityToGroup(groupRelation);
        }
        return this.deviceService.assignDeviceToCustomer(customerId, entityId);

      case EntityType.ASSET:
        if (this.data.assignTargetType === 'group') {
          const groupRelation: GroupRelation = {
            groupId: { entityType: EntityType.GROUP, id: customerId },
            entityId: { entityType: EntityType.DEVICE, id: entityId }
          };
          return this.groupService.assignEntityToGroup(groupRelation);
        }
        return this.assetService.assignAssetToCustomer(customerId, entityId);

      case EntityType.DASHBOARD:
        const groupRelation: GroupRelation = {
          groupId: { entityType: EntityType.GROUP, id: customerId },
          entityId: { entityType: EntityType.DEVICE, id: entityId }
        };
        return this.groupService.assignEntityToGroup(groupRelation);
      case EntityType.CUSTOMER:
        const groupRelationCustomer: GroupRelation = {
          groupId: { entityType: EntityType.GROUP, id: customerId },
          entityId: { entityType: EntityType.DEVICE, id: entityId }
        };
        return this.groupService.assignEntityToGroup(groupRelationCustomer);
      case EntityType.EDGE:
        return this.edgeService.assignEdgeToCustomer(customerId, entityId);
      case EntityType.ENTITY_VIEW:
        return this.entityViewService.assignEntityViewToCustomer(customerId, entityId);
      case EntityType.GROUP:
        return this.groupService.assignGroupToUser(customerId, entityId);
    }
  }

}
