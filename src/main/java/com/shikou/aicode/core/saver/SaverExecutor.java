package com.shikou.aicode.core.saver;

import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;

import java.util.HashMap;
import java.util.Map;

public class SaverExecutor {
    private static final Map<CodeGenTypeEnum, SaverTemplate> saverMap = new HashMap<>();

    static {
        HtmlSaverTemplate htmlSaverTemplate = new HtmlSaverTemplate();
        MutiFileSaverTemplate mutiFileSaverTemplate = new MutiFileSaverTemplate();

        saverMap.put(htmlSaverTemplate.getType(), htmlSaverTemplate);
        saverMap.put(mutiFileSaverTemplate.getType(), mutiFileSaverTemplate);
    }

    public static Object executeSaver(Object result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        SaverTemplate saverTemplate = saverMap.get(codeGenTypeEnum);
        ThrowUtils.throwIf(saverTemplate == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        return saverTemplate.save(result, appId);
    }
}
