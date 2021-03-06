/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-08-20 21:05 创建
 */
package org.antframework.configcenter.biz.service;

import org.antframework.common.util.facade.BizException;
import org.antframework.common.util.facade.CommonResultCode;
import org.antframework.common.util.facade.EmptyResult;
import org.antframework.common.util.facade.Status;
import org.antframework.configcenter.dal.dao.ProfileDao;
import org.antframework.configcenter.dal.dao.PropertyKeyDao;
import org.antframework.configcenter.dal.dao.PropertyValueDao;
import org.antframework.configcenter.dal.entity.Profile;
import org.antframework.configcenter.dal.entity.PropertyKey;
import org.antframework.configcenter.dal.entity.PropertyValue;
import org.antframework.configcenter.facade.order.SetPropertyValuesOrder;
import org.bekit.service.annotation.service.Service;
import org.bekit.service.annotation.service.ServiceExecute;
import org.bekit.service.engine.ServiceContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 设置多个配置value服务
 */
@Service(enableTx = true)
public class SetPropertyValuesService {
    @Autowired
    private ProfileDao profileDao;
    @Autowired
    private PropertyKeyDao propertyKeyDao;
    @Autowired
    private PropertyValueDao propertyValueDao;

    @ServiceExecute
    public void execute(ServiceContext<SetPropertyValuesOrder, EmptyResult> context) {
        SetPropertyValuesOrder order = context.getOrder();
        // 校验环境是否存在（如果有必要）
        for (SetPropertyValuesOrder.KeyValue keyValue : order.getKeyValues()) {
            if (keyValue.getValue() != null) {
                Profile profile = profileDao.findLockByProfileId(order.getProfileId());
                if (profile == null) {
                    throw new BizException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), String.format("不存在环境[%s]", order.getProfileId()));
                }
                break;
            }
        }
        // 设置配置value
        for (SetPropertyValuesOrder.KeyValue keyValue : order.getKeyValues()) {
            if (keyValue.getValue() != null) {
                setSingleValue(order, keyValue);
            } else {
                deleteSingleValue(order, keyValue);
            }
        }
    }

    // 设置单个配置value
    private void setSingleValue(SetPropertyValuesOrder order, SetPropertyValuesOrder.KeyValue keyValue) {
        PropertyKey propertyKey = propertyKeyDao.findLockByAppIdAndKey(order.getAppId(), keyValue.getKey());
        if (propertyKey == null) {
            throw new BizException(Status.FAIL, CommonResultCode.INVALID_PARAMETER.getCode(), String.format("应用[%s]不存在配置key[%s]", order.getAppId(), keyValue.getKey()));
        }

        PropertyValue propertyValue = propertyValueDao.findLockByAppIdAndKeyAndProfileId(order.getAppId(), keyValue.getKey(), order.getProfileId());
        if (propertyValue == null) {
            propertyValue = buildPropertyValue(order, keyValue);
        } else {
            propertyValue.setValue(keyValue.getValue());
        }
        propertyValueDao.save(propertyValue);
    }

    //构建配置value
    private PropertyValue buildPropertyValue(SetPropertyValuesOrder setPropertyValueOrder, SetPropertyValuesOrder.KeyValue keyValue) {
        PropertyValue propertyValue = new PropertyValue();
        BeanUtils.copyProperties(setPropertyValueOrder, propertyValue);
        BeanUtils.copyProperties(keyValue, propertyValue);
        return propertyValue;
    }

    // 删除单个配置value
    private void deleteSingleValue(SetPropertyValuesOrder order, SetPropertyValuesOrder.KeyValue keyValue) {
        PropertyValue propertyValue = propertyValueDao.findLockByAppIdAndKeyAndProfileId(order.getAppId(), keyValue.getKey(), order.getProfileId());
        if (propertyValue != null) {
            propertyValueDao.delete(propertyValue);
        }
    }
}
