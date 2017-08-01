package in.arunkumarsampath.bubbly.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * Created by Arunkumar on 22/05/17.
 */

public class Utils {
    public static int dpToPx(double dp) {
        // resources instead of context !!
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5);
    }
}
