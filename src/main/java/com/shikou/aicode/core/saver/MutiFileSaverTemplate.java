package com.shikou.aicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.shikou.aicode.ai.model.MultiFileResult;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;

public class MutiFileSaverTemplate extends SaverTemplate<MultiFileResult>{
    @Override
    protected void saveFiles(MultiFileResult result, String baseDir) {
        writeToFile(baseDir, "index.html", result.getHtml());
        writeToFile(baseDir, "script.js", result.getJs());
        writeToFile(baseDir, "style.css", result.getCss());
    }

    @Override
    protected void validateInput(MultiFileResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtml())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }

    @Override
    protected CodeGenTypeEnum getType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }
}
