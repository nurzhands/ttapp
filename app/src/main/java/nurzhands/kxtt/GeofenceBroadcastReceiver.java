package nurzhands.kxtt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import nurzhands.kxtt.models.Player;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "nurzhands";

    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "geofencingEvent has error");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                return;
            }

            Player player = new Player(user.getDisplayName(), user.getPhotoUrl() == null ? "" : user.getPhotoUrl().toString(), System.currentTimeMillis());
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            for (Geofence geofence : triggeringGeofences) {
                String place = geofence.getRequestId();
                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                    db.collection("places/" + place + "/playing").document(user.getUid()).set(player);
                } else {
                    db.collection("places/" + place + "/playing").document(user.getUid()).delete();
                }
            }

            // Get the transition details as a String.
//            String geofenceTransitionDetails = getGeofenceTransitionDetails(
//                    this,
//                    geofenceTransition,
//                    triggeringGeofences
//            );

            // Send notification and log the transition details.
//            sendNotification(geofenceTransitionDetails);
//            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, "geofence_transition_invalid_type");
        }
    }
}
