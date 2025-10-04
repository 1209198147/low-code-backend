package com.shikou.aicode.ai.tool;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class WriteFileTool extends BaseTool{

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径") String relativeFilePath,
            @P("内容") String content,
            @ToolMemoryId Long appId
    ){
        try{
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                String projectDirName = StrUtil.format("{}_{}", CodeGenTypeEnum.VUE_PROJECT.getValue(), appId);
                String baseDir = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + projectDirName;
                Path root = Paths.get(baseDir);
                path = root.resolve(relativeFilePath);
            }

            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        }catch (Exception e){
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String getToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return StrUtil.format("""
                [工具调用] {} {}
                ```{}
                {}
                ```
                """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
