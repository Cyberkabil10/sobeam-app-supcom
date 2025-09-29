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

import { RoleId } from "./id/role-Id";
import { BaseData, EntityType, HasTenantId, TenantId } from "./public-api";
import { TranslateService } from "@ngx-translate/core/dist/lib/translate.service";

export interface Role extends BaseData<RoleId>, HasTenantId {
  tenantId: TenantId;
  name: string;
  description?: string;
  type: EntityType.TENANT | EntityType.CUSTOMER;
  permissions: any;
  assignedUserIds?: any[];
}

export enum Resource {
  ALARM = 'Alarm',
  ASSET = 'Asset',
  ASSETPROFIL = 'Asset Profil'
}

export interface ResourceOperations {
  resource: string;
  operations: string[];
  operationStates?: { [key: string]: boolean };
}

export enum roleType {
  CUSTOMER = 'CUSTOMER',
  TENANT = 'TENANT'
}

export interface RoleTypeTemplate {
  fullFqn: string;
}

export interface RoleTypeData {
  name: string;
  icon: string;
  configHelpLinkId: string;
  template: RoleTypeTemplate;
}

export const RoleTypesData = new Map<roleType, RoleTypeData>(
  [
    [
      roleType.TENANT,
      {
        name: 'role.TENANT',
        icon: 'person_outline',
        configHelpLinkId: 'widgetsConfigTimeseries',
        template: {
          fullFqn: 'system.time_series_chart'
        }
      }
    ],
    [
      roleType.CUSTOMER,
      {
        name: 'role.CUSTOMER',
        icon: 'people',
        configHelpLinkId: 'widgetsConfigLatest',
        template: {
          fullFqn: 'system.cards.attributes_card'
        }
      }
    ],
  ]
);

// Base operations available for all resources
export const Operations: ResourceOperation[] = [
  { key: 'ALL', value: 'select_all' },
  { key: 'CREATE', value: 'add' },
  { key: 'READ', value: 'visibility' },
  { key: 'WRITE', value: 'edit' },
  { key: 'DELETE', value: 'Delete' }
];

// Resource-specific operations
export const ResourceSpecificOperations: { [key: string]: ResourceOperation[] } = {
  'DEVICE': [
    { key: 'ASSIGN_TO_CUSTOMER', value: 'person_add' },        
    { key: 'UNASSIGN_FROM_CUSTOMER', value: 'person_remove' }, 
    { key: 'RPC_CALL', value: 'settings_remote' },             
    { key: 'READ_CREDENTIALS', value: 'visibility' },          
    { key: 'WRITE_CREDENTIALS', value: 'vpn_key' },            
    { key: 'READ_ATTRIBUTES', value: 'search' },              
    { key: 'WRITE_ATTRIBUTES', value: 'edit' },              
    { key: 'READ_TELEMETRY', value: 'sensors' },               
    { key: 'WRITE_TELEMETRY', value: 'timeline' },             
    { key: 'CLAIM_DEVICES', value: 'how_to_reg' },             
    { key: 'READ_CALCULATED_FIELD', value: 'fact_check' }     
  ],

  'ASSET': [
    { key: 'ASSIGN_TO_CUSTOMER', value: 'person_add' },
    { key: 'UNASSIGN_FROM_CUSTOMER', value: 'person_remove' },
    { key: 'READ_ATTRIBUTES', value: 'search' },
    { key: 'WRITE_ATTRIBUTES', value: 'edit' },
    { key: 'READ_TELEMETRY', value: 'sensors' },
    { key: 'WRITE_TELEMETRY', value: 'timeline' },
    { key: 'READ_CALCULATED_FIELD', value: 'fact_check' }
  ],

  'CUSTOMER': [
    { key: 'READ_ATTRIBUTES', value: 'search' },
    { key: 'WRITE_ATTRIBUTES', value: 'edit' },
    { key: 'READ_TELEMETRY', value: 'sensors' },
    { key: 'WRITE_TELEMETRY', value: 'timeline' }
  ],

  'DASHBOARD': [
    { key: 'ASSIGN_TO_CUSTOMER', value: 'person_add' },
    { key: 'UNASSIGN_FROM_CUSTOMER', value: 'person_remove' }
  ],

  'USER': [
    { key: 'WRITE_CALCULATED_FIELD', value: 'functions' }
  ],

  'DEVICE_PROFILE': [
    { key: 'READ_CALCULATED_FIELD', value: 'fact_check' }
  ],

  'ASSET_PROFILE': [
    { key: 'READ_CALCULATED_FIELD', value: 'fact_check' }
  ],
  'RULE_CHAIN': [
     { key: 'READ_ATTRIBUTES', value: 'search' },
    { key: 'WRITE_ATTRIBUTES', value: 'edit' },
    { key: 'READ_TELEMETRY', value: 'sensors' },
    { key: 'WRITE_TELEMETRY', value: 'timeline' }
  ]
};

