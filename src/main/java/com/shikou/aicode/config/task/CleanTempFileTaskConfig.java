package com.shikou.aicode.config.task;

import cn.hutool.core.io.FileUtil;
import com.shikou.aicode.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
public class CleanTempFileTaskConfig {

    private List<String> FilePaths = List.of(
            AppConstant.TEMP_SCREEN_SHOT_DIR
    );

    @Scheduled(cron = "0 0 2 * * ?")
    private void cleanUpTempFile(){
        log.info("开始清理临时目录");
        for(String path : FilePaths){
            boolean result = FileUtil.del(path);
            if(result){
                log.info("清理 {}  文件成功", path);
            }else {
                log.error("清理 {}  文件失败", path);
            }
        }
    }
}
