package nurzhands.kxtt;

import android.content.Context;

public class TimeToTextUtils {
    public static String getTimeSpent(Context context, long t) {
        t = (System.currentTimeMillis() - t) / 1000 / 60;
        String ss;
        if (t == 0) {
            ss = context.getString(R.string.now);
        } else if (t < 60) {
            ss = t + context.getString(R.string.min);
        } else {
            t /= 60;
            if (t < 24) {
                ss = t + context.getString(R.string.hour);
            } else {
                t /= 24;
                ss = t + context.getString(R.string.day);
            }
        }
        return ss;
    }
}
