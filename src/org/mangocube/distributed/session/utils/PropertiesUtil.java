package org.mangocube.distributed.session.utils;

import com.ctol.mango.pge.common.ParamServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.common.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by yeqing on 14-5-9.
 * 获取配置文件中key value值
 */
public final class PropertiesUtil {

    private static final Log LOG = LogFactory.getLog(PropertiesUtil.class);
    private static Properties properties = new Properties();

    static {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(Constants.PROPERTIES_FILE_PATH);
        try {
            properties.load(is);
        } catch (IOException e) {
            LOG.error("加载配置文件(" + Constants.PROPERTIES_FILE_PATH + ")失败！" + e);
        }
    }

    public static String get(String key) {
        if (null != properties) {
            String value = (String) properties.get(key);
            value = ParamServiceImpl.getInstance().getConfValue(value);
            LOG.info("从配置文件获取键值对 [" + key + "]======>[" + value + "]");
            return value;
        } else {
            LOG.info("加载配置文件内容失败！");
        }
        return null;
    }

    public static String get(String key, String defaultValue) {
        if (null != properties) {
            String value = (String) properties.get(key);
            value = ParamServiceImpl.getInstance().getConfValue(value);
            LOG.info("从配置文件获取键值对 [" + key + "]======>[" + value + "]");
            return value;
        } else {
            LOG.info("加载配置文件内容失败！返回默认值:" + defaultValue);
        }
        return defaultValue;
    }
}
