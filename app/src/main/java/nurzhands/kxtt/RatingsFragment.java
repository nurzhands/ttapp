package nurzhands.kxtt;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import nurzhands.kxtt.models.Player;
import nurzhands.kxtt.models.PlayerHolder;
import nurzhands.kxtt.models.User;
import nurzhands.kxtt.models.UserHolder;

import static nurzhands.kxtt.TimeToTextUtils.getTimeSpent;

public class RatingsFragment extends Fragment {
    private static final String TAG = "nurzhands";

    private RecyclerView ratings;
    private FirestoreRecyclerAdapter<User, UserHolder> userAdapter;
    private View view;

    private SharedPreferences sp;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private String place;
    private TextView noRatingsText;

    public RatingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        place = sp.getString("place", "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_ratings, container, false);

        showRatings();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userAdapter != null) {
            userAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
    }

    private void showRatings() {
        noRatingsText = view.findViewById(R.id.no_ratings_text);

        ratings = view.findViewById(R.id.ratings);
        ratings.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        Query query = FirebaseFirestore.getInstance()
                .collection("places/" + place + "/players")
                .whereGreaterThan("rating", 0)
                .orderBy("rating", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        userAdapter =
                new FirestoreRecyclerAdapter<User, UserHolder>(options) {
                    @Override
                    public void onBindViewHolder(UserHolder holder, int position, User user) {
                        holder.view.setTag(user);
                        TextView name = holder.view.findViewById(R.id.name);
                        name.setText(user.getName());
                        TextView rating = holder.view.findViewById(R.id.rating);
                        rating.setText(user.getRating() == 0 ? getString(R.string.rating_tbd) : String.valueOf(user.getRating()));
                        boolean myself = user.getUid() == RatingsFragment.this.user.getUid();
                        name.setTypeface(null, myself ? Typeface.BOLD : Typeface.BOLD);
                        rating.setTypeface(null, myself ? Typeface.BOLD : Typeface.BOLD);
                        Glide.with(getContext())
                                .load(user.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.face)
                                .into((ImageView) holder.view.findViewById(R.id.photo));
                    }

                    @Override
                    public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        final View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_user, parent, false);

                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                User user = (User) v.getTag();
                                List<User> list = new ArrayList<>();
                                list.add(user);
                                new StfalconImageViewer.Builder<>(getContext(), list, new ImageLoader<User>() {
                                    @Override
                                    public void loadImage(ImageView imageView, User image) {
                                        Glide.with(getContext())
                                                .load(image.getPhotoUrl())
                                                .into(imageView);
                                    }
                                }).withHiddenStatusBar(true).show();
                            }
                        });

                        return new UserHolder(view);
                    }

                    @Override
                    public void onDataChanged() {
                        // Called each time there is a new query snapshot. You may want to use this method
                        // to hide a loading spinner or check for the "no documents" state and update your UI.
                        // ...
                        Log.d(TAG, "data change");
                        ratings.smoothScrollToPosition(0);
                        noRatingsText.setVisibility(userAdapter != null && userAdapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    public void onError(FirebaseFirestoreException e) {
                        Snackbar.make(ratings, e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                };
        ratings.setAdapter(userAdapter);
    }
}
