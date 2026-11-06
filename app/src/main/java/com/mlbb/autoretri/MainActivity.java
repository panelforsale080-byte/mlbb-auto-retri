package com.mlbb.autoretri;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    private static final String PREFS = "autoretri";
    private static final String PREF_PATH = "shared_dir_override";
    private static final String PREF_MASTER = "master_enable";
    private static final String PREF_TAP_X = "tap_x";
    private static final String PREF_TAP_Y = "tap_y";
    private static final String PREF_LORD = "t_lord";
    private static final String PREF_TURTLE = "t_turtle";
    private static final String PREF_RED = "t_red";
    private static final String PREF_BLUE = "t_blue";
    private static final String PREF_CRAB = "t_crab";
    private static final String PREF_LITO = "t_lito";

    private static final int REQ_NOTIF = 2001;

    private EditText pathEdit;
    private EditText tapXEdit;
    private EditText tapYEdit;
    private Switch masterSw;
    private CheckBox ckLord, ckTurtle, ckRed, ckBlue, ckCrab, ckLito;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(32, 48, 32, 32);
        scroll.addView(shell);

        TextView title = new TextView(this);
        title.setTextSize(18f);
        title.setText("MLBB Auto Retri (standalone)\nReads mlbb_retri_state.txt from ESP Pure, taps spell via Accessibility.");
        shell.addView(title);

        TextView dirHint = new TextView(this);
        dirHint.setPadding(0, 16, 0, 0);
        shell.addView(dirHint);

        pathEdit = new EditText(this);
        pathEdit.setHint("Optional: exact shared folder (must match ESP Pure)");
        pathEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        shell.addView(pathEdit);

        tapXEdit = new EditText(this);
        tapXEdit.setHint("tap_x");
        tapXEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        shell.addView(tapXEdit);

        tapYEdit = new EditText(this);
        tapYEdit.setHint("tap_y");
        tapYEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        shell.addView(tapYEdit);

        masterSw = new Switch(this);
        masterSw.setText("Enable auto tap (master)");
        shell.addView(masterSw);

        ckLord = mkCk("Lord", shell);
        ckTurtle = mkCk("Turtle", shell);
        ckRed = mkCk("Red buff", shell);
        ckBlue = mkCk("Blue buff", shell);
        ckCrab = mkCk("Crab", shell);
        ckLito = mkCk("Lito", shell);

        Button save = new Button(this);
        save.setText("Save config + write mlbb_retri_config.txt");
        save.setOnClickListener(v -> saveAll());
        shell.addView(save);

        Button a11y = new Button(this);
        a11y.setText("Open Accessibility (enable tap service)");
        a11y.setOnClickListener(
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        shell.addView(a11y);

        Button start = new Button(this);
        start.setText("Start watch service");
        start.setOnClickListener(v -> {
            ensureNotifThen(() -> RetriWatchService.start(this));
            Toast.makeText(this, "Watch service started", Toast.LENGTH_SHORT).show();
        });
        shell.addView(start);

        File defDir = SharedPaths.resolveSharedDir(this);
        dirHint.setText("Default shared dir:\n" + defDir.getAbsolutePath());

        loadPrefs();
        setContentView(scroll);
    }

    private CheckBox mkCk(String label, LinearLayout shell) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        shell.addView(c);
        return c;
    }

    private void loadPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        pathEdit.setText(p.getString(PREF_PATH, ""));
        tapXEdit.setText(String.valueOf(p.getFloat(PREF_TAP_X, 1575f)));
        tapYEdit.setText(String.valueOf(p.getFloat(PREF_TAP_Y, 661f)));
        masterSw.setChecked(p.getBoolean(PREF_MASTER, true));
        ckLord.setChecked(p.getBoolean(PREF_LORD, true));
        ckTurtle.setChecked(p.getBoolean(PREF_TURTLE, true));
        ckRed.setChecked(p.getBoolean(PREF_RED, true));
        ckBlue.setChecked(p.getBoolean(PREF_BLUE, true));
        ckCrab.setChecked(p.getBoolean(PREF_CRAB, true));
        ckLito.setChecked(p.getBoolean(PREF_LITO, true));
    }

    private void saveAll() {
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            float tx = Float.parseFloat(tapXEdit.getText().toString().trim());
            float ty = Float.parseFloat(tapYEdit.getText().toString().trim());
            p.edit()
                    .putString(PREF_PATH, pathEdit.getText().toString().trim())
                    .putBoolean(PREF_MASTER, masterSw.isChecked())
                    .putFloat(PREF_TAP_X, tx)
                    .putFloat(PREF_TAP_Y, ty)
                    .putBoolean(PREF_LORD, ckLord.isChecked())
                    .putBoolean(PREF_TURTLE, ckTurtle.isChecked())
                    .putBoolean(PREF_RED, ckRed.isChecked())
                    .putBoolean(PREF_BLUE, ckBlue.isChecked())
                    .putBoolean(PREF_CRAB, ckCrab.isChecked())
                    .putBoolean(PREF_LITO, ckLito.isChecked())
                    .apply();

            File dir = resolveDirFromPrefs();
            ConfigWriter.write(
                    dir,
                    tx,
                    ty,
                    ckLord.isChecked(),
                    ckTurtle.isChecked(),
                    ckRed.isChecked(),
                    ckBlue.isChecked(),
                    ckCrab.isChecked(),
                    ckLito.isChecked());
            Toast.makeText(this, "Saved → " + new File(dir, SharedPaths.CONFIG).getAbsolutePath(), Toast.LENGTH_LONG)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "Save failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File resolveDirFromPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String o = p.getString(PREF_PATH, "").trim();
        if (!o.isEmpty()) {
            File f = new File(o);
            if (f.isDirectory() || f.mkdirs()) return f;
        }
        return SharedPaths.resolveSharedDir(this);
    }

    private void ensureNotifThen(Runnable r) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                return;
            }
        }
        r.run();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == REQ_NOTIF && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            RetriWatchService.start(this);
        }
    }
}
