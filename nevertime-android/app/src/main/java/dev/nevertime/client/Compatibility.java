package dev.nevertime.client;
import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;

/** Remote metadata cannot enable an adapter that is absent from the APK. */
public final class Compatibility {
    public static final String URL="https://raw.githubusercontent.com/funpaysell71-stack/Newpro/codex/nevertime-5/nevertime-android/app/src/main/assets/compatibility.json";
    public static final boolean NATIVE_ADAPTER_BUILT=false;
    public static JSONObject validate(String json) throws Exception {
        JSONObject o=new JSONObject(json);
        if(o.getInt("schema")!=1 || o.getInt("catalogRevision")<1)throw new IOException("Unsupported catalog schema");
        o.getJSONArray("verifiedNativeVersions"); o.getJSONArray("nativeAdapters");
        return o;
    }
    public static String read(InputStream input) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;
        while((n=input.read(bytes))!=-1) { if(out.size()+n>65536)throw new IOException("Catalog exceeds 64 KB");out.write(bytes,0,n); }
        return out.toString(StandardCharsets.UTF_8.name());
    }
    public static String refresh(Context context) throws Exception {
        HttpsURLConnection connection=(HttpsURLConnection)new URL(URL).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000); connection.setInstanceFollowRedirects(false);
        try {
            if(connection.getResponseCode()!=200)throw new IOException("HTTP "+connection.getResponseCode());
            String json;try(InputStream in=connection.getInputStream()){json=read(in);}JSONObject parsed=validate(json);
            new SettingsStore(context).prefs.edit().putString("catalog",json).putLong("catalog_checked",System.currentTimeMillis()).apply();
            return "Каталог #"+parsed.getInt("catalogRevision")+" обновлён. Нативный адаптер в APK отсутствует.";
        } finally { connection.disconnect(); }
    }
    public static String status(Context context) {
        SettingsStore s=new SettingsStore(context);
        try {
            String json=s.prefs.getString("catalog",null);
            if(json==null)try(InputStream in=context.getAssets().open("compatibility.json")){json=read(in);}
            JSONObject catalog=validate(json);
            return "Каталог #"+catalog.getInt("catalogRevision")+" • Native adapter: absent\nSupported: нет проверенных нативных версий";
        }catch(Exception e){return "Каталог недоступен • нативная интеграция отключена";}
    }
}
