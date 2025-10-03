package com.shikou.aicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.shikou.aicode.ai.model.HtmlResult;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;

public class HtmlSaverTemplate extends SaverTemplate<HtmlResult>{
    @Override
    protected void saveFiles(HtmlResult result, String baseDir) {
        writeToFile(baseDir, "index.html", result.getHtml());
    }

    @Override
    protected CodeGenTypeEnum getType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtml())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML 代码不能为空");
        }
    }
}
