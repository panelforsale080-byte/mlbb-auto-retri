package com.mlbb.autoretri;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/** Shared prefs keys for Auto Retri (MainActivity + AnchorOverlayService). */
public final class RetriPreferences {
    static final String PREFS = "autoretri";
    static final String PREF_PATH = "shared_dir_override";
    static final String PREF_MASTER = "master_enable";
    static final String PREF_TAP_X = "tap_x";
    static final String PREF_TAP_Y = "tap_y";
    static final String PREF_LORD = "t_lord";
    static final String PREF_TURTLE = "t_turtle";
    static final String PREF_RED = "t_red";
    static final String PREF_BLUE = "t_blue";
    static final String PREF_CRAB = "t_crab";
    static final String PREF_LITO = "t_lito";

    private RetriPreferences() {}

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static File resolveConfigDir(Context ctx) {
        String o = prefs(ctx).getString(PREF_PATH, "").trim();
        if (!o.isEmpty()) {
            File f = new File(o);
            if (f.isDirectory() || f.mkdirs()) return f;
        }
        return SharedPaths.resolveSharedDir(ctx);
    }

    /** Persist tap anchor + write mlbb_retri_config.txt. */
    public static void saveTapAndSyncConfig(Context ctx, float tapX, float tapY) throws Exception {
        SharedPreferences p = prefs(ctx);
        p.edit().putFloat(PREF_TAP_X, tapX).putFloat(PREF_TAP_Y, tapY).apply();
        File dir = resolveConfigDir(ctx);
        ConfigWriter.write(
                dir,
                tapX,
                tapY,
                p.getBoolean(PREF_LORD, true),
                p.getBoolean(PREF_TURTLE, true),
                p.getBoolean(PREF_RED, true),
                p.getBoolean(PREF_BLUE, true),
                p.getBoolean(PREF_CRAB, true),
                p.getBoolean(PREF_LITO, true));
    }
}
