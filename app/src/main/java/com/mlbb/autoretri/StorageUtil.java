package com.mlbb.autoretri;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/** Pick a directory we can actually write (Android 10+ scoped storage, etc.). */
public final class StorageUtil {
    private static final String TAG = "MlbbAutoRetriStorage";

    private StorageUtil() {}

    public static File ensureWritableConfigDir(Context ctx) {
        File primary = RetriPreferences.resolveConfigDir(ctx);
        Log.i(TAG, "preferred handoff dir: " + primary.getAbsolutePath());
        if (canWriteDir(primary, "preferred")) return primary;

        File fallbackShared = SharedPaths.resolveSharedDir(ctx);
        if (!primary.equals(fallbackShared) && canWriteDir(fallbackShared, "fallback SharedPaths")) {
            Log.w(TAG, "preferred not writable, using SharedPaths fallback: " + fallbackShared.getAbsolutePath());
            return fallbackShared;
        }

        File ext = ctx.getExternalFilesDir(null);
        if (ext != null && canWriteDir(ext, "externalFilesDir")) {
            Log.w(TAG, "using externalFilesDir: " + ext.getAbsolutePath());
            return ext;
        }

        File intDir = ctx.getFilesDir();
        if (canWriteDir(intDir, "filesDir")) {
            Log.w(TAG, "using filesDir: " + intDir.getAbsolutePath());
            return intDir;
        }

        Log.e(TAG, "no writable directory found; returning preferred (will likely fail): " + primary);
        return primary;
    }

    private static boolean canWriteDir(File dir, String label) {
        if (dir == null) {
            Log.d(TAG, "canWriteDir(" + label + "): null");
            return false;
        }
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "canWriteDir(" + label + "): mkdirs failed " + dir.getAbsolutePath());
            return false;
        }
        if (!dir.isDirectory()) {
            Log.w(TAG, "canWriteDir(" + label + "): not dir " + dir.getAbsolutePath());
            return false;
        }
        File probe = new File(dir, ".autoretri_wprobe");
        try {
            FileOutputStream o = new FileOutputStream(probe);
            o.write(1);
            o.close();
            if (!probe.delete()) probe.deleteOnExit();
            Log.d(TAG, "canWriteDir(" + label + "): OK " + dir.getAbsolutePath());
            return true;
        } catch (Throwable e) {
            Log.w(TAG, "canWriteDir(" + label + "): probe failed " + dir.getAbsolutePath(), e);
            return false;
        }
    }
}
