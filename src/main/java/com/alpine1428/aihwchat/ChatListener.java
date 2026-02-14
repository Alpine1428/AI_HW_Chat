package com.alpine1428.aihwchat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import java.util.Random;

public class ChatListener {
    private final ChatAI chatAI;
    private boolean listening = false;
    private static final String MY_NICK = "Alpine1428";
    private final Random random = new Random();

    public ChatListener(ChatAI chatAI) { this.chatAI = chatAI; }
    public void setListening(boolean v) { this.listening = v; }

    public void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (listening && !overlay) processMessage(message.getString());
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register(
            (message, signedMessage, sender, params, receptionTimestamp) -> {
                if (listening) processMessage(message.getString());
                return true;
            });
    }

    private void processMessage(String raw) {
        if (raw == null || raw.isEmpty()) return;
        String cleaned = stripColors(raw).trim();
        if (cleaned.isEmpty()) return;
        if (chatAI.isLearning()) handleLearning(cleaned);
        if (chatAI.isActive()) handleAutoRespond(cleaned);
    }

    private String stripColors(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7' && i + 1 < s.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    private void handleLearning(String msg) {
        ParsedMessage p = parseMessage(msg);
        if (p == null) return;
        if (p.isMyMessage) chatAI.recordModeratorMessage(p.content);
        else if (p.isCheckPlayer) chatAI.recordPlayerMessage(p.content);
    }

    private void handleAutoRespond(String msg) {
        String lower = msg.toLowerCase();
        if (lower.contains("spyfrz")) { chatAI.setInCheck(true); return; }
        if (lower.contains("sban")) { chatAI.setInCheck(false); return; }
        if (chatAI.isInCheck()) {
            ParsedMessage p = parseMessage(msg);
            if (p != null && p.isCheckPlayer) {
                String resp = chatAI.generateResponse(p.content);
                if (resp != null && !resp.isEmpty()) scheduleMessage(resp);
            }
        }
    }

    private void scheduleMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        long delay = 1000 + random.nextInt(2500);
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                client.execute(() -> {
                    if (client.player != null && client.player.networkHandler != null)
                        client.player.networkHandler.sendChatMessage(msg);
                });
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "AI-Sender").start();
    }

    private String cleanNick(String s) {
        return s.replace("<", "").replace(">", "")
                .replace("[", "").replace("]", "").trim();
    }

    private ParsedMessage parseMessage(String msg) {
        ParsedMessage result = new ParsedMessage();
        String working = msg;
        if (working.contains("[CHECK]")) {
            result.isCheckPlayer = true;
            working = working.replace("[CHECK]", "").trim();
        }
        int colonIdx = working.indexOf(':');
        if (colonIdx > 0 && colonIdx < 40) {
            String nick = cleanNick(working.substring(0, colonIdx));
            String content = working.substring(colonIdx + 1).trim();
            if (!nick.isEmpty() && !content.isEmpty()) {
                result.nick = nick;
                result.content = content;
                result.isMyMessage = nick.equalsIgnoreCase(MY_NICK);
                if (result.isMyMessage) result.isCheckPlayer = false;
                return result;
            }
        }
        if (working.startsWith("<")) {
            int close = working.indexOf('>');
            if (close > 1) {
                String nick = working.substring(1, close).trim();
                String content = working.substring(close + 1).trim();
                if (!nick.isEmpty() && !content.isEmpty()) {
                    result.nick = nick;
                    result.content = content;
                    result.isMyMessage = nick.equalsIgnoreCase(MY_NICK);
                    if (result.isMyMessage) result.isCheckPlayer = false;
                    return result;
                }
            }
        }
        int arrow = working.indexOf(">>");
        if (arrow > 0 && arrow < 40) {
            String nick = cleanNick(working.substring(0, arrow));
            String content = working.substring(arrow + 2).trim();
            if (!nick.isEmpty() && !content.isEmpty()) {
                result.nick = nick;
                result.content = content;
                result.isMyMessage = nick.equalsIgnoreCase(MY_NICK);
                if (result.isMyMessage) result.isCheckPlayer = false;
                return result;
            }
        }
        return null;
    }

    static class ParsedMessage {
        String nick = "";
        String content = "";
        boolean isMyMessage = false;
        boolean isCheckPlayer = false;
    }
}
