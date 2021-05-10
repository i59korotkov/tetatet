package dev.korotkov.tetatet;

public class Emoji {

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
