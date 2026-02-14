package com.alpine1428.aihwchat;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class AiCommands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("ai")
            .then(ClientCommandManager.literal("lesson").executes(ctx -> {
                AiHwChatMod.chatAI.setLearning(true);
                AiHwChatMod.chatListener.setListening(true);
                ctx.getSource().sendFeedback(Text.literal("\u00a7a[AI] Learning ON"));
                return 1;
            }))
            .then(ClientCommandManager.literal("stoplesson").executes(ctx -> {
                AiHwChatMod.chatAI.setLearning(false);
                ctx.getSource().sendFeedback(Text.literal("\u00a7c[AI] Learning OFF. Dialogs: "
                    + AiHwChatMod.chatAI.getDialogCount()));
                return 1;
            }))
            .then(ClientCommandManager.literal("start").executes(ctx -> {
                AiHwChatMod.chatAI.setActive(true);
                AiHwChatMod.chatListener.setListening(true);
                ctx.getSource().sendFeedback(Text.literal("\u00a7a[AI] Auto-respond ON"));
                return 1;
            }))
            .then(ClientCommandManager.literal("stop").executes(ctx -> {
                AiHwChatMod.chatAI.setActive(false);
                AiHwChatMod.chatAI.setInCheck(false);
                ctx.getSource().sendFeedback(Text.literal("\u00a7c[AI] Auto-respond OFF"));
                return 1;
            }))
            .then(ClientCommandManager.literal("status").executes(ctx -> {
                ChatAI ai = AiHwChatMod.chatAI;
                ctx.getSource().sendFeedback(Text.literal("\u00a7e[AI] L:"
                    + ai.isLearning() + " A:" + ai.isActive() + " C:" + ai.isInCheck()
                    + " D:" + ai.getDialogCount() + " P:" + ai.getPatternCount()));
                return 1;
            }))
            .then(ClientCommandManager.literal("reset").executes(ctx -> {
                AiHwChatMod.chatAI.reset();
                ctx.getSource().sendFeedback(Text.literal("\u00a7c[AI] Reset done"));
                return 1;
            }))
        );
    }
}
