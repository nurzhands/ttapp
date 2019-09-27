package nurzhands.kxtt;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.glide.transformations.BlurTransformation;
import nurzhands.kxtt.models.Media;
import nurzhands.kxtt.models.MediaHolder;

import static android.app.Activity.RESULT_OK;
import static nurzhands.kxtt.TimeToTextUtils.getTimeSpent;

public class MediaFragment extends Fragment {
    private static final String TAG = "nurzhands";

    private static final int PERMISSION_STORAGE = 100;
    private static final int RC_MEDIA = 101;

    private RecyclerView media;
    private View addMedia;
    private FirestoreRecyclerAdapter<Media, MediaHolder> mediaAdapter;
    private Toast toast;
    private View view;

    private FirebaseUser user;
    private FirebaseFirestore db;
    private SharedPreferences sp;
    private String place;
    private View noMediaText;

    public MediaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        place = sp.getString("place", "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_media, container, false);
        showMedia();
        return view;
    }

    private void showMedia() {
        noMediaText = view.findViewById(R.id.no_media_text);

        addMedia = view.findViewById(R.id.add_media);
        addMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_STORAGE);
                } else {
                    doAddMedia();
                }
            }
        });

        media = view.findViewById(R.id.medialist);
        media.setLayoutManager(new GridLayoutManager(getContext(), 3));

        Query query = FirebaseFirestore.getInstance()
                .collection("places/" + place + "/media")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Media> options = new FirestoreRecyclerOptions.Builder<Media>()
                .setQuery(query, Media.class)
                .build();

        mediaAdapter = new FirestoreRecyclerAdapter<Media, MediaHolder>(options) {
            @Override
            public void onBindViewHolder(MediaHolder holder, int position, Media media) {
                holder.view.setTag(media);
                holder.view.setTag(R.id.media_id, getSnapshots().getSnapshot(position).getId());
                RequestBuilder<Drawable> load = Glide.with(MediaFragment.this).load(media.getUrl());
                if (media.isVideo()) {
                    load = load.apply(RequestOptions.bitmapTransform(new BlurTransformation(7, 3)));
                }
                load.into((ImageView) holder.view.findViewById(R.id.image));
                holder.view.findViewById(R.id.videoicon).setVisibility(media.isVideo() ? View.VISIBLE : View.GONE);
            }

            @Override
            public MediaHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_media, parent, false);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Media media = (Media) v.getTag();
                        String mediaInfo = media.getOwner() + ", " + getTimeSpent(getContext(), media.getTimestamp());
                        // TODO show this mediaInfo inside full screen
                        if (media.isVideo()) {
                            Intent intent = new Intent(getContext(), VideoActivity.class);
                            intent.putExtra("video_url", media.getUrl());
                            intent.putExtra("video_info", mediaInfo);
                            startActivity(intent);
                        } else {
                            List<Media> list = new ArrayList<>();
                            list.add(media);
                            new StfalconImageViewer.Builder<>(getContext(), list, new ImageLoader<Media>() {
                                @Override
                                public void loadImage(ImageView imageView, Media image) {
                                    Glide.with(MediaFragment.this)
                                            .load(image.getUrl())
                                            .into(imageView);
                                }
                            }).withHiddenStatusBar(true).show();
                        }
                    }
                });

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Media media = (Media) v.getTag();
                        String docId = (String) v.getTag(R.id.media_id);
                        boolean admin = "dyussenaliyev@gmail.com".equals(user.getEmail()) || "erzhands@gmail.com".equals(user.getEmail()) || sp.getBoolean("admin", false);
                        if (user.getUid().equals(media.getOwnerId()) || admin) {
                            showDeleteMediaDialog(media, docId);
                        }
                        return true;
                    }
                });

                return new MediaHolder(view);
            }

            @Override
            public void onDataChanged() {
                Log.d(TAG, "data change");
                media.smoothScrollToPosition(0);
                noMediaText.setVisibility(mediaAdapter != null && mediaAdapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Snackbar.make(media, e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        };
        media.setAdapter(mediaAdapter);
    }

    private void showDeleteMediaDialog(Media media, final String docId) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.delete_media);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                db.collection("places/" + place + "/media").document(docId).delete();
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void doAddMedia() {
        Matisse.from(this)
                .choose(MimeType.ofAll())
                .countable(true)
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, "nurzhands.kxtt.fileprovider", "TT App"))
                .countable(false)
                //.maxSelectable(1)
                .spanCount(3)
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(new Glide4Engine())
                .theme(R.style.Matisse_Dracula)
                .forResult(RC_MEDIA);
    }

    private void uploadMedia(Uri uri, final boolean isVideo) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        final StorageReference mediaRef = storageRef.child("media/" + uri.getLastPathSegment());
        UploadTask uploadTask = mediaRef.putFile(uri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return mediaRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    addMediaToFirestore(downloadUri, isVideo);
                } else {
                    Snackbar.make(addMedia, R.string.error_uploading, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        // Observe state change events such as progress, pause, and resume
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if (toast != null) {
                    toast.cancel();
                }
                String percentage = String.format(Locale.US, "%.02f", progress) + "%";
                toast = Toast.makeText(getContext(), getString(R.string.upload_progress, percentage), Toast.LENGTH_SHORT);
                toast.show();
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                if (toast != null) {
                    toast.cancel();
                }
                Toast.makeText(getContext(), R.string.upload_pause, Toast.LENGTH_LONG).show();
            }
        });

    }

    private void addMediaToFirestore(Uri downloadUri, boolean isVideo) {
        Media media = new Media(downloadUri.toString(), isVideo, user.getDisplayName().split(" ")[0], user.getUid());
        db.collection("places/" + place + "/media").add(media).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                Toast.makeText(getContext(), task.isSuccessful() ? R.string.uploaded : R.string.upload_fail, Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_MEDIA && resultCode == RESULT_OK) {
            List<Uri> selected = Matisse.obtainResult(data);
            if (!selected.isEmpty()) {
                Uri uri = selected.get(0);
                uploadMedia(uri, uri.toString().contains("video"));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doAddMedia();
                } else {
                    final Snackbar noPerms = Snackbar.make(addMedia, R.string.no_storage_permission, Snackbar.LENGTH_INDEFINITE);
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

    @Override
    public void onResume() {
        super.onResume();
        if (mediaAdapter != null) {
            mediaAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaAdapter != null) {
            mediaAdapter.stopListening();
        }
    }

    @Override
    public void onDestroy() {
        if (toast != null) {
            toast.cancel();
        }
        super.onDestroy();
    }
}
