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

import { Component, DoCheck } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { User } from '@app/shared/models/user.model';
import { EntityType } from '@shared/models/entity-type.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RoleService } from '@app/core/http/role.service';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';

@Component({
  selector: 'tb-user-tabs',
  templateUrl: './user-tabs.component.html',
  styleUrls: []
})
export class UserTabsComponent extends EntityTabsComponent<User>{
  entityType = EntityType;
  selectedRoles: any[] = [];
  affectedRole: UntypedFormGroup;
  lastEntityId: string | null = null;
  isSaving = false; // Ajoutez cette variable dans votre composant

  constructor(protected store: Store<AppState>,
              public fb: UntypedFormBuilder, 
              private roleService: RoleService) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.affectedRole = this.fb.group({
      roleIds: [[]],  
    });
  }

  /**
   * Vérifie si l'entité change et recharge les rôles dynamiquement
   */
  ngDoCheck() {
    if (this.entity && this.entity.id.id !== this.lastEntityId) {
      this.lastEntityId = this.entity.id.id;
      this.loadRoles();
    }
  }

  /**
   * Charge uniquement les rôles **actifs** liés à l'entité actuelle
   */
  loadRoles(): void {
    if (this.entity.id.id) {
      this.isSaving = true; // Désactive le bouton pendant le chargement
  
      this.roleService.getUserRole(this.entity.id.id).subscribe({
        next: (roles) => {
          this.affectedRole.get('roleIds')?.patchValue(null);
          
          if (roles && Array.isArray(roles) && roles.length > 0) {
            const roleIds = roles.map(role => role.id.id);
            this.affectedRole.get('roleIds')?.patchValue(roleIds);
            console.log('Roles affected:', roleIds);
          } else {
            console.warn('Roles response is not an array:', roles);
          }
          
          this.affectedRole.markAsPristine(); // Marque le formulaire comme non modifié
          this.isSaving = false; // Réactive le bouton après le chargement
        },
        error: (error) => {
          console.error('Error loading roles:', error);
          this.isSaving = false; // Réactive le bouton même en cas d'erreur
        }
      });
    }
  }
  
  save(): void {
    if (this.affectedRole.invalid) return;
  
    this.isSaving = true; // Désactiver le bouton
  
    const selectedRoles = this.affectedRole.get('roleIds').value;
    if (selectedRoles && selectedRoles.length > 0) {
      console.log('Saving selected roles:', selectedRoles);
      this.roleService.assignRolesToUser(selectedRoles, this.entity.id).subscribe({
        next: () => {
          console.log('Roles updated successfully');
          this.store.dispatch(new ActionNotificationShow({
            message: "Saving successfully",
            type: 'success',
            duration: 750,

            
          }));
          this.affectedRole.markAsPristine(); // Marquer le formulaire comme non modifié
        },
        error: (err) => {
          console.error('Error updating roles', err);
          this.store.dispatch(new ActionNotificationShow({
            message: "Error saving roles",
            type: 'error',
            duration: 750,

          }));
        },
        complete: () => {
          this.isSaving = false; // Réactiver le bouton après la requête
        }
      });
    } else {
      console.log('No roles selected');
      this.store.dispatch(new ActionNotificationShow({
        message: "No roles selected",
        type: 'error'
      }));
      this.isSaving = false;
    }
  }
}