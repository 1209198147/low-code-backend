package com.shikou.aicode.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CachekeyUtils {

    public static String getCacheKey(Object o){
        if(o == null){
            return DigestUtil.md5Hex("null");
        }
        String jsonStr = JSONUtil.toJsonStr(o);
        return DigestUtil.md5Hex(jsonStr);
    }
}
