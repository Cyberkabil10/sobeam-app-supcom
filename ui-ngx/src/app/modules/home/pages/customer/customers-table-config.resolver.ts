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

import { Router } from '@angular/router';

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Customer } from '@app/shared/models/customer.model';
import { CustomerService } from '@app/core/http/customer.service';
import { CustomerComponent } from '@modules/home/pages/customer/customer.component';
import { CustomerTabsComponent } from '@home/pages/customer/customer-tabs.component';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerId, EntityId } from '@app/shared/public-api';
import { MatDialog } from '@angular/material/dialog';
import { AssignToCustomerDialogComponent, AssignToCustomerDialogData } from '../../dialogs/assign-to-customer-dialog.component';
import { TbManageGroupsDialogComponent, EntityDialogData } from '@app/shared/components/dialog/tb-manage-groups-dialog/tb-manage-groups-dialog.component';
import { GroupRelation } from '@app/shared/models/group.models';
import { forkJoin, map, mergeMap, Observable, of } from 'rxjs';
import { GroupService } from '@app/core/http/group.service';

@Injectable()
export class CustomersTableConfigResolver {

  private readonly config: EntityTableConfig<Customer> = new EntityTableConfig<Customer>();
  authUser = getCurrentAuthUser(this.store);

  constructor(private customerService: CustomerService,
    private homeDialogs: HomeDialogsService,
    private translate: TranslateService,
    private datePipe: DatePipe,
    private dialog: MatDialog,
    private entityGroupService: GroupService,
    private router: Router,
    private store: Store<AppState>) {

    this.config.entityType = EntityType.CUSTOMER;
    this.config.entityComponent = CustomerComponent;
    this.config.entityTabsComponent = CustomerTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.CUSTOMER);
    this.config.entityResources = entityTypeResources.get(EntityType.CUSTOMER);
    const authState = getCurrentAuthState(this.store);

