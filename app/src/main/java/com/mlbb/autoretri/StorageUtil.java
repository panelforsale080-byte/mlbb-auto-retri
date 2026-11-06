package com.mlbb.autoretri;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;

/** Pick a directory we can actually write (Android 10+ scoped storage, etc.). */
public final class StorageUtil {
    private StorageUtil() {}

    public static File ensureWritableConfigDir(Context ctx) {
        File primary = RetriPreferences.resolveConfigDir(ctx);
        if (canWriteDir(primary)) return primary;

        File fallback = SharedPaths.resolveSharedDir(ctx);
        if (!primary.equals(fallback) && canWriteDir(fallback)) return fallback;

        File ext = ctx.getExternalFilesDir(null);
        if (ext != null && canWriteDir(ext)) return ext;

        File intDir = ctx.getFilesDir();
        if (canWriteDir(intDir)) return intDir;

        return primary;
    }

    private static boolean canWriteDir(File dir) {
        if (dir == null) return false;
        if (!dir.exists() && !dir.mkdirs()) return false;
        if (!dir.isDirectory()) return false;
        File probe = new File(dir, ".autoretri_wprobe");
        try {
            FileOutputStream o = new FileOutputStream(probe);
            o.write(1);
            o.close();
            if (!probe.delete()) probe.deleteOnExit();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
