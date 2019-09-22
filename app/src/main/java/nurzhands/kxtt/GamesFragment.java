package nurzhands.kxtt;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nurzhands.kxtt.models.Game;
import nurzhands.kxtt.models.GameHolder;
import nurzhands.kxtt.models.User;

public class GamesFragment extends Fragment {
    private static final String TAG = "nurzhands";

    private RecyclerView games;
    private FirestoreRecyclerAdapter<Game, GameHolder> gameAdapter;
    private View view;

    private SharedPreferences sp;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private List<User> users = new ArrayList<>();
    private View spinner;
    private int checkedItem;
    private User selectedUser;
    private String place;
    private View noGamesText;

    public GamesFragment() {
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
        view = inflater.inflate(R.layout.fragment_games, container, false);

        loadUsers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (gameAdapter != null) {
            gameAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gameAdapter != null) {
            gameAdapter.stopListening();
        }
    }

    private void showGames() {
        addGames();

        noGamesText = view.findViewById(R.id.no_games_text);

        games = view.findViewById(R.id.games);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        games.setLayoutManager(linearLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(games.getContext(),
                linearLayoutManager.getOrientation());
        games.addItemDecoration(dividerItemDecoration);

        Query query = FirebaseFirestore.getInstance()
                .collection("places/" + place + "/games")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Game> options = new FirestoreRecyclerOptions.Builder<Game>()
                .setQuery(query, Game.class)
                .build();

        gameAdapter =
                new FirestoreRecyclerAdapter<Game, GameHolder>(options) {
                    @Override
                    public void onBindViewHolder(GameHolder holder, int position, Game game) {
                        holder.view.setTag(game);
                        TextView firstName = holder.view.findViewById(R.id.firstName);
                        TextView firstGames = holder.view.findViewById(R.id.firstScore);
                        TextView secondName = holder.view.findViewById(R.id.secondName);
                        TextView secondGames = holder.view.findViewById(R.id.secondScore);
                        TextView date = holder.view.findViewById(R.id.date);
                        firstName.setText(getUserName(game.getFirstUid()));
                        secondName.setText(getUserName(game.getSecondUid()));
                        firstGames.setText(String.valueOf(game.getFirstGames()));
                        secondGames.setText(String.valueOf(game.getSecondGames()));
                        date.setText(getDateString(game.getTimestamp()));
                        firstName.setTypeface(null, game.getFirstGames() > game.getSecondGames() ? Typeface.BOLD : Typeface.NORMAL);
                        secondName.setTypeface(null, game.getFirstGames() < game.getSecondGames() ? Typeface.BOLD : Typeface.NORMAL);
                        firstGames.setTypeface(null, game.getFirstGames() > game.getSecondGames() ? Typeface.BOLD : Typeface.NORMAL);
                        secondGames.setTypeface(null, game.getFirstGames() < game.getSecondGames() ? Typeface.BOLD : Typeface.NORMAL);
//                        Glide.with(getContext())
//                                .load(game.getPhotoUrl())
//                                .circleCrop()
//                                .placeholder(R.drawable.face)
//                                .into((ImageView) holder.view.findViewById(R.id.photo));
                    }

                    @Override
                    public GameHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        final View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_game, parent, false);

                        return new GameHolder(view);
                    }

                    @Override
                    public void onDataChanged() {
                        // Called each time there is a new query snapshot. You may want to use this method
                        // to hide a loading spinner or check for the "no documents" state and update your UI.
                        // ...
                        Log.d(TAG, "data change");
                        games.smoothScrollToPosition(0);
                        noGamesText.setVisibility(gameAdapter != null && gameAdapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    public void onError(FirebaseFirestoreException e) {
                        Snackbar.make(games, e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                };
        games.setAdapter(gameAdapter);
        gameAdapter.startListening();
    }

    private String getDateString(long timestamp) {
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm");

        Date result = new Date(timestamp);

        return simple.format(result);
    }

    private String getUserName(String uid) {
        for (User user : users) {
            if (user.getUid().equals(uid)) {
                return user.getName();
            }
        }
        return getString(R.string.n_a);
    }

    private void loadUsers() {
        spinner = view.findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);
        db.collection("places/" + place + "/players")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            users.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                users.add(user);
                            }
                            spinner.setVisibility(View.GONE);
                            showGames();
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.error_getting_documents) + task.getException(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void addGames() {
        view.findViewById(R.id.add_game).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOpponentDialog();
            }
        });
    }

    private void showOpponentDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.choose_opponent);

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUid().equals(user.getUid())) {
                users.remove(i);
                break;
            }
        }

        String[] names = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            names[i] = users.get(i).getName();
        }
        checkedItem = -1; // cow
        builder.setSingleChoiceItems(names, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem = which;
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                which = checkedItem;
                if (0 <= which && which < users.size()) {
                    selectedUser = users.get(which);
                    showScoreDialog();
                    dialog.dismiss();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.create().show();
    }

    private void showScoreDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_game_result, null);
        ((TextView) view.findViewById(R.id.firstName)).setText(user.getDisplayName());
        ((TextView) view.findViewById(R.id.secondName)).setText(selectedUser.getName());
        final EditText wonView = view.findViewById(R.id.won);
        final EditText lostView = view.findViewById(R.id.lost);
        builder.setView(view)
                .setTitle(R.string.enter_scores)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int won = Integer.valueOf(wonView.getText().toString());
                        int lost = Integer.valueOf(lostView.getText().toString());
                        if (0 <= won && won <= 4 && 0 <= lost && lost <= 4 && lost != won) {
                            sendGameResult(won, lost);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getActivity(), R.string.enter_numbers, Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.create().show();
    }

    private void sendGameResult(int won, int lost) {
        Game game = new Game(user.getDisplayName(), selectedUser.getName(), user.getUid(), selectedUser.getUid(), won, lost, selectedUser.getToken());
        db.collection("places/" + place + "/pendingresults").add(game).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
                builder.setTitle(R.string.result_needs_approval);
                builder.setMessage(getString(R.string.result_name_approval, selectedUser.getName()));
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });
    }
}
