package com.mlbb.autoretri;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
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
    private static final int REQ_NOTIF = 2001;

    private EditText pathEdit;
    private EditText tapXEdit;
    private EditText tapYEdit;
    private Switch masterSw;
    private CheckBox ckLord, ckTurtle, ckRed, ckBlue, ckCrab, ckLito;
    private boolean pendingAnchorOverlay = false;

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
        title.setText("MLBB Auto Retri\nReads ESP Pure state file; taps spell via Accessibility.");
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

        Button overlayAnchor = new Button(this);
        overlayAnchor.setText("Set anchor (overlay on MLBB)");
        overlayAnchor.setOnClickListener(v -> tryStartAnchorOverlay());
        shell.addView(overlayAnchor);

        Button testTap = new Button(this);
        testTap.setText("Test tap (same coords as anchor)");
        testTap.setOnClickListener(
                v -> {
                    if (!RetriAccessibilityService.ready()) {
                        Toast.makeText(
                                        this,
                                        "Enable MLBB Auto Retri (tap) in Accessibility first",
                                        Toast.LENGTH_LONG)
                                .show();
                        return;
                    }
                    try {
                        float tx = Float.parseFloat(tapXEdit.getText().toString().trim());
                        float ty = Float.parseFloat(tapYEdit.getText().toString().trim());
                        boolean ok = RetriAccessibilityService.dispatchTap(tx, ty);
                        Toast.makeText(
                                        this,
                                        ok ? ("Tap sent at " + tx + ", " + ty) : "dispatchGesture failed",
                                        Toast.LENGTH_LONG)
                                .show();
                    } catch (Throwable t) {
                        Toast.makeText(this, "Invalid tap_x/tap_y", Toast.LENGTH_SHORT).show();
                    }
                });
        shell.addView(testTap);

        Button a11y = new Button(this);
        a11y.setText("Open Accessibility (enable tap service)");
        a11y.setOnClickListener(
                v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        shell.addView(a11y);

        Button start = new Button(this);
        start.setText("Start watch service");
        start.setOnClickListener(
                v -> {
                    ensureNotifThen(
                            () -> {
                                RetriWatchService.start(this);
                                Toast.makeText(this, "Watch service started", Toast.LENGTH_SHORT).show();
                            });
                });
        shell.addView(start);

        File defDir = SharedPaths.resolveSharedDir(this);
        dirHint.setText("Default shared dir:\n" + defDir.getAbsolutePath());

        loadPrefs();
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Reload after overlay saved tap_x/tap_y */
        loadPrefs();
        if (pendingAnchorOverlay && canOverlay()) {
            pendingAnchorOverlay = false;
            AnchorOverlayService.start(this);
            Toast.makeText(this, "Drag orange dot to spell → SET ANCHOR", Toast.LENGTH_LONG).show();
        }
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void tryStartAnchorOverlay() {
        if (!canOverlay()) {
            pendingAnchorOverlay = true;
            try {
                startActivity(
                        new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
            } catch (Throwable t) {
                Toast.makeText(this, "Allow display over other apps for this app", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "Grant overlay permission, then try again", Toast.LENGTH_LONG).show();
            return;
        }
        AnchorOverlayService.start(this);
        Toast.makeText(this, "Drag orange dot to spell → SET ANCHOR", Toast.LENGTH_LONG).show();
    }

    private CheckBox mkCk(String label, LinearLayout shell) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        shell.addView(c);
        return c;
    }

    private void loadPrefs() {
        SharedPreferences p = RetriPreferences.prefs(this);
        pathEdit.setText(p.getString(RetriPreferences.PREF_PATH, ""));
        tapXEdit.setText(String.valueOf(p.getFloat(RetriPreferences.PREF_TAP_X, 1575f)));
        tapYEdit.setText(String.valueOf(p.getFloat(RetriPreferences.PREF_TAP_Y, 661f)));
        masterSw.setChecked(p.getBoolean(RetriPreferences.PREF_MASTER, true));
        ckLord.setChecked(p.getBoolean(RetriPreferences.PREF_LORD, true));
        ckTurtle.setChecked(p.getBoolean(RetriPreferences.PREF_TURTLE, true));
        ckRed.setChecked(p.getBoolean(RetriPreferences.PREF_RED, true));
        ckBlue.setChecked(p.getBoolean(RetriPreferences.PREF_BLUE, true));
        ckCrab.setChecked(p.getBoolean(RetriPreferences.PREF_CRAB, true));
        ckLito.setChecked(p.getBoolean(RetriPreferences.PREF_LITO, true));
    }

    private void saveAll() {
        try {
            SharedPreferences p = RetriPreferences.prefs(this);
            float tx = Float.parseFloat(tapXEdit.getText().toString().trim());
            float ty = Float.parseFloat(tapYEdit.getText().toString().trim());
            p.edit()
                    .putString(RetriPreferences.PREF_PATH, pathEdit.getText().toString().trim())
                    .putBoolean(RetriPreferences.PREF_MASTER, masterSw.isChecked())
                    .putFloat(RetriPreferences.PREF_TAP_X, tx)
                    .putFloat(RetriPreferences.PREF_TAP_Y, ty)
                    .putBoolean(RetriPreferences.PREF_LORD, ckLord.isChecked())
                    .putBoolean(RetriPreferences.PREF_TURTLE, ckTurtle.isChecked())
                    .putBoolean(RetriPreferences.PREF_RED, ckRed.isChecked())
                    .putBoolean(RetriPreferences.PREF_BLUE, ckBlue.isChecked())
                    .putBoolean(RetriPreferences.PREF_CRAB, ckCrab.isChecked())
                    .putBoolean(RetriPreferences.PREF_LITO, ckLito.isChecked())
                    .apply();

            File dir = RetriPreferences.resolveConfigDir(this);
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
            tapXEdit.setText(String.valueOf(tx));
            tapYEdit.setText(String.valueOf(ty));
            Toast.makeText(this, "Saved → " + new File(dir, SharedPaths.CONFIG).getAbsolutePath(), Toast.LENGTH_LONG)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "Save failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
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
