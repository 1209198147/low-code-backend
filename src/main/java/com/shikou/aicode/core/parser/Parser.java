package com.shikou.aicode.core.parser;

public interface Parser<T> {
    /**
     * 解析内容
     * @param content
     * @return 对应的类型
     */
    T parse(String content);
}
