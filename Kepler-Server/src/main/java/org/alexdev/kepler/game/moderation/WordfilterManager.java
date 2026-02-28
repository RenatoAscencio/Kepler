package org.alexdev.kepler.game.moderation;

import org.alexdev.kepler.dao.mysql.WordfilterDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WordfilterManager {
    private static WordfilterManager instance;
    private static final Logger log = LoggerFactory.getLogger(WordfilterManager.class);

    private Map<String, String> filteredWords;

    public WordfilterManager() {
        this.filteredWords = new ConcurrentHashMap<>();
        this.reload();
    }

    public void reload() {
        this.filteredWords.clear();
        this.filteredWords.putAll(WordfilterDao.getWordfilter());
        log.info("Loaded {} wordfilter entries", this.filteredWords.size());
    }

    public static String filterSentence(String message) {
        if (instance == null || instance.filteredWords.isEmpty()) {
            return message;
        }

        String filtered = message;
        for (Map.Entry<String, String> entry : instance.filteredWords.entrySet()) {
            String word = entry.getKey();
            String replacement = entry.getValue();
            // Case-insensitive replacement
            filtered = filtered.replaceAll("(?i)" + java.util.regex.Pattern.quote(word), replacement);
        }

        return filtered;
    }

    public Map<String, String> getFilteredWords() {
        return this.filteredWords;
    }

    public static WordfilterManager getInstance() {
        if (instance == null) {
            instance = new WordfilterManager();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
        WordfilterManager.getInstance();
    }
}
