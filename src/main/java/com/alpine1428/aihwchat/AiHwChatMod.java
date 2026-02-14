package com.alpine1428.aihwchat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiHwChatMod implements ClientModInitializer {
    public static final String MOD_ID = "ai_hw_chat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ChatAI chatAI = new ChatAI();
    public static final ChatListener chatListener = new ChatListener(chatAI);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AI_HW_Chat] Mod loaded!");
        ClientCommandRegistrationCallback.EVENT.register((d, r) -> AiCommands.register(d));
        chatListener.register();
    }
}
