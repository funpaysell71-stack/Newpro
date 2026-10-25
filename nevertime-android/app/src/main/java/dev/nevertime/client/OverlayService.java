package dev.nevertime.client;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.*;

/** User-owned, explicitly started overlay. Never reads Minecraft process data. */
public class OverlayService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String STOP = "dev.nevertime.client.STOP";
    public static volatile boolean running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Widget> widgets = new LinkedHashMap<>();
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private WindowManager wm;
    private SettingsStore store;
    private long started;
    private boolean attached;
    private int batteryPercent = -1;
    private int temperature = Integer.MIN_VALUE;
    private boolean charging;
    private final BroadcastReceiver battery = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            batteryPercent = level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);
            charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0) != 0;
        }
    };
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (!Settings.canDrawOverlays(OverlayService.this)) { stopSelf(); return; }
            try {
                for (Widget w: widgets.values()) w.text.setText(reading(w.id));
            } catch (RuntimeException error) { fail("Не удалось обновить HUD: " + error.getClass().getSimpleName()); return; }
            handler.postDelayed(this,1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        store = new SettingsStore(this);
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        started = SystemClock.elapsedRealtime();
        registerReceiver(battery,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        store.prefs.registerOnSharedPreferenceChangeListener(this);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) { fail("Разрешите отображение поверх приложений"); return START_NOT_STICKY; }
        if (store.count() == 0) { fail("Сначала включите хотя бы один HUD-модуль"); return START_NOT_STICKY; }
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("hud","Плавающий HUD",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open = PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stop = PendingIntent.getService(this,1,new Intent(this,OverlayService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(this,"hud").setSmallIcon(dev.nevertime.client.R.drawable.ic_nevertime)
                .setContentTitle("Nevertime • HUD включён").setContentText("Показатели устройства • нажмите для настройки")
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null,"Остановить",stop).build()).build();
            if (Build.VERSION.SDK_INT >= 34) startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(7,n);
            if (!attached) { rebuild(); attached = true; handler.post(refresh); }
            running = true;
            store.prefs.edit().remove("last_error").apply();
        } catch (RuntimeException error) { fail("HUD не запущен: " + error.getClass().getSimpleName()); }
        return START_NOT_STICKY;
    }
    private void fail(String message) { store.prefs.edit().putString("last_error",message).apply(); stopSelf(); }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private Rect bounds() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics m=wm.getCurrentWindowMetrics();
            android.graphics.Insets in=m.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            Rect r=new Rect(m.getBounds()); r.left+=in.left; r.top+=in.top; r.right-=in.right; r.bottom-=in.bottom; return r;
        }
        android.util.DisplayMetrics m=new android.util.DisplayMetrics(); wm.getDefaultDisplay().getMetrics(m);
        return new Rect(0,0,m.widthPixels,m.heightPixels);
    }
    private void rebuild() {
        removeWidgets();
        if(store.count()==0) { stopSelf(); return; }
        for(String id:SettingsStore.IDS) if(store.enabled(id)) {
            Widget w=new Widget(id); widgets.put(id,w); wm.addView(w.text,w.layout);
            w.text.post(() -> { if(widgets.get(id)==w) place(w); });
        }
    }
    private void place(Widget w) {
        Rect r=bounds();
        w.layout.x=HudMath.position(store.x(w.id),r.width(),w.text.getWidth());
        w.layout.y=HudMath.position(store.y(w.id),r.height(),w.text.getHeight());
        update(w);
    }
    private void update(Widget w) {
        try { if(w.text.isAttachedToWindow()) wm.updateViewLayout(w.text,w.layout); }
        catch(RuntimeException e) { fail("Android отключил плавающий HUD"); }
    }
    private String reading(String id) {
        switch(id) {
            case "clock": return "ЧАСЫ   " + time.format(new Date());
            case "battery": return "БАТАРЕЯ   " + (batteryPercent<0?"—":batteryPercent+"%") + (charging?" +":"")
                + (temperature==Integer.MIN_VALUE?"":String.format(Locale.getDefault(),"  ·  %.1f°C",temperature/10f));
            case "memory":
                ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();
                getSystemService(ActivityManager.class).getMemoryInfo(info);
                return String.format(Locale.getDefault(),"RAM устройства   %.1f / %.1f ГБ",(info.totalMem-info.availMem)/1073741824d,info.totalMem/1073741824d);
            case "session": return "СЕССИЯ HUD   " + HudMath.duration(SystemClock.elapsedRealtime()-started);
            case "display": return String.format(Locale.getDefault(),"ДИСПЛЕЙ   %.0f Гц • не FPS",wm.getDefaultDisplay().getRefreshRate());
            default: return "";
        }
    }
    @android.annotation.SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"}) // Physical screen coordinates; drag-only view calls performClick on release.
    private final class Widget {
        final String id;
        final TextView text;
        final WindowManager.LayoutParams layout;
        Widget(String module) {
            id=module; text=new TextView(OverlayService.this); text.setText(reading(id)); text.setTextColor(0xFFE5F5FF);
            text.setTextSize(store.size(id)); text.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
            text.setPadding(dp(12),dp(9),dp(12),dp(9)); text.setAlpha(store.opacity(id)/100f);
            text.setMaxWidth(Math.max(dp(120),bounds().width()));
            GradientDrawable background=new GradientDrawable(); background.setColor(0xF21A2233); background.setCornerRadius(dp(12));
            background.setStroke(dp(1),0xFF7384B6); text.setBackground(background);
            int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            if(store.locked()) flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            layout=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,PixelFormat.TRANSLUCENT);
            // Android 12 blocks touches through opaque untrusted overlays. Keep window opacity below 0.8 when locked.
            layout.alpha=store.locked()?(float)(1-Math.pow(.25,1d/store.count())):1f;
            layout.gravity=Gravity.TOP|Gravity.LEFT;
            text.setOnTouchListener(new View.OnTouchListener() {
                float startX,startY; int originX,originY;
                @Override public boolean onTouch(View view,MotionEvent event) {
                    switch(event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            startX=event.getRawX(); startY=event.getRawY(); originX=layout.x; originY=layout.y; return true;
                        case MotionEvent.ACTION_MOVE:
                            Rect r=bounds();
                            layout.x=HudMath.clamp(originX+Math.round(event.getRawX()-startX),0,Math.max(0,r.width()-text.getWidth()));
                            layout.y=HudMath.clamp(originY+Math.round(event.getRawY()-startY),0,Math.max(0,r.height()-text.getHeight()));
                            update(Widget.this); return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            Rect area=bounds(); store.position(id,HudMath.normalized(layout.x,area.width(),text.getWidth()),HudMath.normalized(layout.y,area.height(),text.getHeight()));
                            if(event.getActionMasked()==MotionEvent.ACTION_UP) view.performClick(); return true;
                        default: return false;
                    }
                }
            });
        }
    }
    private final Runnable rebuildTask=() -> { if(running) { try { rebuild(); } catch(RuntimeException e) { fail("Не удалось применить настройки HUD"); } } };
    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
        if(key==null || key.equals("locked") || key.equals("layout_revision") || key.endsWith(".enabled") || key.endsWith(".size") || key.endsWith(".opacity")) {
            handler.removeCallbacks(rebuildTask); handler.post(rebuildTask);
        }
    }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); handler.removeCallbacks(rebuildTask); handler.post(rebuildTask); }
    private void removeWidgets() {
        for(Widget w:widgets.values()) { try { wm.removeView(w.text); } catch(IllegalArgumentException ignored) { /* Already removed by Android. */ } }
        widgets.clear();
    }
    @Override public void onDestroy() {
        running=false; handler.removeCallbacksAndMessages(null); store.prefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(battery); removeWidgets(); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
