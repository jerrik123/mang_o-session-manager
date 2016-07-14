package org.mangocube.distributed.session.store.memcached;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.command.KestrelCommandFactory;
import net.rubyeye.xmemcached.command.TextCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.store.HttpSessionStore;
import org.mangocube.distributed.session.utils.PropertiesUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yeqing on 14-5-9.
 * 使用XMemcachedClient将HttpSession存储于Memcached中
 */
public class XMemcachedSessionStore implements HttpSessionStore {

    private static final Log LOG = LogFactory.getLog(XMemcachedSessionStore.class);
    private static MemcachedClient memcachedClient;
    private static int ttl;
    private static String clientType;

    static {
        MemcachedClientBuilder builder;
        //step1 set servers
        String servers = PropertiesUtil.get("session.store.xmemcached.servers", "127.0.0.1:11211");
        if (Boolean.valueOf(PropertiesUtil.get("session.store.xmemcached.failover"))) {
            //support failover mode.servers format: 127.0.0.1:11211,127.0.0.1:11212 127.0.0.1:11213,127.0.0.1:11214
            builder = new XMemcachedClientBuilder(AddrUtil.getAddressMap(servers));
            builder.setFailureMode(true);
        } else {
            //not support failover mode.
            builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(servers));
        }
        //step2 set auth
        if (Boolean.valueOf(PropertiesUtil.get("session.store.xmemcached.sasl"))) {
            //support sasl
            String user = PropertiesUtil.get("session.store.xmemcached.user", "user");
            String pwd = PropertiesUtil.get("session.store.xmemcached.pwd", "pwd");
            String type = PropertiesUtil.get("session.store.xmemcached.sasl.type", "typical");
            String[] hosts = servers.trim().split(" ");
            for (String host : hosts) {
                String[] oneServer = host.trim().split(",");
                for (String s : oneServer) {
                    if ("typical".equals(type)) {
                        builder.addAuthInfo(AddrUtil.getOneAddress(s), AuthInfo.typical(user, pwd));
                    } else if ("cramMD5".equals(type)) {
                        builder.addAuthInfo(AddrUtil.getOneAddress(s), AuthInfo.cramMD5(user, pwd));
                    } else if ("plain".equals(type)) {
                        builder.addAuthInfo(AddrUtil.getOneAddress(s), AuthInfo.plain(user, pwd));
                    } else {
                        LOG.warn("xmemcached sasl暂不支持" + type);
                    }
                }
            }
        }
        //step3 set client type
        clientType = PropertiesUtil.get("session.store.xmemcached.clientType");
        if (null != clientType && !"".equals(clientType)) {
            if (clientType.trim().toLowerCase().equals("binary")) {
                builder.setCommandFactory(new BinaryCommandFactory());
            } else if (clientType.trim().toLowerCase().equals("text")) {
                builder.setCommandFactory(new TextCommandFactory());
            } else if (clientType.trim().toLowerCase().equals("kestrel")) {
                builder.setCommandFactory(new KestrelCommandFactory());
            } else {
                LOG.warn("无法识别xmemcached xmemcached:" + clientType);
            }
        }
        //step4 set operation time out
        long operationTimeout = Long.parseLong(PropertiesUtil.get("session.store.xmemcached.operationTimeOut", "30"));
        if (operationTimeout > 0) {
            builder.setOpTimeout(operationTimeout);
        }
        //step5 set connection pool size
        String poolSizeStr = PropertiesUtil.get("session.store.xmemcached.connection.poolSize");
        if (null != poolSizeStr && !"".equals(poolSizeStr.trim())) {
            builder.setConnectionPoolSize(Integer.parseInt(poolSizeStr));
        }
        //step6 set ttl
        ttl = Integer.parseInt(PropertiesUtil.get("session.store.xmemcached.keepAliveTime", "3600"));
        try {
            memcachedClient = builder.build();
            LOG.info("成功创建与memcached的连接!");
        } catch (IOException e) {
            LOG.error("创建与Memcached的连接失败!\n\r" + e);
        }
    }

    public XMemcachedSessionStore() {
    }

    public Map get(String sessionId) throws Exception {
        LOG.debug("call get method,sessionId:" + sessionId);
        assertSessionIdNotNull(sessionId);
        if (clientType.trim().toLowerCase().equals("binary")) {
            Map session = memcachedClient.getAndTouch(sessionId, ttl);
            if (null == session) {
                // because ConcurrentHashMap dose not support null value.so we use synchronizedMap instead.
                session = Collections.synchronizedMap(new HashMap());
                memcachedClient.add(sessionId, ttl, session);
            }
            return session;
        } else {
            Map session = memcachedClient.get(sessionId);
            if (null == session) {
                session = new ConcurrentHashMap();
                memcachedClient.add(sessionId, ttl, session);
            } else {
                memcachedClient.touch(sessionId, ttl);
            }
            return session;
        }
    }

    public void add(String sessionId, Map session) throws Exception {
        assertSessionIdNotNull(sessionId);
        assertSessionNotNull(session);
        memcachedClient.add(sessionId, ttl, session);
    }

    public Map replace(String sessionId, Map newSession) throws Exception {
        LOG.debug("call replace method,sessionId:" + sessionId);
        assertSessionIdNotNull(sessionId);
        assertSessionNotNull(newSession);
        memcachedClient.replace(sessionId, ttl, newSession);
        return null;
    }

    public Map delete(String sessionId) throws Exception {
        LOG.debug("call delete method,sessionId:" + sessionId);
        assertSessionIdNotNull(sessionId);
        memcachedClient.delete(sessionId);
        return null;
    }

    public boolean shutdown() {
        if (!memcachedClient.isShutdown()) {
            try {
                memcachedClient.shutdown();
                return true;
            } catch (IOException e) {
                LOG.warn("关闭与memcached的连接时出现异常!\n\r" + e);
                return false;
            }
        }
        return true;
    }

    private void assertSessionIdNotNull(String sessionId) {
        if (null == sessionId || "".equals(sessionId.trim())) {
            LOG.error("sid不能为空！");
            throw new IllegalArgumentException("sessionId 不能为空！");
        }
    }

    private void assertSessionNotNull(Map session) {
        if (null == session) {
            LOG.error("SessionMap不能为空！");
            throw new IllegalArgumentException("session 不S能为空！");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!memcachedClient.isShutdown()) {
            memcachedClient.shutdown();
        }
    }
}
