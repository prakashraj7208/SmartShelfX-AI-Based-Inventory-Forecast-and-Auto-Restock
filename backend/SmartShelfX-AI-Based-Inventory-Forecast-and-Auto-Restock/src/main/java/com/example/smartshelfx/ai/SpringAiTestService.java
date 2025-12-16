package com.example.smartshelfx.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAiTestService {

    private final ChatClient chatClient;

    public SpringAiTestService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String testModel() {
        return chatClient.prompt()
                .user("Say: DeepSeek Chimera FREE model connected successfully!")
                .call()
                .content();
    }
}
