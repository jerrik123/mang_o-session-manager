package org.mangocube.distributed.session.codec;

/**
 * Created by yeqing on 14-5-12.
 * 序列化与反序列化服务
 */
public interface CodecService<K, V> {


    /**
     * 序列化
     *
     * @param obj 待序列化对象
     * @return 序列化对象
     */
    public V serialize(K obj);

    /**
     * 反序列化
     *
     * @param obj 待反序列化对象
     * @return 原始对象
     */
    public K deSerialize(V obj);

    /**
     * 判断当前的SerializableService是否能够反序列化该对象
     * @param obj 待检验对象
     * @return 可以反序列化：TRUE otherwise：FALSE
     */
    public boolean accept(Object obj);
}