    this.config.columns.push(
      new DateEntityTableColumn<Customer>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Customer>('title', 'customer.title', '25%'),
      new EntityTableColumn<Customer>('email', 'contact.email', '25%'),
      new EntityTableColumn<Customer>('country', 'contact.country', '25%'),
      new EntityTableColumn<Customer>('city', 'contact.city', '25%')
    );
    if (this.authUser?.status) {
      this.config.cellActionDescriptors.push({
        name: this.translate.instant('customer.select-group'),
        icon: 'diversity_2',
        isEnabled: () => true,
        onAction: ($event, entity) => this.assignToGroup($event, [entity.id])
      });
    }

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('customer.manage-customer-users'),
        icon: 'account_circle',
        isEnabled: (customer) => !customer.additionalInfo || !customer.additionalInfo.isPublic,
        onAction: ($event, entity) => this.manageCustomerUsers($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-assets'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-assets')
            : this.translate.instant('customer.manage-customer-assets');
        },
        icon: 'domain',
        isEnabled: () => true,
        onAction: ($event, entity) => this.manageCustomerAssets($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-devices'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-devices')
            : this.translate.instant('customer.manage-customer-devices');
        },
        icon: 'devices_other',
        isEnabled: () => true,
        onAction: ($event, entity) => this.manageCustomerDevices($event, entity)
      },
      {
        name: this.translate.instant('customer.manage-customer-dashboards'),
        nameFunction: (customer) => {
          return customer.additionalInfo && customer.additionalInfo.isPublic
            ? this.translate.instant('customer.manage-public-dashboards')
            : this.translate.instant('customer.manage-customer-dashboards');
        },
        icon: 'dashboard',
        isEnabled: () => true,
        onAction: ($event, entity) => this.manageCustomerDashboards($event, entity)
      }
    );

    if (authState.edgesSupportEnabled) {
      this.config.cellActionDescriptors.push(
        {
          name: this.translate.instant('customer.manage-customer-edges'),
          nameFunction: (customer) => {
            return customer.additionalInfo && customer.additionalInfo.isPublic
              ? this.translate.instant('customer.manage-public-edges')
              : this.translate.instant('customer.manage-customer-edges');
          },
          icon: 'router',
          isEnabled: (customer) => true,
          onAction: ($event, entity) => this.manageCustomerEdges($event, entity)
        }
      );
    }

    this.config.deleteEntityTitle = customer => this.translate.instant('customer.delete-customer-title', { customerTitle: customer.title });
    this.config.deleteEntityContent = () => this.translate.instant('customer.delete-customer-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('customer.delete-customers-title', { count });
    this.config.deleteEntitiesContent = () => this.translate.instant('customer.delete-customers-text');

    this.config.entitiesFetchFunction = pageLink => this.customerService.getCustomers(pageLink);
    this.config.loadEntity = id => this.customerService.getCustomer(id.id);
    this.config.saveEntity = (customer) => {
      return this.customerService.saveCustomer(customer).pipe(
        mergeMap(saved => this.affecterCustomerAuxGroupes(customer, saved))
      );
    };
    this.config.deleteEntity = id => this.customerService.deleteCustomer(id.id);
    this.config.onEntityAction = action => this.onCustomerAction(action, this.config);
    this.config.deleteEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.entitySelectionEnabled = (customer) => customer && (!customer.additionalInfo || !customer.additionalInfo.isPublic);
    this.config.detailsReadonly = (customer) => customer && customer.additionalInfo && customer.additionalInfo.isPublic;
  }

  resolve(): EntityTableConfig<Customer> {
    this.config.tableTitle = this.translate.instant('customer.customers');

    return this.config;
  }

  private openCustomer($event: Event, customer: Customer, config: EntityTableConfig<Customer>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([customer.id.id], { relativeTo: config.getActivatedRoute() });
    this.router.navigateByUrl(url);
  }

  assignToGroup($event: Event, customerId: Array<CustomerId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AssignToCustomerDialogComponent, AssignToCustomerDialogData, boolean>(AssignToCustomerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityIds: customerId,
        entityType: EntityType.CUSTOMER,
        assignTargetType: 'group'
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  manageCustomerUsers($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/users`);
  }

  manageCustomerAssets($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/assets`);
  }

  manageCustomerDevices($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/devices`);
  }

  manageCustomerDashboards($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/dashboards`);
  }

  manageCustomerEdges($event: Event, customer: Customer) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`customers/${customer.id.id}/edgeInstances`);
  }
  manageCustomerGroups($event: Event, entityId: EntityId) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<TbManageGroupsDialogComponent, EntityDialogData>
      (TbManageGroupsDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          entityId,
        }
      })
      .afterClosed()
      .subscribe(() => {

        this.config.updateData();

      });
  }

  onCustomerAction(action: EntityAction<Customer>, config: EntityTableConfig<Customer>): boolean {
    switch (action.action) {
      case 'open':
        this.openCustomer(action.event, action.entity, config);
        return true;
      case 'manageUsers':
        this.manageCustomerUsers(action.event, action.entity);
        return true;
      case 'manageAssets':
        this.manageCustomerAssets(action.event, action.entity);
        return true;
      case 'manageDevices':
        this.manageCustomerDevices(action.event, action.entity);
        return true;
      case 'manageDashboards':
        this.manageCustomerDashboards(action.event, action.entity);
        return true;
      case 'manageEdges':
        this.manageCustomerEdges(action.event, action.entity);
        return true;
      case 'manageGroups':
        this.manageCustomerGroups(action.event, action.entity.id);
        return true;
    }
    return false;
  }


  affecterCustomerAuxGroupes(customer: any, saveCustomer: Customer): Observable<Customer> {
    const listOfGroups = customer.groupId;
    if (listOfGroups && listOfGroups.length > 0) {
      const assignObservables = listOfGroups.map((groupId: string) => {
        const groupRelation: GroupRelation = {
          groupId: { entityType: EntityType.GROUP, id: groupId },
          entityId: { entityType: EntityType.CUSTOMER, id: saveCustomer.id.id }
        };
        return this.entityGroupService.assignEntityToGroup(groupRelation);
      });
      return forkJoin(assignObservables).pipe(map(() => saveCustomer));
    }
    return of(saveCustomer);
  }


}
