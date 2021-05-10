package dev.korotkov.tetatet;

public class ItemWithEmoji {

    private String id;
    private String name;
    private String emoji;

    public ItemWithEmoji(String id, String name, String emoji) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

}
