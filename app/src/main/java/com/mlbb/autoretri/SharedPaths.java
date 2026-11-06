package com.mlbb.autoretri;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

public final class SharedPaths {
    public static final String DIR_NAME = "MlbbEspRetri";
    public static final String STATE = "mlbb_retri_state.txt";
    public static final String CONFIG = "mlbb_retri_config.txt";

    private SharedPaths() {}

    public static File resolveSharedDir(Context ctx) {
        File pub = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIR_NAME);
        if (pub.mkdirs() || pub.isDirectory()) {
            File probe = new File(pub, ".probe_w");
            try {
                FileOutputStream o = new FileOutputStream(probe);
                o.write(1);
                o.close();
                if (probe.delete()) return pub;
            } catch (Throwable ignored) {
            }
        }
        File ext = ctx.getExternalFilesDir(null);
        if (ext != null) return ext;
        return ctx.getFilesDir();
    }
}
