package dev.korotkov.tetatet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ItemWithEmojiAdapter extends ArrayAdapter<ItemWithEmoji> {

    private Context context;
    private int resource;

    public ItemWithEmojiAdapter(@NonNull Context context, int resource, @NonNull ArrayList<ItemWithEmoji> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get interest info
        String id = getItem(position).getId();
        String name = getItem(position).getName();
        String emoji = getItem(position).getEmoji();

        // Create interest object
        ItemWithEmoji interest = new ItemWithEmoji(id, name, emoji);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        TextView adapterInterestEmoji = (TextView) convertView.findViewById(R.id.adapter_item_with_emoji_emoji);
        TextView adapterInterestName = (TextView) convertView.findViewById(R.id.adapter_item_with_emoji_name);

        adapterInterestEmoji.setText(emoji);
        adapterInterestName.setText(name);

        return convertView;
    }
}
