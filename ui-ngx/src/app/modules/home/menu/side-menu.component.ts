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

import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DashboardService } from '@core/http/dashboard.service';
import { Router } from '@angular/router';
import { RoleService } from '@core/http/role.service';
import { Authority } from '@shared/models/authority.enum';
import { selectAuthState } from '@core/auth/auth.selectors';
import { forkJoin, of, Subject } from 'rxjs';
import { map, switchMap, takeUntil } from 'rxjs/operators';
import { AuthState } from '@core/auth/auth.models';

@Component({
  selector: 'tb-side-menu',
  templateUrl: './side-menu.component.html',
  styleUrls: ['./side-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SideMenuComponent implements OnInit, OnDestroy {
  menuSections$ = this.menuService.menuSections();
  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private store: Store<AppState>,
    private menuService: MenuService,
    private roleService: RoleService,
    private dashboardService: DashboardService
  ) {}

  trackByMenuSection(index: number, section: MenuSection) {
    return section.id;
  }

  ngOnInit() {
    this.menuSections$ = this.store.select(selectAuthState).pipe(
      switchMap(authState => {
        if (!authState || !authState.authUser || authState.authUser.authority !== Authority.TENANT_ADMIN) {
          return this.menuService.menuSections();
        }

        const authUser = authState.authUser;

        return this.roleService.getUserTenantRole(authUser.userId).pipe(
          switchMap(dashboardPermissions => {
            if (!dashboardPermissions || !dashboardPermissions.dashboardPermission) {
              return this.menuService.menuSections();
            }

            return this.menuService.menuSections().pipe(
              switchMap(sections => {
                const dashboardSectionIndex = sections.findIndex(section => section.id === 'dashboards');

                if (dashboardSectionIndex === -1) {
                  return of(sections);
                }

                // 🔹 Extraire TOUS les dashboardIds sans doublons
                const dashboardUserIds = Object.keys(dashboardPermissions.dashboardTenant || {});
                const dashboardIds = Object.keys(dashboardPermissions.dashboardPermission || {});

                const listDashboard: string[] = [];
                [...dashboardUserIds, ...dashboardIds].forEach(id => {
                  if (!listDashboard.includes(id)) {
                    listDashboard.push(id);
                  }
                });

                if (listDashboard.length === 0) {
                  return of(sections);
                } else {
                  return forkJoin(listDashboard.map(id => this.dashboardService.getDashboardInfo(id))).pipe(
                    map(dashboardsInfoArray => {
                      const newSections = [...sections];

                      // ❌ Supprimer l'ancien menu "dashboards"
                      newSections.splice(dashboardSectionIndex, 1);

                      // 🔹 Construire les nouveaux items dashboard
                      const dashboardItems = dashboardsInfoArray.map(info =>
                        this.createDashboardMenuItem(info, authState)
                      );

                      // 🔹 Ajouter les nouveaux items
                      newSections.splice(dashboardSectionIndex, 0, ...dashboardItems);

                      return newSections;
                    })
                  );
                }
              })
            );
          })
        );
      }),
      takeUntil(this.destroy$)
    );
  }

  /**
   * Creates menu items from dashboard info
   */
  private createDashboardMenuItem(dashboardInfo: any, authState: AuthState): any {
    return {
      id: `dashboard-${dashboardInfo.id.id}`,
      name: dashboardInfo.name || `Dashboard ${dashboardInfo.id.id}`,
      type: 'link',
      path: `/dashboards/${dashboardInfo.id.id}`,
      icon: 'dashboards',
    };
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
