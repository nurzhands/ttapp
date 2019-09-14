package nurzhands.kxtt;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import javax.annotation.Nullable;

import nurzhands.kxtt.models.Game;
import nurzhands.kxtt.models.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "nurzhands";

    private FirebaseUser user;
    private FirebaseFirestore db;

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
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            updatePlayer();
            showPlayer();
            setupNotifications();
            setupBottomBar();
            showPlayingFragment();
            subscribeToPendingGameResults();
        }
    }

    private void subscribeToPendingGameResults() {
        db.collection("places/kx/pendingresults")
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
        builder.setTitle("Do you approve this result?")
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
        db.collection("places/kx/games").add(game);
    }

    private void removeGame(String docId) {
        db.collection("places/kx/pendingresults").document(docId).delete();
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
                        DocumentReference docRef = db.collection("places/kx/players").document(user.getUid());
                        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                User update = documentSnapshot.toObject(User.class);
                                if (update == null) {
                                    update = new User();
                                }
                                update.setName(user.getDisplayName());
                                update.setPhotoUrl(user.getPhotoUrl().toString());
                                update.setUid(user.getUid());
                                update.setToken(token);
                                db.collection("places/kx/players").document(user.getUid()).set(update);
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
        FirebaseMessaging.getInstance().subscribeToTopic("all");
    }

    private void showPlayer() {
        String welcomeText = getString(R.string.welcome, user.getDisplayName().split(" ")[0]);
        setTitle(welcomeText);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void askSignOut() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Sign out?")
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
        db.collection("places/kx/playing").document(user.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // user is now signed out
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            }
                        });
            }
        });
    }
}
