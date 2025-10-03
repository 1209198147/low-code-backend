package com.shikou.aicode.manager;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.shikou.aicode.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CosManager {
    @Resource
    private COSClient cosClient;
    @Resource
    private CosClientConfig clientConfig;

    public PutObjectResult putObject(String key, File file){
        PutObjectRequest putObjectRequest = new PutObjectRequest(clientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    public String uploadFile(String key, File file){
        PutObjectResult result = putObject(key, file);
        if(result!=null){
            String url = StrUtil.format("{}/{}", clientConfig.getHost(), key);
            log.info("文件上传Cos成功 {} url: {}", file.getName(), url);
            return url;
        }
        log.info("文件上传Cos失败 {}", file.getName());
        return null;
    }
}
