package com.mlbb.autoretri;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/** Writes mlbb_retri_config.txt for ESP Pure native reader. */
public final class ConfigWriter {
    private static final String TAG = "MlbbAutoRetriFile";

    private ConfigWriter() {}

    public static void write(
            File dir, float tapX, float tapY, boolean lord, boolean turtle, boolean red, boolean blue, boolean crab, boolean lito)
            throws IOException {
        if (dir == null) throw new IOException("dir is null");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("mkdir failed: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new IOException("not a directory: " + dir.getAbsolutePath());
        }

        File tmp = new File(dir, SharedPaths.CONFIG + ".tmp");
        File out = new File(dir, SharedPaths.CONFIG);

        try (FileOutputStream fos = new FileOutputStream(tmp);
                PrintWriter pw =
                        new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            pw.println("tap_x=" + tapX);
            pw.println("tap_y=" + tapY);
            pw.println("lord=" + (lord ? 1 : 0));
            pw.println("turtle=" + (turtle ? 1 : 0));
            pw.println("red=" + (red ? 1 : 0));
            pw.println("blue=" + (blue ? 1 : 0));
            pw.println("crab=" + (crab ? 1 : 0));
            pw.println("lito=" + (lito ? 1 : 0));
            pw.print("\n");
            pw.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "write tmp failed: " + tmp.getAbsolutePath(), e);
            throw new IOException("write tmp failed: " + tmp + " — " + e.getMessage(), e);
        }

        if (tmp.renameTo(out)) {
            Log.i(TAG, "config saved (rename): " + out.getAbsolutePath());
            return;
        }

        Log.w(TAG, "rename " + tmp + " -> " + out + " failed, copying bytes");
        copyFileReplace(tmp, out);
        if (!tmp.delete()) tmp.deleteOnExit();
        Log.i(TAG, "config saved (copy): " + out.getAbsolutePath());
    }

    private static void copyFileReplace(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                fos.write(buf, 0, n);
            }
            fos.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "copy failed src=" + src + " dst=" + dst, e);
            throw new IOException("copy tmp->out failed: " + e.getMessage(), e);
        }
    }
}
