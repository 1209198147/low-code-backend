package com.shikou.aicode.core;

import com.shikou.aicode.ai.AiGeneratorService;
import com.shikou.aicode.ai.AiGeneratorServiceFactory;
import com.shikou.aicode.core.parser.ParserExecutor;
import com.shikou.aicode.core.saver.SaverExecutor;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiGeneratorFacade {

    @Resource
    private AiGeneratorServiceFactory aiGeneratorServiceFactory;

    public Flux<String> generateCodeAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成的代码类型不能为空");
        AiGeneratorService aiGeneratorService = aiGeneratorServiceFactory.getAiGeneratorService(appId);
        return switch (codeGenTypeEnum){
            case HTML -> {
                Flux<String> stream = aiGeneratorService.generateHtmlStream(userMessage);
                yield proocessStream(stream, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                Flux<String> stream = aiGeneratorService.generateMutiFileStream(userMessage);
                yield proocessStream(stream, codeGenTypeEnum, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    private Flux<String> proocessStream(Flux<String> stream, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        StringBuilder codeBuilder = new StringBuilder();
        return stream.doOnNext(chunk->{
            codeBuilder.append(chunk);
        }).doOnComplete(()->{
            // 整理要用try，不然ai返回的内容可能没有代码，会直接报错
            try{
                String completeCode = codeBuilder.toString();
                Object result = ParserExecutor.executeParser(completeCode, codeGenTypeEnum);
                SaverExecutor.executeSaver(result, codeGenTypeEnum, appId);
            } catch (Exception e){
                log.error("保存代码失败 {}", e.getMessage(), e);
            }
        });
    }
}
