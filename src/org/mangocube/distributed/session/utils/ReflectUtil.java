package org.mangocube.distributed.session.utils;


/**
 * Created by yeqing on 14-5-9.
 * 反射工具类
 */
public class ReflectUtil {
    /**
     * 根据类名获取实例
     *
     * @param clzName 类名
     * @return 实例
     */
    public static Object getInstanceByClzName(String clzName) throws ClassNotFoundException {
        if (null == clzName || "".equals(clzName)) {
            throw new IllegalArgumentException("通过反射获取实例时参数clzName不能为空！");
        }
        Class clz = Class.forName(clzName);
        try {
            return clz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
