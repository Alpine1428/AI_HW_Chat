package com.alpine1428.aihwchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatAI {
    private volatile boolean learning = false;
    private volatile boolean active = false;
    private volatile boolean inCheck = false;
    private final List<String> playerBuffer = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentHashMap<String, List<String>> kb = new ConcurrentHashMap<>();
    private final List<DialogPair> dialogs = Collections.synchronizedList(new ArrayList<>());
    private static final Path SAVE_DIR = Paths.get("config", "ai_hw_chat");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("brain.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Random rng = new Random();

    public ChatAI() { loadBrain(); }

    public boolean isLearning() { return learning; }
    public void setLearning(boolean v) { learning = v; if (v) playerBuffer.clear(); }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public boolean isInCheck() { return inCheck; }
    public void setInCheck(boolean v) { inCheck = v; }
    public int getDialogCount() { return dialogs.size(); }
    public int getPatternCount() { return kb.size(); }

    public void recordPlayerMessage(String msg) {
        if (!learning) return;
        playerBuffer.add(msg.toLowerCase().trim());
    }

    public void recordModeratorMessage(String msg) {
        if (!learning || playerBuffer.isEmpty()) return;
        String pm = playerBuffer.get(playerBuffer.size() - 1);
        String mm = msg.trim();
        dialogs.add(new DialogPair(pm, mm));
        for (String w : tokenize(pm)) addKB(w, mm);
        String key = keywords(pm);
        if (!key.isEmpty()) addKB(key, mm);
        saveBrain();
    }

    private void addKB(String k, String v) {
        kb.computeIfAbsent(k, x -> Collections.synchronizedList(new ArrayList<>()));
        List<String> l = kb.get(k);
        if (!l.contains(v)) l.add(v);
    }

    public String generateResponse(String msg) {
        if (kb.isEmpty()) return defResponse(msg);
        String input = msg.toLowerCase().trim();
        String kw = keywords(input);
        if (!kw.isEmpty() && kb.containsKey(kw)) return pick(kb.get(kw));
        List<String> words = tokenize(input);
        String best = null; int bestS = 0;
        for (Map.Entry<String, List<String>> e : kb.entrySet()) {
            int s = 0;
            for (String w : words) {
                if (e.getKey().contains(w)) s += 2;
            }
            if (s > bestS) { bestS = s; best = e.getKey(); }
        }
        if (best != null && bestS > 0) return pick(kb.get(best));
        List<String> cands = new ArrayList<>();
        for (String w : words) if (kb.containsKey(w)) cands.addAll(kb.get(w));
        if (!cands.isEmpty()) return pick(cands);
        for (DialogPair d : dialogs) {
            if (d.pm != null && (d.pm.contains(input) || input.contains(d.pm))) return d.mm;
        }
        return defResponse(msg);
    }

    private String defResponse(String msg) {
        String l = msg.toLowerCase();
        if (has(l, "\u043f\u0440\u0438\u0432\u0435\u0442", "\u0445\u0430\u0439", "hi"))
            return "\u041f\u0440\u0438\u0432\u0435\u0442. \u0422\u044b \u043d\u0430 \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0435.";
        if (has(l, "\u0437\u0430\u0447\u0435\u043c", "\u043f\u043e\u0447\u0435\u043c\u0443"))
            return "\u041f\u043e\u0434\u043e\u0437\u0440\u0435\u043d\u0438\u0435 \u0432 \u0447\u0438\u0442\u0430\u0445.";
        if (has(l, "\u0447\u0438\u0442", "\u0445\u0430\u043a", "hack"))
            return "\u0415\u0441\u043b\u0438 \u0447\u0438\u0441\u0442 - \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u0431\u044b\u0441\u0442\u0440\u043e \u043f\u0440\u043e\u0439\u0434\u0435\u0442.";
        if (has(l, "\u043d\u0435 \u0431\u0443\u0434\u0443", "\u043e\u0442\u043a\u0430\u0437"))
            return "\u041e\u0442\u043a\u0430\u0437 = \u0431\u0430\u043d.";
        if (has(l, "\u043e\u043a", "\u043f\u043e\u043d\u044f\u043b", "\u043b\u0430\u0434\u043d\u043e"))
            return "\u0425\u043e\u0440\u043e\u0448\u043e.";
        if (has(l, "anydesk", "teamviewer"))
            return "\u0421\u043a\u0430\u0447\u0430\u0439 AnyDesk, \u0434\u0430\u0439 ID.";
        if (has(l, "\u0433\u043e\u0442\u043e\u0432", "ready"))
            return "\u041d\u0430\u0447\u0438\u043d\u0430\u0435\u043c.";
        String[] d = {"\u0421\u043b\u0435\u0434\u0443\u0439 \u0438\u043d\u0441\u0442\u0440\u0443\u043a\u0446\u0438\u044f\u043c.",
            "\u041f\u0440\u043e\u0434\u043e\u043b\u0436\u0430\u0435\u043c.",
            "\u041f\u043e\u0434\u043e\u0436\u0434\u0438.",
            "\u0412\u044b\u043f\u043e\u043b\u043d\u044f\u0439 \u0438\u043d\u0441\u0442\u0440\u0443\u043a\u0446\u0438\u0438."};
        return d[rng.nextInt(d.length)];
    }

    private boolean has(String t, String... ws) {
        for (String w : ws) if (t.contains(w)) return true;
        return false;
    }

    private List<String> tokenize(String t) {
        List<String> r = new ArrayList<>();
        for (String w : t.split(" ")) {
            String c = clean(w);
            if (c.length() > 2) r.add(c);
        }
        return r;
    }

    private String keywords(String t) {
        List<String> k = tokenize(t);
        Collections.sort(k);
        return String.join(" ", k);
    }

    private String clean(String w) {
        StringBuilder sb = new StringBuilder();
        for (char c : w.toCharArray())
            if (Character.isLetterOrDigit(c)) sb.append(Character.toLowerCase(c));
        return sb.toString();
    }

    private String pick(List<String> l) {
        return (l == null || l.isEmpty()) ? null : l.get(rng.nextInt(l.size()));
    }

    public void reset() {
        kb.clear(); dialogs.clear(); playerBuffer.clear();
        learning = false; active = false; inCheck = false; saveBrain();
    }

    private void saveBrain() {
        try {
            Files.createDirectories(SAVE_DIR);
            BrainData bd = new BrainData();
            bd.kb = new HashMap<>(kb); bd.dialogs = new ArrayList<>(dialogs);
            Files.write(SAVE_FILE, gson.toJson(bd).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { AiHwChatMod.LOGGER.error("[AI] save fail", e); }
    }

    private void loadBrain() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            String j = new String(Files.readAllBytes(SAVE_FILE), StandardCharsets.UTF_8);
            BrainData bd = gson.fromJson(j, BrainData.class);
            if (bd != null) {
                if (bd.kb != null) kb.putAll(bd.kb);
                if (bd.dialogs != null) dialogs.addAll(bd.dialogs);
            }
        } catch (Exception e) { AiHwChatMod.LOGGER.error("[AI] load fail", e); }
    }

    static class DialogPair {
        String pm; String mm;
        DialogPair() {}
        DialogPair(String p, String m) { pm = p; mm = m; }
    }
    static class BrainData {
        Map<String, List<String>> kb;
        List<DialogPair> dialogs;
    }
}
