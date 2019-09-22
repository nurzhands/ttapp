package nurzhands.kxtt;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import nurzhands.kxtt.models.Place;

class PlacesAdapter extends ArrayAdapter<Place> {
    private static final String TAG = "nurzhands";
    private final Context context;
    private final int resource;
    private final List<Place> places;
    private int checkedItem = -1;

    PlacesAdapter(@NonNull Context context, int resource, List<Place> places) {
        super(context, resource, places);
        this.context = context;
        this.resource = resource;
        this.places = places;
    }

    ViewHolder holder;

    class ViewHolder {
        AppCompatRadioButton button;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);

        if (convertView == null) {
            convertView = inflater.inflate(resource, parent, false);
        }

        final ImageView icon = convertView.findViewById(R.id.icon);
        final AppCompatRadioButton button = convertView.findViewById(R.id.button);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkedItem = position;
                notifyDataSetChanged();
            }
        });
        button.setChecked(checkedItem == position);
        button.setText(places.get(position).getName());

        Log.d(TAG, "load image");
        Glide.with(context)
                .load(places.get(position).getLogoUrl())
                .circleCrop()
                .into(icon);
        return convertView;
    }

    int getCheckedItem() {
        return checkedItem;
    }
}
