package nurzhands.kxtt;


import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.wasabeef.glide.transformations.BlurTransformation;
import nurzhands.kxtt.models.Media;
import nurzhands.kxtt.models.MediaHolder;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static nurzhands.kxtt.TimeToTextUtils.getTimeSpent;

public class MediaFragment extends Fragment {
    private static final String TAG = "nurzhands";

    private static final int PERMISSION_STORAGE = 100;
    private static final int RC_MEDIA = 101;

    private RecyclerView media;
    private View addMedia;
    private FirestoreRecyclerAdapter<Media, MediaHolder> mediaAdapter;
    private Uri picUri;
    private Toast toast;
    private File imageFile;
    private View view;

    private FirebaseUser user;
    private FirebaseFirestore db;

    public MediaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_media, container, false);
        showMedia();
        return view;
    }

    private void showMedia() {
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
                .collection("places/kx/media")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20);

        FirestoreRecyclerOptions<Media> options = new FirestoreRecyclerOptions.Builder<Media>()
                .setQuery(query, Media.class)
                .build();

        mediaAdapter = new FirestoreRecyclerAdapter<Media, MediaHolder>(options) {
            @Override
            public void onBindViewHolder(MediaHolder holder, int position, Media media) {
                holder.view.setTag(media);
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
                        String mediaInfo = media.getOwner() + ", " + getTimeSpent(media.getTimestamp());
                        // TODO show this mediaInfo inside full screen
                        if (media.isVideo()) {
                            Intent intent = new Intent(getContext(), VideoActivity.class);
                            intent.putExtra("video_url", media.getUrl());
                            intent.putExtra("video_info", mediaInfo + " ago.");
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

                return new MediaHolder(view);
            }

            @Override
            public void onDataChanged() {
                // Called each time there is a new query snapshot. You may want to use this method
                // to hide a loading spinner or check for the "no documents" state and update your UI.
                // ...
                Log.d(TAG, "data change");
                media.smoothScrollToPosition(0);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Snackbar.make(media, e.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        };
        media.setAdapter(mediaAdapter);
    }

    private void doAddMedia() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            picUri = FileProvider.getUriForFile(getContext(),
                    "nurzhands.kxtt.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Intent chooserIntent = Intent.createChooser(takePictureIntent, "Capture Image or Video");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeVideoIntent});
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(chooserIntent, RC_MEDIA);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        imageFile = image;

        return image;
    }

    private void addImageToGallery() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, imageFile.getAbsolutePath());
        getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void addToPhoneGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(picUri);
        getContext().sendBroadcast(mediaScanIntent);
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
                toast = Toast.makeText(getContext(), String.format(Locale.US, "Upload is %.02f", progress) + "% done", Toast.LENGTH_SHORT);
                toast.show();
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                if (toast != null) {
                    toast.cancel();
                }
                Toast.makeText(getContext(), "Upload is paused", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void addMediaToFirestore(Uri downloadUri, boolean isVideo) {
        Media media = new Media(downloadUri.toString(), isVideo, user.getDisplayName().split(" ")[0]);
        db.collection("places/kx/media").add(media).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                Toast.makeText(getContext(), task.isSuccessful() ? "Uploaded!" : "Failed to upload", Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_MEDIA && resultCode == RESULT_OK) {
            if (data.getData() == null) {
                addImageToGallery();
                addToPhoneGallery();
                uploadMedia(picUri, false);
            } else {
                Uri videoUri = data.getData();
                uploadMedia(videoUri, true);
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
