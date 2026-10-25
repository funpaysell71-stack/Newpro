package dev.nevertime.client;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.*;

public final class SettingsStore {
    public static final String[] IDS = {"clock", "battery", "memory", "session", "display"};
    public static final String[] NAMES = {"Часы", "Батарея", "Память устройства", "Время сессии HUD", "Частота дисплея"};
    public final SharedPreferences prefs;
    public SettingsStore(Context context) { prefs = context.getSharedPreferences("nevertime_v2", Context.MODE_PRIVATE); }
    public boolean enabled(String id) { return prefs.getBoolean(id + ".enabled", id.equals("clock") || id.equals("battery")); }
    public void enabled(String id, boolean value) { prefs.edit().putBoolean(id + ".enabled", value).apply(); }
    public int size(String id) { return HudMath.clamp(prefs.getInt(id + ".size", 14), 10, 26); }
    public int opacity(String id) { return HudMath.clamp(prefs.getInt(id + ".opacity", 90), 30, 100); }
    public float x(String id) { return HudMath.fraction(prefs.getFloat(id + ".x", .04f)); }
    public float y(String id) { return HudMath.fraction(prefs.getFloat(id + ".y", .08f + Arrays.asList(IDS).indexOf(id) * .10f)); }
    public boolean locked() { return prefs.getBoolean("locked", false); }
    public void position(String id, float x, float y) { prefs.edit().putFloat(id + ".x", HudMath.fraction(x)).putFloat(id + ".y", HudMath.fraction(y)).apply(); }
    public void resetPositions() {
        SharedPreferences.Editor e = prefs.edit();
        for (String id : IDS) { e.remove(id + ".x"); e.remove(id + ".y"); }
        e.apply();
    }
    public int count() { int count=0; for (String id:IDS) if(enabled(id)) count++; return count; }
    public String snapshot() {
        JSONObject root = new JSONObject();
        try {
            root.put("locked", locked());
            for (String id : IDS) root.put(id, new JSONObject().put("enabled",enabled(id)).put("size",size(id))
                .put("opacity",opacity(id)).put("x",x(id)).put("y",y(id)));
        } catch (org.json.JSONException e) { throw new IllegalStateException(e); }
        return root.toString();
    }
    public void restore(String json) throws org.json.JSONException {
        JSONObject root = new JSONObject(json);
        SharedPreferences.Editor edit = prefs.edit().putBoolean("locked",root.optBoolean("locked",false));
        for (String id:IDS) {
            JSONObject m=root.getJSONObject(id);
            edit.putBoolean(id+".enabled",m.getBoolean("enabled"));
            edit.putInt(id+".size",HudMath.clamp(m.getInt("size"),10,26));
            edit.putInt(id+".opacity",HudMath.clamp(m.getInt("opacity"),30,100));
            edit.putFloat(id+".x",HudMath.fraction((float)m.getDouble("x")));
            edit.putFloat(id+".y",HudMath.fraction((float)m.getDouble("y")));
        }
        // Only apply after every module has passed validation.
        edit.apply();
    }
    public SortedSet<String> profiles() { return new TreeSet<>(prefs.getStringSet("profiles", Collections.emptySet())); }
    public void saveProfile(String name) {
        SortedSet<String> names=profiles(); names.add(name);
        prefs.edit().putString("profile."+name,snapshot()).putStringSet("profiles",names).apply();
    }
    public void loadProfile(String name) throws org.json.JSONException { restore(prefs.getString("profile."+name,"{}")); }
    public void deleteProfile(String name) {
        SortedSet<String> names=profiles(); names.remove(name);
        prefs.edit().remove("profile."+name).putStringSet("profiles",names).apply();
    }
}
