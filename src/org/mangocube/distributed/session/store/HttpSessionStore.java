package org.mangocube.distributed.session.store;

import java.io.NotSerializableException;
import java.util.Map;

/**
 * Created by yeqing on 14-5-9.
 * Session Store 接口
 * 通过实现该接口可以将Session存储于不同的地方
 */
public interface HttpSessionStore {

    public Map get(String sessionId) throws Exception;

    public void add(String sessionId, Map session) throws Exception;

    public Map replace(String sessionId, Map newSession) throws Exception;

    public Map delete(String sessionId) throws Exception;

    public boolean shutdown();
}
