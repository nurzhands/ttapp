package nurzhands.kxtt;

public class TimeToTextUtils {
    public static String getTimeSpent(long t) {
        t = (System.currentTimeMillis() - t) / 1000 / 60;
        String ss;
        if (t == 0) {
            ss = "now";
        } else if (t < 60) {
            ss = t + "m";
        } else {
            t /= 60;
            if (t < 24) {
                ss = t + "h";
            } else {
                t /= 24;
                ss = t + "d";
            }
        }
        return ss;
    }
}
