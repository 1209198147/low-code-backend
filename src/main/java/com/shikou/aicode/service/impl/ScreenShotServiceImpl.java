package com.shikou.aicode.service.impl;

import cn.hutool.core.io.FileUtil;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.manager.CosManager;
import com.shikou.aicode.service.ScreenShotService;
import com.shikou.aicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service
@Slf4j
public class ScreenShotServiceImpl implements ScreenShotService {
    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String url){
        ThrowUtils.throwIf(StringUtils.isEmpty(url), ErrorCode.PARAMS_ERROR, "url不能为空");
        log.info("开始生成截图...");
        String path = WebScreenshotUtils.saveWebScreenShot(url);
        ThrowUtils.throwIf(StringUtils.isEmpty(path), ErrorCode.SYSTEM_ERROR, "生成截图失败");
        try{
            String cosUrl = uploadCos(path);
            ThrowUtils.throwIf(StringUtils.isEmpty(cosUrl), ErrorCode.PARAMS_ERROR, "上传截图失败");
            log.info("截图上传成功");
            return cosUrl;
        }finally {
            cleanFile(path);
        }
    }

    @Override
    public String generateAndUploadScreenshot(Long appId, String url){
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StringUtils.isEmpty(url), ErrorCode.PARAMS_ERROR, "url不能为空");
        log.info("开始生成截图...");
        String path = WebScreenshotUtils.saveWebScreenShot(url);
        ThrowUtils.throwIf(StringUtils.isEmpty(path), ErrorCode.SYSTEM_ERROR, "生成截图失败");
        try{
            String cosUrl = uploadCos(appId, path);
            ThrowUtils.throwIf(StringUtils.isEmpty(cosUrl), ErrorCode.PARAMS_ERROR, "上传截图失败");
            log.info("截图上传成功");
            return cosUrl;
        }finally {
            cleanFile(path);
        }
    }

    private String uploadCos(String path){
        ThrowUtils.throwIf(StringUtils.isEmpty(path), ErrorCode.PARAMS_ERROR, "路径不能为空");
        File file = new File(path);
        if(!file.exists()){
            log.error("文件不存在 {}", path);
            return null;
        }
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.png";
        return cosManager.uploadFile(fileName, file);
    }

    private String uploadCos(Long appId, String path){
        ThrowUtils.throwIf(StringUtils.isEmpty(path), ErrorCode.PARAMS_ERROR, "路径不能为空");
        File file = new File(path);
        if(!file.exists()){
            log.error("文件不存在 {}", path);
            return null;
        }
        String fileName = "cover_" + appId + ".png";
        return cosManager.uploadFile(fileName, file);
    }

    private void cleanFile(String path){
        File file = new File(path);
        if(file.exists()){
            FileUtil.del(file.getParentFile());
            log.info("文件已经清除 {}", file.getName());
        }
    }
}
