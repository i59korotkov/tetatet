package dev.korotkov.tetatet;

import junit.framework.TestCase;

public class ItemWithEmojiTest extends TestCase {

    public void testGetName() {
        String name = "Basketball";
        ItemWithEmoji item = new ItemWithEmoji("12", name, "\uD83C\uDFC0");

        assertEquals(item.getName(), name);
    }

    public void testEquals() {
        ItemWithEmoji item1 = new ItemWithEmoji("1", "Basketball", "\uD83C\uDFC0");
        ItemWithEmoji item2 = new ItemWithEmoji("1", "Basketball", "\uD83C\uDFC0");

        assertTrue(item1.equals(item2));
    }

    public void testNotEquals() {
        ItemWithEmoji item1 = new ItemWithEmoji("1", "Basketball", "\uD83C\uDFC0");
        ItemWithEmoji item2 = new ItemWithEmoji("2", "Football", "⚽");

        assertFalse(item1.equals(item2));
    }
}