// Définition d'un type pour chaque ressource
export interface ResourceOperation {
  key: string;
  value: string;
}

export function getTranslatedOperation(operationKey: string, translate: TranslateService): string {
  const translationMap = {
    'ALL': 'permission.operation.all',
    'CREATE': 'permission.operation.CREATE',
    'READ': 'permission.operation.READ',
    'WRITE': 'permission.operation.WRITE',
    'DELETE': 'permission.operation.DELETE',
    'ASSIGN_TO_CUSTOMER': 'permission.operation.ASSIGN_TO_CUSTOMER',
    'READ_ATTRIBUTES': 'permission.operation.READ_ATTRIBUTES',
    'READ_CREDENTIALS': 'permission.operation.READ_CREDENTIALS',
    'WRITE_CREDENTIALS': 'permission.operation.WRITE_CREDENTIALS',
  };
  return translate.instant(translationMap[operationKey] || operationKey);
}

// Function to get all operations for a specific resource
export function getOperationsForResource(resourceKey: string): ResourceOperation[] {
  const baseOperations = [...Operations];
  const specificOperations = ResourceSpecificOperations[resourceKey] || [];
  return [...baseOperations, ...specificOperations];
}

// Function to get all unique operations across all resources
export function getAllOperations(): ResourceOperation[] {
  const allOps = new Map<string, ResourceOperation>();

  // Add base operations
  Operations.forEach(op => allOps.set(op.key, op));

  // Add resource-specific operations
  Object.values(ResourceSpecificOperations).forEach(ops => {
    ops.forEach(op => allOps.set(op.key, op));
  });

  return Array.from(allOps.values());
}

// Enhanced resource operation interface that includes available operations
export interface EnhancedResourceOperation extends ResourceOperation {
  availableOperations: ResourceOperation[];
}

