package com.shikou.aicode.manager;

import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.shikou.aicode.config.CosClientConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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

    public String uploadFile(String key, MultipartFile file) {
        try {
            // 创建上传Object的Metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(clientConfig.getBucket(), key,
                    file.getInputStream(), metadata);
            PutObjectResult result = cosClient.putObject(putObjectRequest);
            if(result!=null){
                String url = StrUtil.format("{}/{}", clientConfig.getHost(), key);
                log.info("文件上传Cos成功 {} url: {}", file.getName(), url);
                return url;
            }
            log.info("文件上传Cos失败 {}", file.getName());
            return null;

        } catch (Exception e) {
            log.error("上传文件到COS失败 {}", e.getMessage(), e);
            return null;
        }
    }
}
