package org.example.p10.service;

import org.example.p10.model.dto.ChatRequestDTO;
import org.example.p10.model.vo.ChatResponseVO;

public interface ChatService {
    ChatResponseVO chat(ChatRequestDTO requestDTO);
}