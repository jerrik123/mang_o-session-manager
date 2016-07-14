package org.mangocube.distributed.session.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.codec.CodecFactory;
import org.mangocube.distributed.session.codec.CodecService;
import org.mangocube.distributed.session.store.HttpSessionStore;
import org.mangocube.distributed.session.store.SessionStoreFactory;
import org.mangocube.distributed.session.utils.PropertiesUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

/**
 * Created by yeqing on 14-5-9.
 * http session wrapper
 */
public class HttpSessionWrapper implements HttpSession {
    private static final Log LOG = LogFactory.getLog(HttpSessionWrapper.class);
    private String sid;
    private Map map = null;
    private HttpSession session;
    private volatile static CodecService serializableService = CodecFactory.getInstance().getSerializableService();
    private volatile static HttpSessionStore sessionStore = SessionStoreFactory.getInstance().getSessionStore();
    private static boolean useOriginalSession = Boolean.parseBoolean(PropertiesUtil.get("useOriginalSession", "false"));

    public HttpSessionWrapper(HttpSession session, String sessionContext) {
        if(null != session){
            this.session = session;
            this.sid = this.session.getId();
            if (null != sid && !"".equals(sid)) {
                this.sid = sessionContext + "_" + sid.trim();
            }
            try {
                this.map = sessionStore.get(sid);
                LOG.info("当前用户sid【" + this.sid + "】 session中共有" + map.size() + "个对象");
            } catch (Exception e) {
                LOG.error("初始化session出现异常！当前用户sid【" + this.sid + "】" + "\r\n" + e);
            }
        }else {
            LOG.warn("session 为空！");
        }
    }

    public HttpSessionWrapper(String sid, HttpSession session) {
        if(null != session){
            this.session = session;
            this.sid = sid;
            try {
                this.map = sessionStore.get(sid);
                LOG.info("当前用户sid【" + this.sid + "】 session中共有" + map.size() + "个对象");
            } catch (Exception e) {
                LOG.error("初始化session出现异常！当前用户sid【" + this.sid + "】" + "\r\n" + e);
            }
        }else {
            LOG.warn("session 为空！");
        }
    }


    @SuppressWarnings("unchecked")
    public Object getAttribute(String key) {
        Object value = null;
        if (null == map) {
            LOG.info("从原生态session中获取：" + key + " 当前用户sid【" + this.sid + "】");
            if (useOriginalSession && null != session) {
                value = session.getAttribute(key);
            }
        } else {
            LOG.info("从集中式session管理系统中获取：" + key + " 当前用户sid【" + this.sid + "】");
            value = this.map.get(key);
        }
        LOG.info("key[" + key + "]========>value[" + value + "]");
        if (serializableService.accept(value)) {
            return serializableService.deSerialize(value);
        } else {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    public void setAttribute(String key, Object value) {
        Object savedValue;
        if (!(value instanceof Serializable)) {
            //待存入的对象未被序列化
            LOG.info("使用FastJson序列化value【" + value + "】...");
            savedValue = serializableService.serialize(value);
        } else {
            LOG.info("使用Java原生态序列化【" + value + "】...");
            savedValue = value;
        }
        LOG.info("序列化结果value【" + value + "】========>【" + savedValue + "】");
        if (useOriginalSession && null != session) {
            session.setAttribute(key, savedValue);
        }
        if (null != map) {
            LOG.info("存入 key【" + key + "】 到集中式session管理系统 当前用户sid【" + this.sid + "】");
            map.put(key, value);
            try {
                sessionStore.replace(this.sid, this.map);
            } catch (Exception e) {
                if (e.getCause() instanceof NotSerializableException) {
                    handleNotSerializableException(key, value);
                } else {
                    LOG.error("存入 key【" + key + "】 到集中式session管理系统异常！ 当前用户sid【" + this.sid + "】" + "\r\n" + e);
                }
            }
        }
    }

    public void removeAttribute(String key) {
        if (useOriginalSession && null != session) {
            session.removeAttribute(key);
        }
        if (null != map) {
            LOG.info("从集中式session管理系统中移除key【" + key + "】 当前用户sid【" + this.sid + "】");
            this.map.remove(key);
            try {
                sessionStore.replace(this.sid, this.map);
            } catch (Exception e) {
                LOG.error("从集中式session管理系统中移除key【" + key + "】异常！ 当前用户sid【" + this.sid + "】" + "\r\n" + e);
            }
        }
    }

    public Enumeration getAttributeNames() {
        LOG.info("获取当前用户sid【" + this.sid + "】session中所有key");
        if (null == map) {
            if (useOriginalSession && null != session) {
                return session.getAttributeNames();
            }
        } else {
            return Collections.enumeration(map.keySet());
        }
        return Collections.enumeration(Collections.emptyList());
    }

    public void invalidate() {
        if (useOriginalSession && null != session) {
            session.invalidate();
        }
        if (null != map) {
            LOG.info("使当前用户sid【" + this.sid + "】session失效");
            this.map.clear();
            try {
                sessionStore.delete(this.sid);
            } catch (Exception e) {
                LOG.error("使当前用户sid【" + this.sid + "】session失效发生异常!" + "\r\n" + e);
            }
        }
    }

    public String getId() {
        if (null != sid && !"".equals(sid)) {
            return sid;
        } else {
            if(null != session){
                return session.getId();
            }
            return null;
        }
    }

    /**
     * 处理未被序列化异常
     *
     * @param key   key
     * @param value value
     */
    @SuppressWarnings({"unchecked"})
    private void handleNotSerializableException(String key, Object value) {
        LOG.warn("key[" + key + "]========>value[" + value + "]使用" + serializableService.getClass().getSimpleName() + "重新序列化");
        //待存入的对象未被序列化
        LOG.info("使用FastJson序列化value【" + value + "】...");
        Object serializableValue = serializableService.serialize(value);
        LOG.info("序列化结果value【" + value + "】========>【" + serializableValue + "】");
        this.map.put(key, serializableValue);
        try {
            sessionStore.replace(this.sid, this.map);
            if(useOriginalSession){
                session.setAttribute(this.sid,map);
            }
        } catch (Exception e) {
            LOG.error("存入 key【" + key + "】 到集中式session管理系统序列化异常！ 当前用户sid【" + this.sid + "】" + "\r\n" + e);
        }
    }

    public Object getValue(String s) {
        return session.getValue(s);
    }

    public String[] getValueNames() {
        return session.getValueNames();
    }

    public boolean isNew() {
        return session.isNew();
    }

    public void removeValue(String s) {
        session.removeValue(s);
    }

    public void putValue(String s, Object o) {
        session.putValue(s, o);
    }

    public long getCreationTime() {
        return session.getCreationTime();
    }

    public long getLastAccessedTime() {
        return session.getLastAccessedTime();
    }

    public ServletContext getServletContext() {
        return session.getServletContext();
    }

    public void setMaxInactiveInterval(int i) {
        session.setMaxInactiveInterval(i);
    }

    public int getMaxInactiveInterval() {
        return session.getMaxInactiveInterval();
    }

    public HttpSessionContext getSessionContext() {
        return session.getSessionContext();
    }
}
