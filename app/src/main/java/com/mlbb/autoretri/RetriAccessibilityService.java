package com.mlbb.autoretri;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Gesture injection for Retribution tap; enable in Accessibility settings on the non-virtual device.
 */
public final class RetriAccessibilityService extends AccessibilityService {
    private static final String TAG = "MlbbAutoRetriA11y";
    private static final int TAP_MS = 48;

    private static volatile RetriAccessibilityService sInstance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        Log.i(TAG, "connected");
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    public static boolean ready() {
        return sInstance != null;
    }

    public static boolean dispatchTap(float x, float y) {
        RetriAccessibilityService svc = sInstance;
        if (svc == null) return false;
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, TAP_MS);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        boolean ok = svc.dispatchGesture(gesture, null, null);
        if (!ok) Log.w(TAG, "dispatchGesture failed");
        return ok;
    }
}
