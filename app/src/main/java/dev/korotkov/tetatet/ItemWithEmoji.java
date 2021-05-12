package dev.korotkov.tetatet;

import java.util.Objects;

public class ItemWithEmoji extends Emoji {

    private String name;

    public ItemWithEmoji(String id, String name, String emoji) {
        super(id, emoji);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemWithEmoji)) return false;
        ItemWithEmoji that = (ItemWithEmoji) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
