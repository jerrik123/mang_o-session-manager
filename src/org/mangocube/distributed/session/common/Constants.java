package org.mangocube.distributed.session.common;

/**
 * Created by yeqing on 14-5-8.
 * 常量类
 */
public interface Constants {
    String COOKIE_SESSION_ID_PREFIX = "SessionID";
    String META_DATA_SEPARATOR = "@ttt";
    String SESSION_STORE_CLZ = "session.store.clz";
    String PROPERTIES_FILE_PATH = "config/session.properties";
    String SESSION_SERIALIZE_SERVICE_CLZ = "session.serialize.clz";
    String SESSION_SERIALIZE_SERVICE_DEFAULT_CLZ = "JsonSerializeService";
    String DEFAULT_SESSION_CONTEXT = "default";
}
