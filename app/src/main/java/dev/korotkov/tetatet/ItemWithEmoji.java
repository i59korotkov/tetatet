package dev.korotkov.tetatet;

import java.io.Serializable;
import java.util.Objects;

public class ItemWithEmoji extends Emoji implements Serializable {

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
        return Objects.equals(hashCode(), that.hashCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name + getId() + getEmoji());
    }
}
