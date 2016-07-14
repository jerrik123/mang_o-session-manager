package org.mangocube.distributed.session.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.common.Constants;
import org.mangocube.distributed.session.utils.PropertiesUtil;
import org.mangocube.distributed.session.utils.ReflectUtil;

/**
 * Created by yeqing on 14-5-12.
 * 序列化与反序列化服务工厂类
 */
public class CodecFactory {
    private static Log LOG = LogFactory.getLog(CodecFactory.class);
    private volatile static CodecService serializableService;
    private volatile static CodecFactory factory;

    /**
     * constructor for creating a new CodecService instance
     */
    public CodecFactory() {
        String serializeServiceClzName = PropertiesUtil.get(Constants.SESSION_SERIALIZE_SERVICE_CLZ, Constants.SESSION_SERIALIZE_SERVICE_DEFAULT_CLZ);
        try {
            serializableService = (CodecService) ReflectUtil.getInstanceByClzName(serializeServiceClzName);
        } catch (Exception e) {
            LOG.error("获取Object2SerializableService[" + serializeServiceClzName + "]实例失败！");
            throw new IllegalArgumentException("获取Object2SerializableService[" + serializeServiceClzName + "]实例失败！");
        }
    }

    /**
     * singleton patter
     * @return CodecFactory
     */
    public static CodecFactory getInstance() {
        if (null == factory) {
            synchronized (CodecFactory.class) {
                if (null == factory) {
                    factory = new CodecFactory();
                }
            }
        }
        return factory;
    }

    public CodecService getSerializableService() {
        return serializableService;
    }
}
