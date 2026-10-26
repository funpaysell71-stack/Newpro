package dev.nevertime.client.bridge;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.JSONObject;
import java.util.Arrays;

/** Receives diagnostics only from the installed Minecraft UID; never accepts commands. */
public final class BridgeProvider extends ContentProvider {
    public static final String PREFS = "bridge_report";
    @Override public boolean onCreate() { return true; }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (!"report".equals(method)) throw new IllegalArgumentException("Unsupported method");
        int callerUid = Binder.getCallingUid();
        String[] packages = getContext().getPackageManager().getPackagesForUid(callerUid);
        if (packages == null || !Arrays.asList(packages).contains("com.mojang.minecraftpe")) {
            throw new SecurityException("Only Minecraft may report bridge state");
        }
        if (extras == null) throw new IllegalArgumentException("Missing report");
        JSONObject report = new JSONObject();
        try {
            for (String key : new String[]{"phase", "detail", "version", "enginePath", "loadBias", "executableSegments", "sha256"}) {
                String value = extras.getString(key, "");
                if (value == null || value.length() > 4096) throw new IllegalArgumentException("Invalid field: " + key);
                report.put(key, value);
            }
            report.put("callerUid", callerUid);
            report.put("pid", Binder.getCallingPid());
            report.put("receivedAt", System.currentTimeMillis());
            report.put("receivedElapsed", SystemClock.elapsedRealtime());
            report.put("bootCount", android.provider.Settings.Global.getInt(getContext().getContentResolver(), "boot_count", -1));
            report.put("gameplayReady", false);
        } catch (org.json.JSONException e) { throw new IllegalArgumentException(e); }
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("json", report.toString()).apply();
        return Bundle.EMPTY;
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
