package com.shikou.aicode.core.parser;

import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;

import java.util.HashMap;
import java.util.Map;

public class ParserExecutor {
    private static final Map<CodeGenTypeEnum, Parser> parserMap = new HashMap<>();

    static {
        HtmlParser htmlParser = new HtmlParser();
        MutiFileParser mutiFileParser = new MutiFileParser();

        parserMap.put(CodeGenTypeEnum.HTML, htmlParser);
        parserMap.put(CodeGenTypeEnum.MULTI_FILE, mutiFileParser);
    }

    public static Object executeParse(String content, CodeGenTypeEnum codeGenTypeEnum) {
        Parser parser = parserMap.get(codeGenTypeEnum);
        ThrowUtils.throwIf(parser == null, ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        return parser.parse(content);
    }
}
