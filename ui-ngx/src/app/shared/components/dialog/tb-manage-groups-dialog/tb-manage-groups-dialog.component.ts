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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppState } from '@app/core/core.state';
import { Store } from '@ngrx/store';
import { EntityId } from '@shared/models/id/entity-id';
import { DialogComponent } from '../../dialog.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { GroupService } from '@app/core/http/group.service';
import { GroupRelation } from '@app/shared/models/group.models';
import { EntityType } from '@app/shared/public-api';

export interface EntityDialogData {
  entityId: EntityId
}

@Component({
  selector: 'tb-tb-manage-groups-dialog',
  templateUrl: './tb-manage-groups-dialog.component.html',
  styleUrl: './tb-manage-groups-dialog.component.scss',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class TbManageGroupsDialogComponent extends DialogComponent<TbManageGroupsDialogComponent> {
  entityFormGroup: UntypedFormGroup;
  updatedEntityIds: Array<string> = [];
  constructor(protected store: Store<AppState>,
    protected router: Router,
    private groupService: GroupService,
    @Inject(MAT_DIALOG_DATA) protected data: EntityDialogData,
    public dialogRef: MatDialogRef<TbManageGroupsDialogComponent>,
    public fb: UntypedFormBuilder
  ) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.entityFormGroup = this.fb.group({
      credential: [null]
    });

    this.loadEntityData(this.data.entityId);
  }


  loadEntityData(entityId: EntityId): void {
    console.log('Loading entity data for entityId:', entityId);   
        this.groupService.getGroupsByEntityId(entityId).subscribe(
          (entities) => {
            const entityIds = entities.map(entity => entity.id.id);
            // Éviter les doublons
            this.updatedEntityIds = [...new Set([...entityIds])];
            // Mettre à jour le champ du formulaire
            this.entityFormGroup.get("credential")?.patchValue(this.updatedEntityIds);
          },
          (error) => {
            console.error('Error loading entity data:', error);
          }
        );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    const newEntityIds = this.entityFormGroup.get("credential")?.value || [];

    const idsToAdd = newEntityIds.filter(id => !this.updatedEntityIds.includes(id));

    const idsToRemove = this.updatedEntityIds.filter(id => !newEntityIds.includes(id));

    if (idsToAdd.length > 0) {
      console.log('IDs à ajouter:', idsToAdd);
      idsToAdd.forEach(groupId => {
        const groupRelation: GroupRelation = {
          groupId: { entityType: EntityType.GROUP, id: groupId },
          entityId: { entityType: EntityType.DEVICE, id: this.data.entityId.id }
        };
        if (this.data.entityId.entityType === EntityType.USER) {
          this.groupService.assignGroupToUser(this.data.entityId.id, groupId).subscribe(
            () => {
              this.dialogRef.close(null);
            },
            (error) => {
              console.error('Erreur lors de l\'ajout de l\'ID:', groupId, error);
            }
          );
        }else {
        this.groupService.assignEntityToGroup(groupRelation).subscribe(
          () => {
            this.dialogRef.close(null);
          },
          (error) => {
            console.error('Erreur lors de l\'ajout de l\'ID:', groupId, error);
          }
        );}
      });
    }

    if (idsToRemove.length > 0) {
      console.log('IDs à supprimer:', idsToRemove);
      idsToRemove.forEach(groupId => {
        const groupRelation: GroupRelation = {
          groupId: { entityType: EntityType.GROUP, id: groupId },
          entityId: { entityType: EntityType.DEVICE, id: this.data.entityId.id }
        };
        if (this.data.entityId.entityType === EntityType.USER) {
              this.groupService.assignGroupToUser('13814000-1dd2-11b2-8080-808080808080', groupId).subscribe(
            () => {
              this.dialogRef.close(null);
            },
            (error) => {
              console.error('Erreur lors de l\'ajout de l\'ID:', groupId, error);
            }
          );
        }else{
        this.groupService.unassignDeviceFromGroup(groupRelation).subscribe(
          () => {
            this.dialogRef.close(null);
          },
          (error) => {
            console.error('Erreur lors de l\'ajout de l\'ID:', groupId, error);
          }
        );}
      });
      // Logique pour supprimer les IDs
    }


  }
}