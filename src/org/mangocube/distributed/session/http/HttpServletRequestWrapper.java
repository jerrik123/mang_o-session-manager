package org.mangocube.distributed.session.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by yeqing on 14-5-9.
 * 获取包装后的session
 */
public class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

    private static final Log LOG = LogFactory.getLog(HttpServletRequestWrapper.class);
    private String sid;

    public HttpServletRequestWrapper(String sid, HttpServletRequest request) {
        super(request);
        this.sid = sid;
    }

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpSession getSession() {
        LOG.info("start call getSession() method. sid:" + sid);
        if (null == sid || "".equals(sid)) {
            String sessionContext = (String) getRequest().getAttribute("sessionContext");
            return new HttpSessionWrapper(super.getSession(), sessionContext);
        } else {
            return new HttpSessionWrapper(sid, super.getSession());
        }
    }

    @Override
    public HttpSession getSession(boolean create) {
        LOG.info("start call getSession(boolean) method. sid:" + sid);
        HttpSession httpSession = super.getSession(create);
        if (null != httpSession) {
            if (null == sid || "".equals(sid)) {
                String sessionContext = (String) getRequest().getAttribute("sessionContext");
                return new HttpSessionWrapper(httpSession, sessionContext);
            } else {
                return new HttpSessionWrapper(sid, httpSession);
            }
        }
        return null;
    }
}
