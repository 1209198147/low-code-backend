package com.shikou.aicode.core.parser;

import com.shikou.aicode.ai.model.MultiFileResult;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MutiFileParser implements Parser<MultiFileResult>{
    private static final Pattern HTML_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);


    @Override
    public MultiFileResult parser(String content) {
        MultiFileResult multiFileResult = new MultiFileResult();
        String html = extractHtml(content);
        String css = extractCss(content);
        String js = extractJs(content);
        if(StringUtils.isNotEmpty(html)){
            multiFileResult.setHtml(html);
        }
        if(StringUtils.isNotEmpty(css)){
            multiFileResult.setCss(css);
        }
        if(StringUtils.isNotEmpty(js)){
            multiFileResult.setJs(js);
        }
        return multiFileResult;
    }

    private String extractHtml(String content) {
        Matcher matcher = HTML_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCss(String content) {
        Matcher matcher = CSS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractJs(String content) {
        Matcher matcher = JS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
