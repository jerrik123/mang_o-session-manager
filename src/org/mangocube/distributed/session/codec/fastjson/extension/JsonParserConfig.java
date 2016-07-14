package org.mangocube.distributed.session.codec.fastjson.extension;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.*;

import java.lang.reflect.Type;

/**
 * Created by yeqing on 14-5-19.
 * 扩展ParserConfig类 获取自定义JavaBeanDeserializerEx
 */
public class JsonParserConfig extends ParserConfig {

    private static ParserConfig config = new JsonParserConfig();
    public JsonParserConfig() {
        super();
    }

    @Override
    public ObjectDeserializer getDeserializer(Class<?> clazz, Type type) {
        ObjectDeserializer derializer = null;
        try {
            derializer = super.getDeserializer(clazz,type);
        }catch (JSONException e){
            if(e.getMessage().contains("default constructor not found")){
                derializer = getUserDefineObjectDeserializer(clazz,type);
                putDeserializer(type, derializer);
            }
        }
        return derializer;
    }

    private ObjectDeserializer getUserDefineObjectDeserializer(Class<?> clazz, Type type){
        return new JavaBeanDeserializerEx(this,clazz,type);
    }

    public static ParserConfig getGlobalInstance() {
        return config;
    }
}
