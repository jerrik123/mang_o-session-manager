package org.mangocube.distributed.session.codec.fastjson;

import com.alibaba.fastjson.JSON;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mangocube.distributed.session.codec.CodecService;
import org.mangocube.distributed.session.common.Constants;
import org.mangocube.distributed.session.codec.fastjson.extension.JsonParserConfig;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by yeqing on 14-5-12.
 * 使用fastjson实现序列化
 */
public class JsonSerializeService implements CodecService<Object, String> {

    private static Log LOG = LogFactory.getLog(JsonSerializeService.class);

    public JsonSerializeService() {
    }

    /**
     * object to json
     *
     * @param obj 待序列化对象
     * @return json
     */
    public String serialize(Object obj) {
        if (null == obj) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        String jsonString = JSON.toJSONString(obj);
        String objClzName = getObjClzName(obj);
        return jsonString + Constants.META_DATA_SEPARATOR + objClzName;
    }

    /**
     * json to object
     *
     * @param obj 待反序列化对象
     * @return 反序列化后对象
     */
    public Object deSerialize(String obj) {
        if (null == obj || "".equals(obj.trim())) {
            return null;
        }
        String[] content = obj.trim().split(Constants.META_DATA_SEPARATOR);
        if (1 == content.length) {
            //for string
            return obj;
        } else if (2 == content.length) {
            //for pojo
            try {
                Class clz = Class.forName(content[1]);
                String jsonString = content[0];
                LOG.debug("json to object:\r\n" + jsonString);
                if (jsonString.startsWith("{")) {
                    return JSON.parseObject(jsonString, clz, JsonParserConfig.getGlobalInstance(), JSON.DEFAULT_PARSER_FEATURE);
                } else {
                    LOG.error("无法将json[" + jsonString + "]转换成" + content[1] + "对象");
                }
            } catch (Exception e) {
                LOG.error("使用fastjson将json字串转为Object时发生异常：\r\n" + "ClassName:" + content[1] + "\r\n" + "json:" + content[0] + "\r\n" + e);
            }
        } else if (2 < content.length) {
            //for map and collection
            try {
                Type type = getType(content, 1);
                return JSON.parseObject(content[0], type, JsonParserConfig.getGlobalInstance(), JSON.DEFAULT_PARSER_FEATURE);
            } catch (Exception e) {
                LOG.error("使用fastjson将json字串转为Object时发生异常：\r\n" + "ClassName:" + content[1] + "\r\n" + "json:" + content[0] + "\r\n" + e);
            }
        }
        return null;
    }

    public boolean accept(Object obj) {
        if (!(obj instanceof String)) {
            return false;
        } else {
            String[] content = ((String) obj).trim().split(Constants.META_DATA_SEPARATOR);
            if (content.length < 2) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取待序列化对象类元数据信息
     *
     * @param obj 待序列化对象
     * @return 类元数据信息
     */
    private String getObjClzName(Object obj) {
        if (obj instanceof Collection || obj instanceof Map) {
            return getTypeString(obj, new StringBuilder());
        }
        return obj.getClass().getName();
    }

    private String getTypeString(Object value, StringBuilder sb) {
        sb.append(value.getClass().getName()).append(Constants.META_DATA_SEPARATOR);
        if (value instanceof Collection) {
            Object newValue = ((Collection) value).iterator().next();
            getTypeString(newValue, sb);
        } else if (value instanceof Map) {
            Object k = ((Map) value).keySet().iterator().next();
            Object v = ((Map) value).get(k);
            getTypeString(k, sb);
            getTypeString(v, sb);
        }
        return sb.toString();
    }

    private Type getType(String[] contents, int index) throws ClassNotFoundException {
        Map<Integer, String> metaDataMap = new HashMap<Integer, String>();
        for (int i = index; i < (contents.length - 1); i++) {
            Class clz = Class.forName(contents[i]);
            if (clz.getName().toLowerCase().contains("map")) {
                metaDataMap.put(i, "map");
            } else if (clz.getName().toLowerCase().contains("list")) {
                metaDataMap.put(i, "list");
            }
        }
        List<Integer> keys = new ArrayList<Integer>();
        keys.addAll(metaDataMap.keySet());
        Collections.sort(keys, new Comparator<Integer>() {
            public int compare(Integer o1, Integer o2) {
                if (o1 > o2) {
                    return 0;
                }
                return 1;
            }
        });
        Type type = null;
        for (int i : keys) {
            if (metaDataMap.get(i).equals("map")) {
                type = ParameterizedTypeImpl.make(Class.forName(contents[i]), new Type[]{Class.forName(contents[i + 1]), null == type ? Class.forName(contents[i + 2]) : type}, null);
            } else if (metaDataMap.get(i).equals("list")) {
                type = ParameterizedTypeImpl.make(Class.forName(contents[i]), new Type[]{null == type ? Class.forName(contents[i + 1]) : type}, null);
            } else {
                LOG.warn("解析对象元数据出现异常！");
            }
        }
        return type;
    }
}
