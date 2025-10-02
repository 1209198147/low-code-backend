package com.shikou.aicode.core.parser;

import com.shikou.aicode.ai.model.HtmlResult;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser implements Parser<HtmlResult>{
    private static final Pattern PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    @Override
    public HtmlResult parser(String content) {
        HtmlResult htmlResult = new HtmlResult();
        String html = extractHtml(content);
        if(StringUtils.isEmpty(content)){
            htmlResult.setHtml(content);
        }else{
            htmlResult.setHtml(html);
        }
        return htmlResult;
    }

    private String extractHtml(String content) {
        Matcher matcher = PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
