package dev.nevertime.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.*;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    @SuppressLint("SetJavaScriptEnabled") // Packaged, trusted UI only; navigation and file/network access are disabled.
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setNavigationBarColor(0xFF000000);
        getWindow().setNavigationBarContrastEnforced(false);
        web = new WebView(this);
        web.setBackgroundColor(0xFF141414);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setTextZoom(100);
        settings.setSupportZoom(false);
        web.addJavascriptInterface(new Actions(), "Nevertime");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public void onPageFinished(WebView view, String url) {
                if (isFinishing() || isDestroyed()) return;
                String minecraft = "не установлен";
                try { minecraft = getPackageManager().getPackageInfo("com.mojang.minecraftpe",0).versionName; }
                catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
                String footer = "Minecraft " + minecraft + "  |  NeverTime Client 1.0";
                web.evaluateJavascript("window.NevertimeUI.init(" + ThemePalette.selected(MainActivity.this) + "," + JSONObject.quote(footer) + ")",null);
            }
        });
        setContentView(web);
        // The window's decor/controller is not available before content is attached.
        web.post(this::hideStatusBar);
        try (InputStream in = getAssets().open("ui/index.html"); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer,0,count);
            web.loadDataWithBaseURL("https://nevertime.invalid/", bytes.toString(StandardCharsets.UTF_8.name()), "text/html", "UTF-8", null);
        } catch (Exception error) { Toast.makeText(this,"Не удалось открыть интерфейс: " + error.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private final class Actions {
        @JavascriptInterface public void launchMinecraft() { runOnUiThread(() -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
            if (intent == null) message("Minecraft не установлен.");
            else try { startActivity(intent); } catch (Exception error) { message("Не удалось запустить Minecraft."); }
        }); }
        @JavascriptInterface public void setTheme(int index) { runOnUiThread(() -> ThemePalette.save(MainActivity.this,index)); }
        @JavascriptInterface public void openDiagnostics() { runOnUiThread(() -> startActivity(new Intent(MainActivity.this,dev.nevertime.client.bridge.BridgeActivity.class))); }
        @JavascriptInterface public void showHud() { runOnUiThread(() -> {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                message("Разрешите показ поверх приложений, затем нажмите «Показать HUD» ещё раз.");
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:" + getPackageName())));
            } else { startService(new Intent(MainActivity.this,OverlayService.class)); message("HUD включён."); }
        }); }
        @JavascriptInterface public void hideHud() { runOnUiThread(() -> { stopService(new Intent(MainActivity.this,OverlayService.class)); message("HUD скрыт."); }); }
    }
    private void hideStatusBar() {
        if (web == null || !web.isAttachedToWindow()) return;
        WindowInsetsController controller = web.getWindowInsetsController();
        if (controller != null) controller.hide(WindowInsets.Type.statusBars());
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideStatusBar();
    }
    private void message(String text) { if(web != null && !isDestroyed()) web.evaluateJavascript("window.NevertimeUI.toast(" + JSONObject.quote(text) + ")",null); }
    @Override public void onBackPressed() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript("window.NevertimeUI ? window.NevertimeUI.close() : false", result -> {
            if (!"true".equals(result)) finish();
        });
    }
    @Override protected void onDestroy() {
        if (web != null) { web.removeJavascriptInterface("Nevertime");web.stopLoading();web.destroy();web = null; }
        super.onDestroy();
    }
}
