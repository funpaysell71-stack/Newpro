package dev.nevertime.client.bridge;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.graphics.Typeface;
import android.widget.*;
import org.json.JSONObject;

public final class BridgeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private final Runnable refresh = new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this, 2000); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding,padding,padding,padding);
        layout.setBackgroundColor(0xFF0A1020);
        scroll.addView(layout); setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("NEVERTIME · Подключение");title.setTextSize(22);title.setTextColor(0xFF6DE5FC);layout.addView(title);
        TextView help = new TextView(this);
        help.setText("Включите Nevertime в LSPosed, выберите Minecraft и полностью перезапустите игру. Вернитесь сюда для проверки.\n\nЭто диагностика подключения. KillAura, FullBright, AutoTotem, CrystalAura и замена неба пока не реализованы.\n");
        help.setTextSize(16);help.setTextColor(-1);layout.addView(help);
        status = new TextView(this); status.setTextColor(-1);status.setTextSize(14);status.setTypeface(Typeface.MONOSPACE);status.setTextIsSelectable(true);layout.addView(status);
        Button copy = new Button(this);copy.setText("Копировать отчёт");layout.addView(copy);
        copy.setOnClickListener(v -> {
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Nevertime report", raw()));
            Toast.makeText(this,"Отчёт скопирован",Toast.LENGTH_SHORT).show();
        });
    }
    private String raw() { return getSharedPreferences(BridgeProvider.PREFS, MODE_PRIVATE).getString("json", "{}"); }
    private void render() {
        try {
            JSONObject r = new JSONObject(raw());
            if (!r.has("receivedAt")) {
                status.setText("Отчёта пока нет.\nМодуль не подтвердил подключение.\nЕсли после запуска игры отчёт не появился, проверьте журнал LSPosed: строки Nevertime.");return;
            }
            long age = SystemClock.elapsedRealtime() - r.optLong("receivedElapsed", -1);
            int boot = android.provider.Settings.Global.getInt(getContentResolver(), "boot_count", -1);
            boolean fresh = boot >= 0 && boot == r.optInt("bootCount", -2) && age >= 0 && age < 15000;
            status.setText((fresh ? "Последний отчёт получен недавно" : "Старый отчёт — текущее подключение не подтверждено") + "\n\n" + r.toString(2));
        } catch (Exception error) { status.setText("Не удалось прочитать отчёт: " + error.getMessage()); }
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
