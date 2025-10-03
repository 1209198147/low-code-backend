package com.shikou.aicode.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.shikou.aicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.shikou.aicode.model.entity.ChatHistory;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.MessageTypeEnum;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author Shikou
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    boolean addMessage(Long userId, Long appId, MessageTypeEnum messageTypeEnum, String message);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory memory, int maxCount);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    boolean deleteByAppId(Long appId);
}
