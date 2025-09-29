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

import { ChangeDetectorRef, Component, Inject, OnInit, Optional } from '@angular/core';
import { FormArray, FormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { AppState } from '@app/core/core.state';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '../../models/entity/entities-table-config.models';
import { 
  getResourceOperationsByRoleType, 
  getTranslatedOperation, 
  Operations, 
  EnhancedResourceOperation, 
  Role, 
  roleType,
  getAllOperations
} from '@app/shared/models/role.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DialogComponent } from '@app/shared/public-api';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { RoleService } from '@app/core/http/role.service';

@Component({
  selector: 'tb-role',
  templateUrl: './role-tabs.component.html',
  styleUrls: ['./role-tabs.component.scss']
})
export class RoleTabsComponent extends DialogComponent<RoleTabsComponent, null> implements OnInit {

  value: number = 1;
  entityType = EntityType;
  operationKeys = getAllOperations(); // Use all available operations
  entityForm: FormGroup;
  roleTypes: roleType;
  type: String;
  resourceTypes: EnhancedResourceOperation[];

  get translatedRoleType(): {[key: string]: string} {
    return {
      [roleType.CUSTOMER]: this.translate.instant('role.types.CUSTOMER'),
      [roleType.TENANT]: this.translate.instant('role.types.TENANT')
    };
  }

  constructor(
    protected store: Store<AppState>,
    protected translate: TranslateService,
    @Optional() @Inject('entity') protected entityValue: Role,
    @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
    @Inject(MAT_DIALOG_DATA) public data: { type?: EntityType, role?: Role, status?: string },

    protected fb: UntypedFormBuilder,
    protected cd: ChangeDetectorRef,
    protected router: Router,
    protected roleService: RoleService,
    public dialogRef: MatDialogRef<RoleTabsComponent>
  ) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.resourceTypes = getResourceOperationsByRoleType(this.data.type);
    this.buildForm();
    this.type = this.data.type || this.data.role.type
    if (this.data.role) { this.updateForm(this.data.role); }
  }

  buildForm(): void {
    this.entityForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      type: [this.data.type, Validators.required],
      permissions: this.fb.group({
        menuPermission: this.fb.array([])
      }),
      assignedUserIds: [null],
    });
  }

  updateForm(entity: Role): void {
    this.resourceTypes = getResourceOperationsByRoleType(entity.type);

    // Patch the basic values
    this.entityForm.patchValue({
      name: entity.name,
      description: entity.description,
      type: entity.type,
    });

    // Update menuPermission FormArray if needed
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    menuPermissionArray.clear();

    if (entity.permissions && entity.permissions.menuPermission) {
      entity.permissions.menuPermission.forEach(permission => {
        menuPermissionArray.push(this.fb.group({
          resource: [permission.resource],
          operations: [permission.operations]
        }));
      });
    }
    
    if (entity.assignedUserIds) {
      const assignedUserIds = Array.isArray(entity.assignedUserIds)
        ? entity.assignedUserIds.map(user => {
          if (typeof user === 'object' && user?.id?.getId) {
            return user.id.getId().toString();
          }
          return user?.toString() || '';
        }).filter(id => id) : [];

      this.entityForm.get('assignedUserIds')?.patchValue(assignedUserIds);
    }
  }
  
  getResourceLabel(resourceKey: string): string {
    return this.translate.instant(`menu.${resourceKey}`) || resourceKey;
  }

  getOperationLabel(operationKey: string): string {
    return getTranslatedOperation(operationKey, this.translate);
  }

  // Get operations available for a specific resource
  getResourceOperations(resourceKey: string) {
    const resource = this.resourceTypes.find(r => r.key === resourceKey);
    return resource ? resource.availableOperations : Operations;
  }

  // Check if an operation is available for a specific resource
  isOperationAvailableForResource(resourceKey: string, operationKey: string): boolean {
    if (operationKey === 'all') return true; // 'all' is always available
    const resourceOps = this.getResourceOperations(resourceKey);
    return resourceOps.some(op => op.key === operationKey);
  }

  toggleResource(resource: string, checked: boolean): void {
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    const index = menuPermissionArray.controls.findIndex(c => c.get('resource')?.value === resource);

    if (checked) {
      if (index < 0) {
        // Ajout avec aucune opération au départ
        const newPermission = this.fb.group({
          resource: [resource],
          operations: [[]]
        });
        menuPermissionArray.push(newPermission);
      }
    } else {
      if (index >= 0) {
        menuPermissionArray.removeAt(index);
      }
    }
  }

  isResourceChecked(resource: string): boolean {
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    return menuPermissionArray.controls.some(control => control.get('resource')?.value === resource);
  }

  isOperationAssigned(resource: string, operation: string): boolean {
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    const permission = menuPermissionArray.controls.find(c => c.get('resource')?.value === resource);
    return permission?.get('operations')?.value.includes(operation);
  }

  operationAssigned(resource: string, operation: string): void {
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;

    if (resource === 'all') {
      // For 'all', only include resources that support this operation
      const supportingResources = this.resourceTypes.filter(res => 
        this.isOperationAvailableForResource(res.key, operation)
      );

      const allHaveOperation = supportingResources.every(res => {
        const permission = menuPermissionArray.controls.find(c => c.get('resource')?.value === res.key);
        return permission && permission.get('operations')?.value.includes(operation);
      });

      if (allHaveOperation) {
        // Tous ont déjà l'opération → retirer
        supportingResources.forEach(res => {
          const index = menuPermissionArray.controls.findIndex(c => c.get('resource')?.value === res.key);
          if (index >= 0) {
            const control = menuPermissionArray.at(index);
            let ops: string[] = control.get('operations')?.value || [];
            ops = ops.filter(op => op !== operation);
            if (ops.length === 0) {
              menuPermissionArray.removeAt(index);
            } else {
              control.get('operations')?.setValue(ops);
            }
          }
        });
      } else {
        // Au moins un ne l'a pas → ajouter à tous
        supportingResources.forEach(res => {
          const control = menuPermissionArray.controls.find(c => c.get('resource')?.value === res.key);
          if (control) {
            const ops: string[] = control.get('operations')?.value || [];
            if (!ops.includes(operation)) {
              ops.push(operation);
              control.get('operations')?.setValue(ops);
            }
          } else {
            const newControl = this.fb.group({
              resource: [res.key],
              operations: [[operation]]
            });
            menuPermissionArray.push(newControl);
          }
        });
      }
      return;
    }

    // Cas normal (ressource unique)
    const existingPermissionIndex = menuPermissionArray.controls.findIndex(
      control => control.get('resource')?.value === resource
    );

    if (existingPermissionIndex >= 0) {
      const existingPermission = menuPermissionArray.at(existingPermissionIndex);
      const operations = existingPermission.get('operations')?.value || [];

      if (operations.includes(operation)) {
        const updatedOperations = operations.filter((op: string) => op !== operation);
        existingPermission.get('operations')?.setValue(updatedOperations);

        if (updatedOperations.length === 0) {
          menuPermissionArray.removeAt(existingPermissionIndex);
        }
      } else {
        operations.push(operation);
        existingPermission.get('operations')?.setValue(operations);
      }
    } else {
      const newPermission = this.fb.group({
        resource: [resource],
        operations: [[operation]]
      });
      menuPermissionArray.push(newPermission);
    }
  }

  isOperationAssignedToAll(operation: string): boolean {
    const menuPermissionArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    // Only check resources that support this operation
    const supportingResources = this.resourceTypes.filter(res => 
      this.isOperationAvailableForResource(res.key, operation)
    );
    
    return supportingResources.every(res => {
      const control = menuPermissionArray.controls.find(c => c.get('resource')?.value === res.key);
      return control && control.get('operations')?.value.includes(operation);
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  onClick(value: number): void {
    this.value = value;
    this.cd.detectChanges();
  }

  save(): void {
    if (this.entityForm.valid) {
      const role: Role = this.entityForm.value;
      role.id = this.data.role?.id || null; // Use existing ID if available
      this.roleService.saveRole(role).subscribe(savedRole => {
        this.updateForm(savedRole);
        this.entityForm.markAsPristine();
        this.cd.detectChanges();
      });

      this.dialogRef.close(role);
    } else {
      this.entityForm.markAllAsTouched();
    }
  }
}