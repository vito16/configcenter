/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-09-15 11:38 创建
 */
package org.antframework.configcenter.web.controller.manage;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.antframework.common.util.facade.*;
import org.antframework.configcenter.biz.util.AppUtils;
import org.antframework.configcenter.biz.util.ConfigUtils;
import org.antframework.configcenter.biz.util.RefreshUtils;
import org.antframework.configcenter.facade.api.PropertyValueService;
import org.antframework.configcenter.facade.info.AppInfo;
import org.antframework.configcenter.facade.info.ProfileProperty;
import org.antframework.configcenter.facade.order.SetPropertyValuesOrder;
import org.antframework.configcenter.facade.vo.Property;
import org.antframework.configcenter.facade.vo.Scope;
import org.antframework.configcenter.web.common.KeyPrivileges;
import org.antframework.configcenter.web.common.ManagerApps;
import org.antframework.configcenter.web.common.Privilege;
import org.antframework.manager.facade.enums.ManagerType;
import org.antframework.manager.web.Managers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 配置value管理controller
 */
@RestController
@RequestMapping("/manage/propertyValue")
public class PropertyValueManageController {
    // 掩码后的配置value
    private static final String MASKED_VALUE = "******";
    @Autowired
    private PropertyValueService propertyValueService;

    /**
     * 设置多个配置value
     *
     * @param appId     应用id（必须）
     * @param profileId 环境id（必须）
     * @param keys      一个或多个key（必须，json格式）
     * @param values    与keys一一对应的values（必须，json格式）
     */
    @RequestMapping("/setPropertyValues")
    public EmptyResult setPropertyValues(String appId, String profileId, String keys, String values) {
        ManagerApps.adminOrHaveApp(appId);

        List<String> parsedKeys = JSON.parseArray(keys, String.class);
        List<String> parsedValues = JSON.parseArray(values, String.class);
        if (parsedKeys.size() != parsedValues.size()) {
            throw new BizException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), "配置key和value数量不相等");
        }
        if (Managers.currentManager().getType() != ManagerType.ADMIN) {
            // 校验配置key权限
            Map<String, Privilege> keyPrivileges = KeyPrivileges.findPrivileges(appId);
            for (String key : parsedKeys) {
                Privilege privilege = keyPrivileges.getOrDefault(key, Privilege.READ_WRITE);
                if (privilege != Privilege.READ_WRITE) {
                    throw new BizException(Status.FAIL, CommonResultCode.ILLEGAL_STATE.getCode(), String.format("key[%s]为敏感配置，只有超级管理员才能修改", key));
                }
            }
        }

        SetPropertyValuesOrder order = new SetPropertyValuesOrder();
        order.setAppId(appId);
        order.setProfileId(profileId);
        for (int i = 0; i < parsedKeys.size(); i++) {
            SetPropertyValuesOrder.KeyValue keyValue = new SetPropertyValuesOrder.KeyValue();
            keyValue.setKey(parsedKeys.get(i));
            keyValue.setValue(parsedValues.get(i));
            order.addKeyValue(keyValue);
        }

        EmptyResult result = propertyValueService.setPropertyValues(order);
        // 刷新客户端
        RefreshUtils.refreshClients(appId, profileId);
        return result;
    }

    /**
     * 查找应用继承的配置
     *
     * @param appId     应用id
     * @param profileId 环境id
     */
    @RequestMapping("/findInheritedProperties")
    public FindInheritedPropertiesResult findInheritedProperties(String appId, String profileId) {
        ManagerApps.adminOrHaveApp(appId);

        FindInheritedPropertiesResult result = new FindInheritedPropertiesResult();
        result.setStatus(Status.SUCCESS);
        result.setCode(CommonResultCode.SUCCESS.getCode());
        result.setMessage(CommonResultCode.SUCCESS.getMessage());
        // 获取继承的所有应用的配置
        for (AppInfo app : AppUtils.findInheritedApps(appId)) {
            // 获取应用的配置
            Scope minScope = Objects.equals(app.getAppId(), appId) ? Scope.PRIVATE : Scope.PROTECTED;
            List<ProfileProperty> profileProperties = ConfigUtils.findAppSelfProperties(app.getAppId(), profileId, minScope);
            if (Managers.currentManager().getType() != ManagerType.ADMIN) {
                // 对敏感配置进行掩码
                maskProperty(profileProperties, app.getAppId());
            }
            result.addAppProperty(new FindInheritedPropertiesResult.AppProperty(app.getAppId(), profileProperties));
        }
        return result;
    }

    // 对敏感配置进行掩码
    private static void maskProperty(List<ProfileProperty> profileProperties, String appId) {
        Map<String, Privilege> keyPrivileges = KeyPrivileges.findPrivileges(appId);
        for (ProfileProperty profileProperty : profileProperties) {
            List<Property> maskedProperties = new ArrayList<>();
            for (Property property : profileProperty.getProperties()) {
                Privilege privilege = keyPrivileges.getOrDefault(property.getKey(), Privilege.READ_WRITE);
                if (privilege == Privilege.NONE) {
                    property = new Property(property.getKey(), MASKED_VALUE, property.getScope());
                }
                maskedProperties.add(property);
            }
            profileProperty.getProperties().clear();
            profileProperty.getProperties().addAll(maskedProperties);
        }
    }

    /**
     * 查找应用继承的全部配置
     */
    @Getter
    public static class FindInheritedPropertiesResult extends AbstractResult {
        // 由近及远继承的所有应用的配置
        private final List<AppProperty> appProperties = new ArrayList<>();

        public void addAppProperty(AppProperty appProperty) {
            appProperties.add(appProperty);
        }

        /**
         * 应用配置
         */
        @AllArgsConstructor
        @Getter
        public static class AppProperty implements Serializable {
            // 应用id
            private final String appId;
            // 由近及远继承的所用环境中的配置
            private final List<ProfileProperty> profileProperties;
        }
    }
}
