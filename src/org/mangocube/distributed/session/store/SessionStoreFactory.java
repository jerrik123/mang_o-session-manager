package org.mangocube.distributed.session.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.common.Constants;
import org.mangocube.distributed.session.utils.PropertiesUtil;
import org.mangocube.distributed.session.utils.ReflectUtil;

/**
 * Created by yeqing on 14-5-9.
 * create HttpSessionStore instance
 */
public class SessionStoreFactory {
    private static final Log LOG = LogFactory.getLog(SessionStoreFactory.class);
    private volatile static SessionStoreFactory storeFactory;
    private volatile static HttpSessionStore sessionStore;

    public SessionStoreFactory() {
        String sessionStoreClz = PropertiesUtil.get(Constants.SESSION_STORE_CLZ);
        try {
            sessionStore = (HttpSessionStore) ReflectUtil.getInstanceByClzName(sessionStoreClz);
        } catch (Exception e) {
            LOG.error("获取HttpSessionStore[" + sessionStoreClz + "]实例失败！");
            throw new IllegalArgumentException("获取HttpSessionStore[" + sessionStoreClz + "]实例失败！");
        }
    }

    public static SessionStoreFactory getInstance() {
        if (null == storeFactory) {
            synchronized (SessionStoreFactory.class) {
                if (null == storeFactory) {
                    storeFactory = new SessionStoreFactory();
                }
            }
        }
        return storeFactory;
    }

    public HttpSessionStore getSessionStore(){
        return sessionStore;
    }

    public boolean shutdownSessionStore(){
        LOG.info("release session store connection...");
        return sessionStore.shutdown();
    }
}
