package org.mangocube.distributed.session.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.common.Constants;
import org.mangocube.distributed.session.http.HttpServletRequestWrapper;
import org.mangocube.distributed.session.store.SessionStoreFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by yeqing on 14-5-8.
 * 拦截请求，转换session，使之透明化
 */
public class DistributedSessionFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(DistributedSessionFilter.class);
    private String sessionId;
    private String sessionContext;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.sessionId = filterConfig.getInitParameter("sessionId");
        if (null == this.sessionId) {
            this.sessionId = Constants.COOKIE_SESSION_ID_PREFIX;
        }
        this.sessionContext = filterConfig.getInitParameter("sessionContext");
        if(null == sessionContext){
            this.sessionContext = Constants.DEFAULT_SESSION_CONTEXT;
        }
        LOG.info("mango-session-manager已启动！");
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        //step1 从cookie中获取session id
        Cookie cookies[] = request.getCookies();
        String sid = "";
        if (null != cookies && cookies.length > 0) {
            for (Cookie sCookie : cookies) {
                if (sCookie.getName().equals(sessionId)) {
                    sid = sCookie.getValue();
					break;
                }
            }
        }
		LOG.info("sid:"+sid);
        if (sid == null || "".equals(sid)) {
            //step2 如果session id为空，则调用默认方法创建session（新的会话）
            request.setAttribute("sessionContext",sessionContext);
            filterChain.doFilter(new HttpServletRequestWrapper(request), servletResponse);
        } else {
            //step3 如果session id不为空，则使用客户端传过来的sessionId
            if(!sid.toLowerCase().contains(sessionContext.toLowerCase())){
                sid = sessionContext+"_"+sid;
            }
            filterChain.doFilter(new HttpServletRequestWrapper(sid, request), servletResponse);
        }
    }

    public void destroy() {
        SessionStoreFactory.getInstance().shutdownSessionStore();
        LOG.info("mango-session-manager已停止！");
    }
}
