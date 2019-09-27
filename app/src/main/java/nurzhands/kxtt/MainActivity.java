package nurzhands.kxtt;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import nurzhands.kxtt.models.Game;
import nurzhands.kxtt.models.Place;
import nurzhands.kxtt.models.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nurzhands";

    private FirebaseUser user;
    private FirebaseFirestore db;
    private SharedPreferences sp;
    private String place;
    private View spinner;
    private List<Place> places = new ArrayList<>();
    private List<String> placeIds = new ArrayList<>();
    private int checkedItem;
    private Place placeInfo;

    private void showPlayingFragment() {
        attachFragment(new PlayingFragment(), "playing");
    }

    private void attachFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(R.id.frag_container, fragment);
//        if (!TextUtils.isEmpty(tag)) {
//            transaction.addToBackStack(tag);
//        }
        transaction.commitNow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cancelNotifications();
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        place = sp.getString("place", "");
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else if (TextUtils.isEmpty(place)) {
            showPlacePicker();
        } else {
            rollIt();
        }
    }

    private void showPlacePicker() {
        spinner = findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);
        db.collection("places")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            places.clear();
                            placeIds.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Place place = document.toObject(Place.class);
                                places.add(place);
                                placeIds.add(document.getId());
                            }
                            spinner.setVisibility(View.GONE);
                            showPlaces();
                        } else {
                            Toast.makeText(MainActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }

    private void showPlaces() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.choose_place);
        builder.setCancelable(!"".equals(place));

        final PlacesAdapter adapter = new PlacesAdapter(this, R.layout.item_place, places);
        builder.setSingleChoiceItems(adapter, checkedItem, null);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem = adapter.getCheckedItem();
                which = checkedItem;
                if (0 <= which && which < placeIds.size()) {
                    dialog.dismiss();
                    savePlace(placeIds.get(which), places.get(which));
                    rollIt();
                } else {
                    Toast.makeText(MainActivity.this, R.string.select_place, Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.create().show();
    }

    private void savePlace(String place, Place placeInfo) {
        this.place = place;
        this.placeInfo = placeInfo;
        sp.edit().putString("place", place).apply();
        sp.edit().putFloat("placeLat", placeInfo.getLat()).apply();
        sp.edit().putFloat("placeLon", placeInfo.getLon()).apply();
        sp.edit().putString("placeName", placeInfo.getName()).apply();
    }

    private void rollIt() {
        updatePlayer();
        showPlayer();
        setupNotifications();
        setupBottomBar();
        showPlayingFragment();
        subscribeToPendingGameResults();
    }

    private void subscribeToPendingGameResults() {
        db.collection("places/" + place + "/pendingresults")
                .whereEqualTo("secondUid", user.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Game game = doc.toObject(Game.class);
                            reviewGame(doc.getId(), game);
                            return;
                        }

                    }
                });

    }

    private void reviewGame(final String docId, final Game game) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.approve_result)
                .setMessage(getGameString(game))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        approveGame(game);
                        removeGame(docId);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeGame(docId);
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private void approveGame(Game game) {
        game.setTimestamp(System.currentTimeMillis());
        db.collection("places/" + place + "/games").add(game);
    }

    private void removeGame(String docId) {
        db.collection("places/" + place + "/pendingresults").document(docId).delete();
    }

    private String getGameString(Game game) {
        return String.format("%s %d - %d %s", game.getFirstName(), game.getFirstGames(), game.getSecondGames(), game.getSecondName());
    }

    private void updatePlayer() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        final String token = task.getResult().getToken();
                        DocumentReference docRef = db.collection("places/" + place + "/players").document(user.getUid());
                        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                User update = documentSnapshot.toObject(User.class);
                                if (update == null) {
                                    update = new User();
                                }
                                sp.edit().putBoolean("admin", update.isAdmin()).apply();
                                update.setName(user.getDisplayName());
                                update.setPhotoUrl(user.getPhotoUrl().toString());
                                update.setUid(user.getUid());
                                update.setToken(token);
                                db.collection("places/" + place + "/players").document(user.getUid()).set(update);
                            }
                        });
                    }
                });
    }

    private void setupBottomBar() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void cancelNotifications() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    private void setupNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic(place);
    }

    private void showPlayer() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.welcome);
            actionBar.setSubtitle(user.getDisplayName().split(" ")[0] + "\uD83D\uDCCD " + sp.getString("placeName", ""));
//            actionBar.setLogo(R.mipmap.ic_launcher);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                askSignOut();
                return true;
            case R.id.action_change_place:
                showPlacePicker();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.playing:
                    showPlayingFragment();
                    return true;
                case R.id.media:
                    attachFragment(new MediaFragment(), "");
                    return true;
                case R.id.ratings:
                    attachFragment(new RatingsFragment(), "");
                    return true;
                case R.id.games:
                    attachFragment(new GamesFragment(), "");
                    return true;
            }
            return false;
        }
    };

    private void askSignOut() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.sign_out)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        signOut();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private void signOut() {
        db.collection("places/" + place + "/playing").document(user.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                sp.edit().remove("place");
                                FirebaseMessaging.getInstance().unsubscribeFromTopic(place);
                                // user is now signed out
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            }
                        });
            }
        });
    }
}
