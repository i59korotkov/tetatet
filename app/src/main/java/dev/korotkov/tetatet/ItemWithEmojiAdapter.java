package dev.korotkov.tetatet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ItemWithEmojiAdapter extends ArrayAdapter<ItemWithEmoji> {

    private Context context;
    private int resource;

    private int emojiSize;

    private ArrayList<ItemWithEmoji> chosenObjects = new ArrayList<>();

    public ItemWithEmojiAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ItemWithEmoji> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.emojiSize = 16;
    }

    public ItemWithEmojiAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ItemWithEmoji> objects, int emojiSize) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.emojiSize = emojiSize;
    }

    public ItemWithEmojiAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ItemWithEmoji> objects, ArrayList<ItemWithEmoji> chosenObjects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.emojiSize = 16;
        this.chosenObjects = chosenObjects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get item info
        String id = getItem(position).getId();
        String name = getItem(position).getName();
        String emoji = getItem(position).getEmoji();

        // Create item object
        ItemWithEmoji item = new ItemWithEmoji(id, name, emoji);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        TextView adapterItemEmoji = (TextView) convertView.findViewById(R.id.adapter_item_with_emoji_emoji);
        TextView adapterItemName = (TextView) convertView.findViewById(R.id.adapter_item_with_emoji_name);

        adapterItemEmoji.setTextSize(TypedValue.COMPLEX_UNIT_DIP, emojiSize);
        adapterItemEmoji.setText(emoji);
        adapterItemName.setText(name);

        if (chosenObjects.contains(item)) {
            convertView.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.blue)));
            adapterItemName.setTextColor(context.getColor(R.color.white));
        }

        return convertView;
    }
}
