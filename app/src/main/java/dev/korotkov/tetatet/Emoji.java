package dev.korotkov.tetatet;

import java.io.Serializable;

public class Emoji implements Serializable {

    private String id;
    private String emoji;

    public Emoji(String id, String emoji) {
        this.id = id;
        this.emoji = emoji;
    }

    public String getId() {
        return id;
    }

    public String getEmoji() {
        return emoji;
    }
}
