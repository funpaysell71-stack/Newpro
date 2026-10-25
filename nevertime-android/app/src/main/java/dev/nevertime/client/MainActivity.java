package dev.nevertime.client;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.Locale;

@android.annotation.SuppressLint("SetTextI18n") // Russian-only first release; localization is tracked in TEST_REPORT.
public class MainActivity extends Activity {
    private static final int BG=0xFF0C1019, CARD=0xFF171E2C, INK=0xFFEEF3FF, MUTED=0xFF9AAAC2, BLUE=0xFF80DEFF, PURPLE=0xFFB4A4FF;
    private SettingsStore store;
    private LinearLayout shell, body, nav;
    private TextView liveStatus;
    private int tab;
    private String search="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable statusTick=new Runnable() { public void run() { updateStatus(); handler.postDelayed(this,1000); } };
    private int dp(float v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); store=new SettingsStore(this);
        if(state!=null) { tab=state.getInt("tab",0); search=state.getString("search",""); }
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        shell=column(); shell.setBackgroundColor(BG); setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v,insets) -> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(i.left,i.top,i.right,i.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        shell.requestApplyInsets();
        LinearLayout brand=column(); brand.setPadding(dp(24),dp(18),dp(24),dp(10));
        label(brand,"N / NEVERTIME",21,INK); label(brand,"CLIENT   /   ANDROID",10,BLUE); shell.addView(brand);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body=column(); body.setPadding(dp(20),dp(12),dp(20),dp(24)); scroll.addView(body);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this); nav.setPadding(dp(8),dp(6),dp(8),dp(8)); nav.setBackgroundColor(CARD); shell.addView(nav);
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putInt("tab",tab); out.putString("search",search); }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(statusTick); handler.post(statusTick); }
    @Override protected void onPause() { handler.removeCallbacks(statusTick); super.onPause(); }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private GradientDrawable bg(int color,int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); if(stroke!=0)d.setStroke(dp(1),stroke); return d;
    }
    private TextView label(LinearLayout parent,String text,int size,int color) {
        TextView view=new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setLineSpacing(dp(3),1); if(size>=20)view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(8); parent.addView(view,lp); return view;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout l=column(); l.setPadding(dp(18),dp(18),dp(18),dp(14)); l.setBackground(bg(CARD,0xFF273248));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(14); parent.addView(l,lp); return l;
    }
    private Button button(LinearLayout parent,String title,boolean primary,Runnable action) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary?BG:INK);
        b.setBackground(bg(primary?BLUE:0xFF26324A,0)); b.setMinHeight(dp(50)); b.setPadding(dp(12),dp(10),dp(12),dp(10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8); lp.bottomMargin=dp(5); parent.addView(b,lp);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void render() {
        liveStatus=null; body.removeAllViews(); nav.removeAllViews();
        String[] names={"Главная","HUD","Профили","Игра"};
        for(int i=0;i<names.length;i++) {
            final int index=i; Button b=new Button(this); b.setText(names[i]); b.setAllCaps(false); b.setTextSize(12);
            b.setPadding(0,dp(8),0,dp(8)); b.setTextColor(tab==i?BLUE:MUTED); b.setBackground(bg(tab==i?0xFF25364B:CARD,0));
            nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1)); b.setOnClickListener(v -> { tab=index; render(); });
        }
        switch(tab) { case 1: hud(); break; case 2: profiles(); break; case 3: game(); break; default: home(); }
        body.setAlpha(.4f); body.animate().alpha(1f).setDuration(180).start(); updateStatus();
    }
    private void home() {
        label(body,"Твой экран.\nТвои правила.",32,INK);
        label(body,"Лёгкий HUD и быстрый вход в Minecraft.",14,MUTED);
        LinearLayout hero=card(body);
        GradientDrawable heroBackground=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF243B53,0xFF27213E});
        heroBackground.setCornerRadius(dp(18)); hero.setBackground(heroBackground);
        label(hero,"NEVERTIME / 0.2",11,BLUE);
        liveStatus=label(hero,"Проверка состояния…",18,INK);
        button(hero,"Запустить Minecraft  ↗",true,this::launchMinecraft);
        button(hero,"Включить HUD",false,this::startHud);
        button(hero,"Остановить HUD",false,() -> { stopService(new Intent(this,OverlayService.class)); handler.postDelayed(this::updateStatus,150); });
        LinearLayout info=card(body); label(info,"Настрой один раз",20,INK);
        label(info,"Перемещай виджеты пальцем. Размер, прозрачность и положение сохраняются. Перед игрой включи блокировку касаний на вкладке HUD.",14,MUTED);
        button(info,"Настроить виджеты",false,() -> { tab=1; render(); });
        label(body,"Данные устройства отделены от данных мира. Игровые координаты доступны через отдельное дополнение.",12,MUTED);
    }
    private void updateStatus() {
        if(liveStatus==null)return;
        String error=store.prefs.getString("last_error","");
        liveStatus.setText((OverlayService.running?"● HUD работает":"○ HUD выключен")+"\n"+store.count()+" из 5 модулей включено\n"+
            (Settings.canDrawOverlays(this)?"Наложение разрешено":"Нужно разрешение на наложение")+"\n"+minecraftVersion()+(error.isEmpty()?"":"\n"+error));
    }
    private void startHud() {
        if(!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setTitle("Разрешение для HUD")
                .setMessage("Разреши Nevertime отображаться поверх других приложений. Затем вернись и нажми «Включить HUD».")
                .setPositiveButton("Открыть настройки",(d,w)->safeStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("Позже",null).show(); return;
        }
        if(store.count()==0) { toast("Включи хотя бы один модуль на вкладке HUD"); return; }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
            && !store.prefs.getBoolean("notification_asked",false)) {
            store.prefs.edit().putBoolean("notification_asked",true).apply(); requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);
        }
        try { startForegroundService(new Intent(this,OverlayService.class)); handler.postDelayed(this::updateStatus,250); }
        catch(RuntimeException e) { toast("Не удалось запустить HUD: "+e.getClass().getSimpleName()); }
    }
    private void hud() {
        label(body,"Настрой свой HUD",28,INK); label(body,"Каждый виджет можно перемещать отдельно.",14,MUTED);
        LinearLayout options=card(body);
        toggle(options,"Блокировать касания HUD",store.locked(),on -> store.prefs.edit().putBoolean("locked",on).apply());
        label(options,"При блокировке виджеты становятся прозрачнее, чтобы Android пропускал касания в игру. Для перемещения отключи блокировку.",12,MUTED);
        button(options,"Сбросить расположение",false,() -> { store.resetPositions(); restartIfRunning(); toast("Расположение сброшено"); });
        EditText field=new EditText(this); field.setSingleLine(true); field.setTextColor(INK); field.setHintTextColor(MUTED);
        field.setHint("Поиск модулей и настроек"); field.setTextSize(15); field.setPadding(dp(14),dp(8),dp(14),dp(8));
        field.setBackground(bg(CARD,0xFF34425A)); field.setText(search); body.addView(field,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout modules=column(); modules.setPadding(0,dp(16),0,0); body.addView(modules);
        showModules(modules);
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { search=s.toString(); showModules(modules); }
            public void afterTextChanged(Editable e) {}
        });
    }
    private void showModules(LinearLayout parent) {
        parent.removeAllViews();
        String[] descriptions={"Локальное время телефона.","Заряд и температура батареи, не процессора.","Используемая и общая RAM всего устройства.","Время с запуска сервиса HUD, не игровое время.","Текущая частота экрана в Гц. Это не FPS Minecraft."};
        for(int i=0;i<SettingsStore.IDS.length;i++) {
            String id=SettingsStore.IDS[i], name=SettingsStore.NAMES[i];
            if(!(name+" "+descriptions[i]+" размер прозрачность").toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT).trim()))continue;
            LinearLayout m=card(parent); label(m,"УСТРОЙСТВО / "+String.format(Locale.ROOT,"%02d",i+1),10,PURPLE);
            toggle(m,name,store.enabled(id),enabled -> store.enabled(id,enabled)); label(m,descriptions[i],12,MUTED);
            slider(m,"Размер",store.size(id),10,26," sp",value->store.prefs.edit().putInt(id+".size",value).apply());
            slider(m,"Прозрачность",store.opacity(id),30,100,"%",value->store.prefs.edit().putInt(id+".opacity",value).apply());
        }
        if(parent.getChildCount()==0)label(parent,"Ничего не найдено",16,MUTED);
    }
    private interface BooleanAction { void accept(boolean value); }
    private interface IntAction { void accept(int value); }
    private void toggle(LinearLayout parent,String title,boolean value,BooleanAction action) {
        Switch s=new Switch(this); s.setText(title); s.setTextSize(16); s.setTextColor(INK); s.setChecked(value); s.setMinHeight(dp(52));
        s.setPadding(0,dp(6),0,dp(6)); parent.addView(s,new LinearLayout.LayoutParams(-1,-2)); s.setOnCheckedChangeListener((v,on)->action.accept(on));
    }
    private void slider(LinearLayout parent,String title,int value,int min,int max,String unit,IntAction action) {
        TextView caption=label(parent,title+"  "+value+unit,12,MUTED);
        SeekBar s=new SeekBar(this); s.setContentDescription(title); s.setMax(max-min); s.setProgress(value-min);
        s.setProgressTintList(ColorStateList.valueOf(BLUE)); s.setThumbTintList(ColorStateList.valueOf(BLUE));
        parent.addView(s,new LinearLayout.LayoutParams(-1,dp(44)));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser) { caption.setText(title+"  "+(progress+min)+unit); }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) { action.accept(bar.getProgress()+min); }
        });
    }
    private void profiles() {
        label(body,"Твои профили",28,INK); label(body,"Модули, позиции, размеры, прозрачность и блокировка касаний — в одном профиле.",14,MUTED);
        button(body,"+ Сохранить текущий профиль",true,() -> {
            EditText input=new EditText(this); input.setSingleLine(); input.setHint("Например, Вечер"); input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Новый профиль").setView(input).setPositiveButton("Сохранить",null).setNegativeButton("Отмена",null).create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name=input.getText().toString().trim(); if(name.isEmpty()) { input.setError("Укажи название"); return; }
                if(store.profiles().contains(name)) { input.setError("Это имя уже занято"); return; }
                if(store.profiles().size()>=20) { input.setError("Максимум 20 профилей"); return; }
                store.saveProfile(name); dialog.dismiss(); render(); toast("Профиль сохранён");
            })); dialog.show();
        });
        if(store.profiles().isEmpty()) { LinearLayout empty=card(body); label(empty,"Пока без профилей",20,INK); label(empty,"Настрой HUD и сохрани первый набор.",14,MUTED); }
        for(String name:store.profiles()) {
            LinearLayout c=card(body); label(c,name,20,INK);
            button(c,"Применить",false,() -> {
                try { store.loadProfile(name); restartIfRunning(); toast("Применён: "+name); }
                catch(org.json.JSONException e) { toast("Профиль повреждён. Текущие настройки сохранены."); }
            });
            button(c,"Удалить",false,() -> new AlertDialog.Builder(this).setTitle("Удалить «"+name+"»?")
                .setPositiveButton("Удалить",(d,w)->{store.deleteProfile(name); render();}).setNegativeButton("Отмена",null).show());
        }
    }
    private void restartIfRunning() {
        // Force position refresh without restarting the session timer.
        if(OverlayService.running) store.prefs.edit().putLong("layout_revision",SystemClock.elapsedRealtimeNanos()).apply();
    }
    private String minecraftVersion() {
        try { return "Minecraft "+getPackageManager().getPackageInfo("com.mojang.minecraftpe",0).versionName; }
        catch(PackageManager.NameNotFoundException e) { return "Minecraft не установлен"; }
    }
    private void launchMinecraft() {
        Intent intent=getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
        if(intent==null) { toast("Minecraft не найден. Установи игру из Google Play."); return; } safeStart(intent);
    }
    private void game() {
        label(body,"Minecraft + Nevertime",28,INK);
        LinearLayout installed=card(body); label(installed,minecraftVersion(),20,INK);
        label(installed,"Запускается установленная игра. Nevertime не меняет APK Minecraft.",14,MUTED);
        button(installed,"Открыть Minecraft",true,this::launchMinecraft);
        LinearLayout addon=card(body); label(addon,"HUD внутри мира",23,INK);
        label(addon,"Отдельное дополнение: реальные координаты, направление, измерение, время мира и скорость в блоках за игровой тик. Настройки сохраняются для каждого игрока.",14,MUTED);
        button(addon,"Сохранить дополнение .mcpack",false,() -> safeStart(new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"Nevertime-Bedrock.mcpack"),73));
        label(addon,"1. Сохрани файл.\n2. Открой его через Minecraft из приложения «Файлы».\n3. Включи набор поведения в настройках своего мира.\n4. Используй компас или /scriptevent nevertime:menu.",14,INK);
        label(addon,"Работает только в мирах, где установлен набор. Не добавляет функции на чужие серверы. HUD дополнения занимает строку action bar и может конфликтовать с другими наборами.",12,MUTED);
        LinearLayout about=card(body); label(about,"Честно о совместимости",20,INK);
        label(about,"Android 8.0+ • пакет dev.nevertime.client\nЦелевая версия игры: Bedrock 26.52. Проверка в самой игре ещё требуется. Дополнение использует стабильный Script API 2.0.0.\n\nFPS Minecraft, пинг сервера, зум и изменение рендера недоступны этому приложению через публичные Android API.",13,MUTED);
    }
    private void safeStart(Intent intent) { try { startActivity(intent); } catch(ActivityNotFoundException|SecurityException e) { toast("Нет приложения для этого действия"); } }
    private void safeStart(Intent intent,int code) { try { startActivityForResult(intent,code); } catch(ActivityNotFoundException|SecurityException e) { toast("Системный выбор файла недоступен"); } }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==73 && result==RESULT_OK && data!=null && data.getData()!=null) {
            Uri uri=data.getData();
            new Thread(() -> {
                try(InputStream input=getAssets().open("Nevertime-Bedrock.mcpack"); OutputStream output=getContentResolver().openOutputStream(uri)) {
                    if(output==null)throw new IOException("No output stream"); byte[] buffer=new byte[8192]; int n;
                    while((n=input.read(buffer))!=-1)output.write(buffer,0,n);
                    runOnUiThread(() -> toast("Дополнение сохранено. Открой файл через Minecraft."));
                } catch(IOException|SecurityException e) { runOnUiThread(() -> toast("Не удалось сохранить дополнение. Попробуй другую папку.")); }
            },"export-addon").start();
        }
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
