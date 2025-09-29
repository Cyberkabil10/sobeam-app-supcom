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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { WidgetsBundleComponent } from '@modules/home/pages/widget/widgets-bundle.component';
import { WidgetLibraryRoutingModule } from '../widget/widget-library-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { WidgetEditorComponent } from '../widget/widget-editor.component';
import { SelectWidgetTypeDialogComponent } from '../widget/select-widget-type-dialog.component';
import { SaveWidgetTypeAsDialogComponent } from './save-widget-type-as-dialog.component';
import { WidgetsBundleTabsComponent } from '../widget/widgets-bundle-tabs.component';
import { WidgetTypeComponent } from '../widget/widget-type.component';
import { WidgetTypeTabsComponent } from '../widget/widget-type-tabs.component';
import { WidgetsBundleWidgetsComponent } from '../widget/widgets-bundle-widgets.component';
import { WidgetTypeAutocompleteComponent } from '../widget/widget-type-autocomplete.component';
import { WidgetsBundleDialogComponent } from '../widget/widgets-bundle-dialog.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';

@NgModule({
  declarations: [
    WidgetTypeComponent,
    WidgetsBundleComponent,
    WidgetTypeAutocompleteComponent,
    WidgetsBundleWidgetsComponent,
    WidgetEditorComponent,
    SelectWidgetTypeDialogComponent,
    SaveWidgetTypeAsDialogComponent,
    WidgetTypeTabsComponent,
    WidgetsBundleTabsComponent,
    WidgetsBundleDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    WidgetConfigComponentsModule,
    WidgetLibraryRoutingModule
  ]
})
export class WidgetLibraryModule { }
