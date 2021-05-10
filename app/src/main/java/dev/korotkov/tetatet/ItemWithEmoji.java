package dev.korotkov.tetatet;

public class ItemWithEmoji extends Emoji {

    private String name;

    public ItemWithEmoji(String id, String name, String emoji) {
        super(id, emoji);
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
