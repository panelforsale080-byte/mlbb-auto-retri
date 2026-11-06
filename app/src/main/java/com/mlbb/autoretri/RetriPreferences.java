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
    /** Directory where mlbb_retri_config.txt was last written successfully (watch + ESP handoff). */
    static final String PREF_LAST_WRITE_DIR = "last_write_dir";

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

    /** Persist tap + write config to first writable handoff directory. */
    public static SaveResult saveTapAndSyncConfig(Context ctx, float tapX, float tapY) {
        SharedPreferences p = prefs(ctx);
        p.edit().putFloat(PREF_TAP_X, tapX).putFloat(PREF_TAP_Y, tapY).apply();
        return writeConfigFromPrefs(ctx);
    }

    /** Save path + toggles + tap from preferences (main screen Save). */
    public static SaveResult saveFullConfigFromFields(
            Context ctx,
            String pathOverride,
            boolean master,
            float tapX,
            float tapY,
            boolean lord,
            boolean turtle,
            boolean red,
            boolean blue,
            boolean crab,
            boolean lito) {
        SharedPreferences p = prefs(ctx);
        p.edit()
                .putString(PREF_PATH, pathOverride != null ? pathOverride.trim() : "")
                .putBoolean(PREF_MASTER, master)
                .putFloat(PREF_TAP_X, tapX)
                .putFloat(PREF_TAP_Y, tapY)
                .putBoolean(PREF_LORD, lord)
                .putBoolean(PREF_TURTLE, turtle)
                .putBoolean(PREF_RED, red)
                .putBoolean(PREF_BLUE, blue)
                .putBoolean(PREF_CRAB, crab)
                .putBoolean(PREF_LITO, lito)
                .apply();
        return writeConfigFromPrefs(ctx);
    }

    private static SaveResult writeConfigFromPrefs(Context ctx) {
        SharedPreferences p = prefs(ctx);
        File dir = StorageUtil.ensureWritableConfigDir(ctx);
        try {
            ConfigWriter.write(
                    dir,
                    p.getFloat(PREF_TAP_X, 1575f),
                    p.getFloat(PREF_TAP_Y, 661f),
                    p.getBoolean(PREF_LORD, true),
                    p.getBoolean(PREF_TURTLE, true),
                    p.getBoolean(PREF_RED, true),
                    p.getBoolean(PREF_BLUE, true),
                    p.getBoolean(PREF_CRAB, true),
                    p.getBoolean(PREF_LITO, true));
            p.edit().putString(PREF_LAST_WRITE_DIR, dir.getAbsolutePath()).apply();
            return new SaveResult(true, dir, null);
        } catch (Throwable t) {
            return new SaveResult(false, dir, t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }

    /** Where watch service + ConfigReader should look first. */
    public static File effectiveHandoffDir(Context ctx) {
        SharedPreferences p = prefs(ctx);
        String last = p.getString(PREF_LAST_WRITE_DIR, "").trim();
        if (!last.isEmpty()) {
            File f = new File(last);
            if (f.isDirectory()) return f;
        }
        return StorageUtil.ensureWritableConfigDir(ctx);
    }

    public static final class SaveResult {
        public final boolean ok;
        public final File directory;
        public final String error;

        SaveResult(boolean ok, File directory, String error) {
            this.ok = ok;
            this.directory = directory;
            this.error = error;
        }
    }
}
