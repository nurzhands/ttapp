package nurzhands.kxtt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

import static nurzhands.kxtt.TimeToTextUtils.getTimeSpent;

public class PlayingFragment extends Fragment {
    private static final String TAG = "nurzhands";
    private static final int RC_GOOGLE = 123;
    private static final int PERMISSION_LOCATION = 456;

    private RecyclerView players;
    private FirestoreRecyclerAdapter<Player, PlayerHolder> playerAdapter;
    private TextView noPlayersText;
    private Button checkIn;
    private Button checkOut;
    private View view;

    private SharedPreferences sp;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private String place;
    private String placeName;

    public PlayingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        place = sp.getString("place", "");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playing, container, false);

        setupCurrently();
        showPlayers();
        setupButtons();

        return view;
    }

    private void setupCurrently() {
        TextView currently = view.findViewById(R.id.currently);
        placeName = sp.getString("placeName", "");
        currently.setText(getString(R.string.currently, placeName));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (playerAdapter != null) {
            playerAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (playerAdapter != null) {
            playerAdapter.stopListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkUserLocation();
                } else {
                    final Snackbar noPerms = Snackbar.make(checkIn, R.string.no_location_permission, Snackbar.LENGTH_INDEFINITE);
                    noPerms.setAction(R.string.open_permissions, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            noPerms.dismiss();
                        }
                    });
                    noPerms.show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    private void showPlayers() {
        noPlayersText = view.findViewById(R.id.no_players_text);

        players = view.findViewById(R.id.players);
        players.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        Query query = FirebaseFirestore.getInstance()
                .collection("places/" + place + "/playing")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Player> options = new FirestoreRecyclerOptions.Builder<Player>()
                .setQuery(query, Player.class)
                .build();

        playerAdapter =
                new FirestoreRecyclerAdapter<Player, PlayerHolder>(options) {
                    @Override
                    public void onBindViewHolder(PlayerHolder holder, int position, Player player) {
                        holder.view.setTag(player);
                        ((TextView) holder.view.findViewById(R.id.name)).setText(player.getName());
                        ((TextView) holder.view.findViewById(R.id.timepassed)).setText(getTimeSpent(getContext(), player.getTimestamp()));
                        Glide.with(getContext())
                                .load(player.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.face)
                                .into((ImageView) holder.view.findViewById(R.id.photo));
                    }

                    @Override
                    public PlayerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        final View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_player, parent, false);

                        view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Player player = (Player) v.getTag();
                                List<Player> list = new ArrayList<>();
                                list.add(player);
                                new StfalconImageViewer.Builder<>(getContext(), list, new ImageLoader<Player>() {
                                    @Override
                                    public void loadImage(ImageView imageView, Player image) {
                                        Glide.with(getContext())
                                                .load(image.getPhotoUrl())
                                                .into(imageView);
                                    }
                                }).withHiddenStatusBar(true).show();
                            }
                        });

                        return new PlayerHolder(view);
                    }

                    @Override
                    public void onDataChanged() {
                        // Called each time there is a new query snapshot. You may want to use this method
                        // to hide a loading spinner or check for the "no documents" state and update your UI.
                        // ...
                        Log.d(TAG, "data change");
                        players.smoothScrollToPosition(0);
                        noPlayersText.setVisibility(playerAdapter != null && playerAdapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    public void onError(FirebaseFirestoreException e) {
                        Snackbar.make(players, e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                };
        players.setAdapter(playerAdapter);
    }

    private void addCurrentPlayer() {
        Player player = new Player(user.getDisplayName(), user.getPhotoUrl().toString(), System.currentTimeMillis());
        db.collection("places/" + place + "/playing").document(user.getUid()).set(player);
    }

    private void setupButtons() {
        view.findViewById(R.id.let_know).setVisibility(sp.getBoolean("hint", true) ? View.VISIBLE : View.GONE);

        checkIn = view.findViewById(R.id.check_in);
        checkIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user == null) {
                    noUserSnackbar();
                    return;
                }
                removeHint();
                checkUserLocation();
            }
        });
        checkOut = view.findViewById(R.id.check_out);
        checkOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user == null) {
                    noUserSnackbar();
                    return;
                }
                removeHint();
                db.collection("places/" + place + "/playing").document(user.getUid()).delete();
            }
        });
    }

    private void checkUserLocation() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int services = apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (services == ConnectionResult.SUCCESS) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_LOCATION);
            } else {
                getLocation();
            }
        } else {
            Dialog errorDialog = apiAvailability.getErrorDialog(getActivity(), services, RC_GOOGLE, null);
            errorDialog.show();
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (fusedLocationClient == null) {
            LocationServices.getFusedLocationProviderClient(getActivity());
        }
        final Toast locationToast = Toast.makeText(getContext(), R.string.checking_location, Toast.LENGTH_SHORT);
        locationToast.show();
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        locationToast.cancel();
                        if (location != null && closeToPlace(location)) {
                            addCurrentPlayer();
                        } else {
                            showLocationError(location == null ? getString(R.string.location_error) : getString(R.string.location_too_far, placeName));
                        }
                    }
                });
    }

    private void showLocationError(String errorMessage) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.cant_check_in);
        builder.setMessage(errorMessage);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private boolean closeToPlace(Location userLocation) {
        Location placeLocation = new Location(place);
        placeLocation.setLatitude(sp.getFloat("placeLat", 0));
        placeLocation.setLongitude(sp.getFloat("placeLon", 0));
        float dist = placeLocation.distanceTo(userLocation);
        return dist <= 500; // meters
    }

    private void removeHint() {
        sp.edit().putBoolean("hint", false).apply();
        view.findViewById(R.id.let_know).setVisibility(View.GONE);
    }

    private void noUserSnackbar() {
        Snackbar.make(players, R.string.no_user, Snackbar.LENGTH_LONG).show();
    }
}
