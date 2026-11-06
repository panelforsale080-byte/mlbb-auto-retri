package com.mlbb.autoretri;

import java.io.File;
import java.io.PrintWriter;

/** Writes mlbb_retri_config.txt for ESP Pure native reader. */
public final class ConfigWriter {
    private ConfigWriter() {}

    public static void write(
            File dir, float tapX, float tapY, boolean lord, boolean turtle, boolean red, boolean blue, boolean crab, boolean lito)
            throws Exception {
        if (!dir.isDirectory() && !dir.mkdirs()) throw new Exception("mkdir failed: " + dir);
        File tmp = new File(dir, SharedPaths.CONFIG + ".tmp");
        File out = new File(dir, SharedPaths.CONFIG);
        try (PrintWriter pw = new PrintWriter(tmp)) {
            pw.println("tap_x=" + tapX);
            pw.println("tap_y=" + tapY);
            pw.println("lord=" + (lord ? 1 : 0));
            pw.println("turtle=" + (turtle ? 1 : 0));
            pw.println("red=" + (red ? 1 : 0));
            pw.println("blue=" + (blue ? 1 : 0));
            pw.println("crab=" + (crab ? 1 : 0));
            pw.println("lito=" + (lito ? 1 : 0));
        }
        if (!tmp.renameTo(out)) {
            try (PrintWriter pw = new PrintWriter(out)) {
                pw.println("tap_x=" + tapX);
                pw.println("tap_y=" + tapY);
                pw.println("lord=" + (lord ? 1 : 0));
                pw.println("turtle=" + (turtle ? 1 : 0));
                pw.println("red=" + (red ? 1 : 0));
                pw.println("blue=" + (blue ? 1 : 0));
                pw.println("crab=" + (crab ? 1 : 0));
                pw.println("lito=" + (lito ? 1 : 0));
            }
        }
    }
}
