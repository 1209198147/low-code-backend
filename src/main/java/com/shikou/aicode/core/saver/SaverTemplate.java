package com.shikou.aicode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract class SaverTemplate<T> {
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    public final File save(T result, Long appId){
        // 校验输入
        validateInput(result);
        // 生成目录
        String baseDir = buildUniqueDir(appId);
        // 保存文件
        saveFiles(result, baseDir);
        return new File(baseDir);
    }

    protected abstract void saveFiles(T result, String baseDir);

    private String buildUniqueDir(Long appId) {
        ThrowUtils.throwIf(appId==null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        String type = getType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", type, appId);
        String uniqueDir = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        return uniqueDir;
    }

    protected void validateInput(T result) {
        ThrowUtils.throwIf(result==null, ErrorCode.PARAMS_ERROR, "参数不能为空");
    }

    protected abstract CodeGenTypeEnum getType();

    public final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }
}
