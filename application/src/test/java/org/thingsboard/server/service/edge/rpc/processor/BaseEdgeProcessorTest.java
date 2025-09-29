/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc.processor;


import java.util.UUID;
import java.util.stream.Stream;

public abstract class BaseEdgeProcessorTest {
/*
    @MockBean
    protected TelemetrySubscriptionService tsSubService;

    @MockBean
    protected TbLogEntityActionService logEntityActionService;

    @MockBean
    protected RuleChainService ruleChainService;

    @MockBean
    protected AlarmService alarmService;

    @MockBean
    protected AlarmCommentService alarmCommentService;

    @MockBean
    protected DeviceService deviceService;

    @MockBean
    protected TbDeviceProfileCache deviceProfileCache;

    @MockBean
    protected TbAssetProfileCache assetProfileCache;

    @MockBean
    protected DashboardService dashboardService;

    @MockBean
    protected AssetService assetService;

    @MockBean
    protected EntityViewService entityViewService;

    @MockBean
    protected TenantService tenantService;

    @MockBean
    protected TenantProfileService tenantProfileService;

    @MockBean
    protected EdgeService edgeService;

    @MockBean
    protected CustomerService customerService;

    @MockBean
    protected UserService userService;

    @MockBean
    protected NotificationRuleService notificationRuleService;

    @MockBean
    protected NotificationTargetService notificationTargetService;

    @MockBean
    protected NotificationTemplateService notificationTemplateService;

    @MockBean
    protected DeviceProfileService deviceProfileService;

    @MockBean
    protected AssetProfileService assetProfileService;

    @MockBean
    protected RelationService relationService;

    @MockBean
    protected DeviceCredentialsService deviceCredentialsService;

    @MockBean
    protected AttributesService attributesService;

    @MockBean
    protected TimeseriesService timeseriesService;

    @MockBean
    protected TbClusterService tbClusterService;

    @MockBean
    protected DeviceStateService deviceStateService;

    @MockBean
    protected EdgeEventService edgeEventService;

    @MockBean
    protected WidgetsBundleService widgetsBundleService;

    @MockBean
    protected WidgetTypeService widgetTypeService;

    @MockBean
    protected OtaPackageService otaPackageService;

    @MockBean
    protected QueueService queueService;

    @MockBean
    protected PartitionService partitionService;

    @MockBean
    protected ResourceService resourceService;

    @MockBean
    protected OAuth2Service oAuth2Service;

    @MockBean
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @MockBean
    protected DataValidator<Device> deviceValidator;

    @MockBean
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    @MockBean
    protected DataValidator<Asset> assetValidator;

    @MockBean
    protected DataValidator<AssetProfile> assetProfileValidator;

    @MockBean
    protected DataValidator<Dashboard> dashboardValidator;

    @MockBean
    protected DataValidator<EntityView> entityViewValidator;

    @MockBean
    protected DataValidator<TbResource> resourceValidator;

    @MockBean
    protected EdgeMsgConstructor edgeMsgConstructor;

    @MockBean
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @MockBean
    protected AdminSettingsMsgConstructorV1 adminSettingsMsgConstructorV1;

    @MockBean
    protected AdminSettingsMsgConstructorV2 adminSettingsMsgConstructorV2;

    @MockBean
    protected AlarmMsgConstructorV1 alarmMsgConstructorV1;

    @MockBean
    protected AlarmMsgConstructorV2 alarmMsgConstructorV2;

    @SpyBean
    protected AssetMsgConstructorV1 assetMsgConstructorV1;

    @SpyBean
    protected AssetMsgConstructorV2 assetMsgConstructorV2;

    @MockBean
    protected CustomerMsgConstructorV1 customerMsgConstructorV1;

    @MockBean
    protected CustomerMsgConstructorV2 customerMsgConstructorV2;

    @MockBean
    protected DashboardMsgConstructorV1 dashboardMsgConstructorV1;

    @MockBean
    protected DashboardMsgConstructorV2 dashboardMsgConstructorV2;

    @SpyBean
    protected DeviceMsgConstructorV1 deviceMsgConstructorV1;

    @SpyBean
    protected DeviceMsgConstructorV2 deviceMsgConstructorV2;

    @MockBean
    protected EntityViewMsgConstructorV1 entityViewMsgConstructorV1;

    @MockBean
    protected EntityViewMsgConstructorV2 entityViewMsgConstructorV2;

    @MockBean
    protected OtaPackageMsgConstructorV1 otaPackageMsgConstructorV1;

    @MockBean
    protected OtaPackageMsgConstructorV2 otaPackageMsgConstructorV2;

    @MockBean
    protected QueueMsgConstructorV1 queueMsgConstructorV1;

    @MockBean
    protected QueueMsgConstructorV2 queueMsgConstructorV2;

    @MockBean
    protected RelationMsgConstructorV1 relationMsgConstructorV1;

    @MockBean
    protected RelationMsgConstructorV2 relationMsgConstructorV2;

    @MockBean
    protected ResourceMsgConstructorV1 resourceMsgConstructorV1;

    @MockBean
    protected ResourceMsgConstructorV2 resourceMsgConstructorV2;

    @SpyBean
    protected RuleChainMsgConstructorV1 ruleChainMsgConstructorV1;

    @SpyBean
    protected RuleChainMsgConstructorV2 ruleChainMsgConstructorV2;

    @MockBean
    protected TenantMsgConstructorV1 tenantMsgConstructorV1;

    @MockBean
    protected TenantMsgConstructorV2 tenantMsgConstructorV2;

    @MockBean
    protected UserMsgConstructorV1 userMsgConstructorV1;

    @MockBean
    protected UserMsgConstructorV2 userMsgConstructorV2;

    @MockBean
    protected WidgetMsgConstructorV1 widgetMsgConstructorV1;

    @MockBean
    protected WidgetMsgConstructorV2 widgetMsgConstructorV2;

    @MockBean
    protected NotificationMsgConstructor notificationMsgConstructor;

    @MockBean
    protected OAuth2MsgConstructor oAuth2MsgConstructor;

    @MockBean
    protected AlarmEdgeProcessorV1 alarmProcessorV1;

    @MockBean
    protected AlarmEdgeProcessorV2 alarmProcessorV2;

    @SpyBean
    protected AssetEdgeProcessorV1 assetProcessorV1;

    @SpyBean
    protected AssetEdgeProcessorV2 assetProcessorV2;

    @SpyBean
    protected AssetProfileEdgeProcessorV1 assetProfileProcessorV1;

    @SpyBean
    protected AssetProfileEdgeProcessorV2 assetProfileProcessorV2;

    @MockBean
    protected DashboardEdgeProcessorV1 dashboardProcessorV1;

    @MockBean
    protected DashboardEdgeProcessorV2 dashboardProcessorV2;

    @MockBean
    protected ImageService imageService;

    @SpyBean
    protected DeviceEdgeProcessorV1 deviceEdgeProcessorV1;

    @SpyBean
    protected DeviceEdgeProcessorV2 deviceEdgeProcessorV2;

    @SpyBean
    protected DeviceProfileEdgeProcessorV1 deviceProfileProcessorV1;

    @SpyBean
    protected DeviceProfileEdgeProcessorV2 deviceProfileProcessorV2;

    @MockBean
    protected EntityViewProcessorV1 entityViewProcessorV1;

    @MockBean
    protected EntityViewProcessorV2 entityViewProcessorV2;

    @MockBean
    protected ResourceEdgeProcessorV1 resourceEdgeProcessorV1;

    @MockBean
    protected ResourceEdgeProcessorV2 resourceEdgeProcessorV2;

    @MockBean
    protected RelationEdgeProcessorV1 relationEdgeProcessorV1;

    @MockBean
    protected RelationEdgeProcessorV2 relationEdgeProcessorV2;

    @MockBean
    protected OAuth2EdgeProcessor oAuth2EdgeProcessor;

    @MockBean
    protected NotificationEdgeProcessor notificationEdgeProcessor;

    @SpyBean
    protected RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    @MockBean
    protected AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    @SpyBean
    protected DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    @SpyBean
    protected AssetMsgConstructorFactory assetMsgConstructorFactory;

    @MockBean
    protected DashboardMsgConstructorFactory dashboardMsgConstructorFactory;

    @MockBean
    protected EntityViewMsgConstructorFactory entityViewMsgConstructorFactory;

    @MockBean
    protected RelationMsgConstructorFactory relationMsgConstructorFactory;

    @MockBean
    protected UserMsgConstructorFactory userMsgConstructorFactory;

    @MockBean
    protected CustomerMsgConstructorFactory customerMsgConstructorFactory;

    @MockBean
    protected TenantMsgConstructorFactory tenantMsgConstructorFactory;

    @MockBean
    protected WidgetMsgConstructorFactory widgetBundleMsgConstructorFactory;

    @MockBean
    protected AdminSettingsMsgConstructorFactory adminSettingsMsgConstructorFactory;

    @MockBean
    protected OtaPackageMsgConstructorFactory otaPackageMsgConstructorFactory;

    @MockBean
    protected QueueMsgConstructorFactory queueMsgConstructorFactory;

    @MockBean
    protected ResourceMsgConstructorFactory resourceMsgConstructorFactory;

    @MockBean
    protected AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    @SpyBean
    protected AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    @MockBean
    protected DashboardEdgeProcessorFactory dashboardEdgeProcessorFactory;

    @SpyBean
    protected DeviceEdgeProcessorFactory deviceEdgeProcessorFactory;

    @MockBean
    protected EntityViewProcessorFactory entityViewProcessorFactory;

    @MockBean
    protected RelationEdgeProcessorFactory relationEdgeProcessorFactory;

    @MockBean
    protected ResourceEdgeProcessorFactory resourceEdgeProcessorFactory;

    @MockBean
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    @MockBean
    protected DbCallbackExecutorService dbCallbackExecutorService;

    protected EdgeId edgeId;
    protected TenantId tenantId;
    protected EdgeEvent edgeEvent;

    protected DashboardId getDashboardId(long expectedDashboardIdMSB, long expectedDashboardIdLSB) {
        DashboardId dashboardId;
        if (expectedDashboardIdMSB != 0 && expectedDashboardIdLSB != 0) {
            dashboardId = new DashboardId(new UUID(expectedDashboardIdMSB, expectedDashboardIdLSB));
        } else {
            dashboardId = new DashboardId(UUID.randomUUID());
        }
        return dashboardId;
    }

    protected RuleChainId getRuleChainId(long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        RuleChainId ruleChainId;
        if (expectedRuleChainIdMSB != 0 && expectedRuleChainIdLSB != 0) {
            ruleChainId = new RuleChainId(new UUID(expectedRuleChainIdMSB, expectedRuleChainIdLSB));
        } else {
            ruleChainId = new RuleChainId(UUID.randomUUID());
        }
        return ruleChainId;
    }

    protected static Stream<Arguments> provideParameters() {
        UUID dashboardUUID = UUID.randomUUID();
        UUID ruleChainUUID = UUID.randomUUID();
        return Stream.of(
                Arguments.of(EdgeVersion.V_3_3_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_3_3, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_4_0, 0, 0, 0, 0),
                Arguments.of(EdgeVersion.V_3_6_0,
                        dashboardUUID.getMostSignificantBits(),
                        dashboardUUID.getLeastSignificantBits(),
                        ruleChainUUID.getMostSignificantBits(),
                        ruleChainUUID.getLeastSignificantBits())
        );
    }*/

}
