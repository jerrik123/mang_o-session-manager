package org.mangocube.distributed.session.codec.kryo;

import org.mangocube.distributed.session.codec.CodecService;

/**
 * Created by yeqing on 14-5-13.
 * 基于Kryo框架的序列化与反序列化服务
 */
public class KryoSerializeService implements CodecService<Object, byte[]> {

    public byte[] serialize(Object obj) {
        return new byte[0];
    }

    public Object deSerialize(byte[] obj) {
        return null;
    }

    public boolean accept(Object obj) {
        return false;
    }
}
