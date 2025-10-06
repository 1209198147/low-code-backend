package com.shikou.aicode.constant;

import java.io.File;
import java.time.Duration;

public interface AppConstant {

    Integer GOOD_APP_PRIORITY = 99;
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";
    String TEMP_SCREEN_SHOT_DIR = System.getProperty("user.dir") + File.separator + "temp" + File.separator + "screenshots";
}
