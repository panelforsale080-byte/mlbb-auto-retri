package com.mlbb.autoretri;

import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reads tap_x/tap_y from mlbb_retri_config.txt (saved by this app — use for gestures). */
public final class ConfigReader {
    private static final String TAG = "MlbbAutoRetriFile";
    private static final Set<String> KEYS;

    static {
        HashSet<String> s = new HashSet<>();
        s.add("tap_x");
        s.add("tap_y");
        KEYS = Collections.unmodifiableSet(s);
    }

    private ConfigReader() {}

    public static final class Tap {
        public final float x;
        public final float y;

        public Tap(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Returns null if file missing or parse fails. */
    public static Tap readTap(File dir) {
        File f = new File(dir, SharedPaths.CONFIG);
        if (!f.isFile()) {
            Log.w(TAG, "no config file: " + f.getAbsolutePath());
            return null;
        }
        try {
            Map<String, String> m = HandoffParse.parse(f, KEYS, 65536);
            String sx = m.get("tap_x");
            String sy = m.get("tap_y");
            if (sx == null || sy == null) {
                Log.w(TAG, "config missing tap_x/tap_y (tolerant parse): " + f.getAbsolutePath());
                return null;
            }
            return new Tap(Float.parseFloat(sx.trim()), Float.parseFloat(sy.trim()));
        } catch (Throwable e) {
            Log.e(TAG, "readTap failed: " + f.getAbsolutePath(), e);
            return null;
        }
    }
}