// Ressources du tenant (toutes les permissions) with enhanced operations
export const tenantResourceOperations: EnhancedResourceOperation[] = [
  { key: 'ALARM', value: 'Alarm', availableOperations: getOperationsForResource('ALARM') },
  { key: 'DASHBOARD', value: 'Dashboard', availableOperations: getOperationsForResource('DASHBOARD') },

  { key: 'DEVICE', value: 'Device', availableOperations: getOperationsForResource('DEVICE') },
  { key: 'ASSET', value: 'Asset', availableOperations: getOperationsForResource('ASSET') },
  { key: 'ENTITY_VIEW', value: 'Entity View', availableOperations: getOperationsForResource('ENTITY_VIEW') },
  { key: 'GATEWAY', value: 'Gateway', availableOperations: getOperationsForResource('GATEWAY') },

  { key: 'DEVICE_PROFILE', value: 'Device Profile', availableOperations: getOperationsForResource('DEVICE_PROFILE') },
  { key: 'ASSET_PROFILE', value: 'Asset Profile', availableOperations: getOperationsForResource('ASSET_PROFILE') },

  { key: 'CUSTOMER', value: 'Customer', availableOperations: getOperationsForResource('CUSTOMER') },

  { key: 'USER', value: 'User', availableOperations: getOperationsForResource('USER') },

  //{ key: 'ROLE', value: 'Role', availableOperations: getOperationsForResource('ROLE') },

  { key: 'RULE_CHAIN', value: 'Rule Chain', availableOperations: getOperationsForResource('RULE_CHAIN') },
  { key: 'EDGE', value: 'Edge', availableOperations: getOperationsForResource('EDGE') },

  { key: 'OTA_PACKAGE', value: 'OTA Package', availableOperations: getOperationsForResource('OTA_PACKAGE') },
  { key: 'VERSION_CONTROL', value: 'Version Control', availableOperations: getOperationsForResource('VERSION_CONTROL') },
  { key: 'CALCULATED_FIELD', value: 'Calculated Field', availableOperations: getOperationsForResource('CALCULATED_FIELD') },
  { key: 'SCHEDULEREVENT', value: 'Scheduler Event', availableOperations: getOperationsForResource('SCHEDULEREVENT') },

  { key: 'TB_RESOURCE', value: 'TB Resource', availableOperations: getOperationsForResource('TB_RESOURCE') },

  // { key: 'IMAGE', value: 'Image', availableOperations: getOperationsForResource('IMAGE') },
  //{ key: 'SCADA_SYMBOL', value: 'Scada Symbols', availableOperations: getOperationsForResource('SCADA_SYMBOL') },
  //{ key: 'JS_LIBRARY', value: 'JS Library', availableOperations: getOperationsForResource('JS_LIBRARY') },
  { key: 'WIDGET_TYPE', value: 'Widget Type', availableOperations: getOperationsForResource('WIDGET_TYPE') },

  //  { key: 'WIDGETS_BUNDLE', value: 'Widgets Bundle', availableOperations: getOperationsForResource('WIDGETS_BUNDLE') },

  { key: 'NOTIFICATION', value: 'Notification', availableOperations: getOperationsForResource('NOTIFICATION') },


  /*
   { key: 'MOBILE_APP_SETTINGS', value: 'Mobile App Settings', availableOperations: getOperationsForResource('MOBILE_APP_SETTINGS') },
 
 
   { key: 'API_USAGE_STATE', value: 'API Usage State', availableOperations: getOperationsForResource('API_USAGE_STATE') },
  { key: 'MOBILE_APP', value: 'Mobile App', availableOperations: getOperationsForResource('MOBILE_APP') },
 
   { key: 'MOBILE_APP_BUNDLE', value: 'Mobile App Bundle', availableOperations: getOperationsForResource('MOBILE_APP_BUNDLE') },
 
   { key: 'RPC', value: 'RPC', availableOperations: getOperationsForResource('RPC') },
   { key: 'QUEUE', value: 'Queue', availableOperations: getOperationsForResource('QUEUE') },
   { key: 'OAUTH2_CLIENT', value: 'OAuth2 Client', availableOperations: getOperationsForResource('OAUTH2_CLIENT') },
   { key: 'OAUTH2_CONFIGURATION_TEMPLATE', value: 'OAuth2 Configuration Template', availableOperations: getOperationsForResource('OAUTH2_CONFIGURATION_TEMPLATE') },*/
];

// Ressources du customer (permissions limitées) with enhanced operations
export const customerResourceOperations: EnhancedResourceOperation[] = [
  { key: 'ALARM', value: 'Alarm', availableOperations: getOperationsForResource('ALARM') },
  { key: 'DASHBOARD', value: 'Dashboard', availableOperations: getOperationsForResource('DASHBOARD') },

  { key: 'DEVICE', value: 'Device', availableOperations: getOperationsForResource('DEVICE') },
  { key: 'ASSET', value: 'Asset', availableOperations: getOperationsForResource('ASSET') },
  { key: 'ENTITY_VIEW', value: 'Entity View', availableOperations: getOperationsForResource('ENTITY_VIEW') },

  { key: 'EDGE', value: 'Edge', availableOperations: getOperationsForResource('EDGE') },
  { key: 'NOTIFICATION', value: 'Notification', availableOperations: getOperationsForResource('NOTIFICATION') },

  { key: 'CUSTOMER', value: 'Customer', availableOperations: getOperationsForResource('CUSTOMER') },
  { key: 'USER', value: 'User', availableOperations: getOperationsForResource('USER') },
  { key: 'WIDGET_TYPE', value: 'Widget Type', availableOperations: getOperationsForResource('WIDGET_TYPE') },
  { key: 'RPC', value: 'RPC', availableOperations: getOperationsForResource('RPC') },
  { key: 'DEVICE_PROFILE', value: 'Device Profile', availableOperations: getOperationsForResource('DEVICE_PROFILE') },
  { key: 'ASSET_PROFILE', value: 'Asset Profile', availableOperations: getOperationsForResource('ASSET_PROFILE') },
];

// Fonction pour obtenir les opérations par type de rôle
export function getResourceOperationsByRoleType(entityRole: EntityType): EnhancedResourceOperation[] | null {
  switch (entityRole) {
    case EntityType.TENANT:
      return tenantResourceOperations;
    case EntityType.CUSTOMER:
      return customerResourceOperations;
    default:
      return null;
  }
}