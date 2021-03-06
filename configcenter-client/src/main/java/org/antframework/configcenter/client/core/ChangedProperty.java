/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-09-11 13:54 创建
 */
package org.antframework.configcenter.client.core;

import org.antframework.common.util.tostring.ToString;
import org.antframework.common.util.tostring.format.Mask;

import java.io.Serializable;

/**
 * 被修改的配置项
 */
public class ChangedProperty implements Serializable {
    // 修改类型
    private ChangeType type;
    // key
    private String key;
    // 旧值
    @Mask(allMask = true)
    private String oldValue;
    // 新值
    @Mask(allMask = true)
    private String newValue;

    public ChangedProperty(ChangeType type, String key, String oldValue, String newValue) {
        this.type = type;
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ChangeType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return ToString.toString(this);
    }

    /**
     * 修改类型
     */
    public enum ChangeType {
        // 新增
        ADD,
        // 更新
        UPDATE,
        // 删除
        REMOVE
    }
}
