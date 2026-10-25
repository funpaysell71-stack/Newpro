package dev.nevertime.client;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import static dev.nevertime.client.Design.*;

public class MainActivity extends Activity {
    private SettingsStore store;private LinearLayout body;private int page;private TextView state;private boolean updating;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){if(state!=null)state.setText((OverlayService.running?"N MENU / ONLINE":"N MENU / OFFLINE")+"\n"+installed()+"\nNative game integration: unavailable");handler.postDelayed(this,1500);}};
    @Override public void onCreate(Bundle b){super.onCreate(b);store=new SettingsStore(this);if(b!=null)page=b.getInt("page");render();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("page",page);}
    @Override protected void onResume(){super.onResume();handler.post(tick);}
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
    private void render(){
        state=null;LinearLayout shell=column(this);shell.setBackgroundColor(BG);setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets a=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(a.left,a.top,a.right,a.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});shell.requestApplyInsets();
        boolean wide=getResources().getConfiguration().screenWidthDp>=600;
        LinearLayout row=new LinearLayout(this);row.setOrientation(wide?0:1);shell.addView(row,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout rail=column(this);rail.setPadding(dp(this,16),dp(this,18),dp(this,16),dp(this,14));rail.setBackgroundColor(0xFF101723);
        if(wide){row.addView(rail,new LinearLayout.LayoutParams(dp(this,188),-1));ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_nevertime);rail.addView(logo,new LinearLayout.LayoutParams(dp(this,44),dp(this,44)));text(rail,"NEVERTIME",18,TEXT);text(rail,"5.0 / PREVIEW",10,CYAN);}else row.addView(rail,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout links=new LinearLayout(this);links.setOrientation(wide?1:0);rail.addView(links);
        String[] names={"PLAY","Modules","HUD Editor","Settings"},icons={"play","modules","edit","settings"};
        for(int i=0;i<4;i++){final int next=i;Button b=button(links,names[i],icons[i],page==i,()->{page=next;render();});if(!wide)b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(this,52),1));}
        if(wide){text(rail,"FREE EDITION",10,CYAN);text(rail,"Device HUD + launcher\nИгровой адаптер не подключён",11,MUTED);}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);body=column(this);body.setPadding(dp(this,22),dp(this,20),dp(this,22),dp(this,20));scroll.addView(body);
        row.addView(scroll,wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        switch(page){case 1:modules();break;case 2:editor();break;case 3:settings();break;default:home();}
        body.setAlpha(.3f);body.animate().alpha(1).setDuration(180).start();
    }
    private void home(){
        text(body,"YOUR WORLD. YOUR SPACE.",11,CYAN);text(body,"Nevertime Client",32,TEXT);
        LinearLayout hero=card(body);LinearLayout split=new LinearLayout(this);hero.addView(split);
        LinearLayout info=column(this);split.addView(info,new LinearLayout.LayoutParams(0,-2,1));text(info,"Готов к новой сессии",21,TEXT);
        state=text(info,"Проверяем установленную игру…",12,MUTED);
        if(getResources().getConfiguration().screenWidthDp>=600){Art art=new Art(this);split.addView(art,new LinearLayout.LayoutParams(dp(this,140),dp(this,140)));}
        button(hero,"PLAY","play",true,this::play);
        text(hero,"Откроет установленный Minecraft. Игровые модули пока недоступны; системный HUD работает отдельно.",12,MUTED);
        LinearLayout c=card(body);text(c,"Твой HUD всегда рядом",19,TEXT);text(c,"Меню N, сохранённые профили и показатели устройства. Без дополнительных пакетов, root и изменения игры.",13,MUTED);
        button(c,"Открыть меню N","modules",false,()->startOverlay(false));button(c,"Version Manager","settings",false,this::versionManager);
    }
    private String installed(){try{return "Minecraft "+getPackageManager().getPackageInfo("com.mojang.minecraftpe",0).versionName;}catch(PackageManager.NameNotFoundException e){return "Minecraft не установлен";}}
    private void play(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
        if(launch==null){new AlertDialog.Builder(this).setTitle("Minecraft не установлен").setMessage("Установи купленную игру из Google Play. Nevertime не содержит Minecraft.").setPositiveButton("Google Play",(d,w)->officialStore()).setNegativeButton("Cancel",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Minecraft Version Not Supported").setMessage(installed()+"\nSupported: нет проверенных нативных версий\n\nВ APK отсутствует игровой адаптер. Можно запустить обычный Minecraft с системным HUD; Coordinates и World-модули не активируются.")
            .setPositiveButton("Minecraft + меню N",(d,w)->{if(Settings.canDrawOverlays(this)){startOverlay(false);handler.postDelayed(()->safe(launch),250);}else new AlertDialog.Builder(this).setTitle("Разрешение меню N").setMessage("Для кнопки N нужно разрешение поверх приложений. После выдачи нажми PLAY ещё раз.").setPositiveButton("Разрешить",(x,y)->overlayPermission()).setNegativeButton("Только игра",(x,y)->safe(launch)).show();})
            .setNeutralButton("Version Manager",(d,w)->versionManager()).setNegativeButton("Cancel",null).show();
    }
    private void officialStore(){safe(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.mojang.minecraftpe")));}
    private void versionManager(){
        new AlertDialog.Builder(this).setTitle("Version Manager").setMessage(installed()+"\n"+Compatibility.status(this)+"\n\nGoogle Play предлагает доступную официальную версию, а не произвольный downgrade. Обновление каталога не устанавливает нативный адаптер.")
            .setPositiveButton("Download Compatible Version",(d,w)->new AlertDialog.Builder(this).setTitle("Нет подтверждённой версии").setMessage("Совместимая нативная версия не определена. Можно открыть только официальную страницу игры.").setPositiveButton("Google Play",(a,b)->officialStore()).setNegativeButton("Cancel",null).show())
            .setNeutralButton("Check Again",(d,w)->{toast(installed());versionManager();}).setNegativeButton("Cancel",null).show();
    }
    private void overlayPermission(){safe(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}
    private void startOverlay(boolean edit){
        if(!Settings.canDrawOverlays(this)){new AlertDialog.Builder(this).setTitle("Разрешить меню N").setMessage("Разреши наложение, вернись и нажми кнопку снова. Меню может оставаться поверх других приложений — Nevertime не отслеживает текущую игру.").setPositiveButton("Открыть настройки",(d,w)->overlayPermission()).setNegativeButton("Позже",null).show();return;}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&!store.prefs.getBoolean("notification_asked",false)){store.prefs.edit().putBoolean("notification_asked",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},42);}
        store.lock(!edit);
        try{startForegroundService(new Intent(this,OverlayService.class).setAction(edit?OverlayService.EDIT:OverlayService.SHOW));if(edit)toast("Перетащи виджеты. Нажми N → Save HUD для фиксации.");}catch(RuntimeException e){toast("Android не разрешил запуск: "+e.getClass().getSimpleName());}
    }
    private void modules(){
        text(body,"Module library",28,TEXT);text(body,"DEVICE / реальные данные Android",11,CYAN);
        EditText search=new EditText(this);search.setHint("Поиск модулей");search.setHintTextColor(MUTED);search.setTextColor(TEXT);search.setSingleLine();body.addView(search);
        LinearLayout list=column(this);body.addView(list);renderModules(list,"");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){renderModules(list,s.toString());}});
    }
    private void renderModules(LinearLayout list,String query){list.removeAllViews();String q=query.toLowerCase(java.util.Locale.ROOT).trim();
        String[] detail={"Локальное время телефона","Заряд и температура батареи, не CPU","Используемая / общая RAM телефона","Время работы сервиса Nevertime","Герцы экрана, не FPS Minecraft"};
        for(int i=0;i<SettingsStore.IDS.length;i++){String id=SettingsStore.IDS[i],name=SettingsStore.NAMES[i];if(!name.toLowerCase(java.util.Locale.ROOT).contains(q))continue;LinearLayout c=card(list);toggle(c,name,store.enabled(id),on->store.enabled(id,on));text(c,detail[i],12,MUTED);}
        String[] names={"Coordinates","Chunk Borders","Slime Chunks","Ender Pearl Trajectory","Fullbright","Custom Sky","Fireflies"};
        for(String name:names)if(name.toLowerCase(java.util.Locale.ROOT).contains(q))unavailable(list,name,"Нет подтверждённого доступа к игровому состоянию или рендерингу. Переключателя, имитирующего работу, здесь нет.");
    }
    private void editor(){
        text(body,"HUD workspace",28,TEXT);text(body,"В обычном режиме виджеты фиксированы. Редактор меняет только HUD устройства.",13,MUTED);
        LinearLayout actions=card(body);button(actions,"Edit HUD","edit",true,()->startOverlay(true));button(actions,"Save HUD","settings",false,()->{store.lock(true);toast("Расположение сохранено и зафиксировано");});
        toggle(actions,"Привязка к сетке 8 dp",store.snap(),v->store.prefs.edit().putBoolean("snap",v).apply());
        slider(actions,"HUD Scale (%)",store.scale(),75,160,v->store.prefs.edit().putInt("scale",v).apply());
        button(actions,"Сбросить расположение","back",false,()->{store.resetPositions();store.prefs.edit().putLong("layout_revision",SystemClock.elapsedRealtimeNanos()).apply();toast("Расположение сброшено");});
        for(String id:SettingsStore.IDS){LinearLayout c=card(body);text(c,SettingsStore.NAMES[java.util.Arrays.asList(SettingsStore.IDS).indexOf(id)],17,TEXT);slider(c,"Размер (sp)",store.size(id),10,26,v->store.prefs.edit().putInt(id+".size",v).apply());slider(c,"Непрозрачность (%)",store.opacity(id),30,100,v->store.prefs.edit().putInt(id+".opacity",v).apply());}
        profiles();
    }
    private void profiles(){LinearLayout c=card(body);text(c,"Configuration profiles",20,TEXT);
        button(c,"Сохранить профиль","settings",true,()->{EditText input=new EditText(this);input.setSingleLine();input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
            AlertDialog d=new AlertDialog.Builder(this).setTitle("Имя профиля").setView(input).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(b->{String name=input.getText().toString().trim();if(name.isEmpty()||store.profiles().contains(name)||store.profiles().size()>=20){input.setError("Новое имя, максимум 20 профилей");return;}store.saveProfile(name);d.dismiss();render();}));d.show();});
        for(String name:store.profiles()){button(c,name,"modules",false,()->new AlertDialog.Builder(this).setTitle(name).setPositiveButton("Применить",(d,w)->{try{store.loadProfile(name);store.prefs.edit().putLong("layout_revision",SystemClock.elapsedRealtimeNanos()).apply();render();}catch(Exception e){toast("Профиль повреждён");}}).setNeutralButton("Удалить",(d,w)->{store.deleteProfile(name);render();}).setNegativeButton("Cancel",null).show());}}
    private void settings(){text(body,"Control center",28,TEXT);
        LinearLayout c=card(body);text(c,"Interface",19,TEXT);slider(c,"Непрозрачность панелей (%)",store.panelOpacity(),60,100,v->{store.prefs.edit().putInt("panel_opacity",v).apply();render();});
        String[] colors={"Arctic Cyan","Iris Purple","Lime"};int[] values={CYAN,PURPLE,0xFFA4E975};for(int i=0;i<3;i++){final int color=values[i];button(c,colors[i],"visual",false,()->{store.prefs.edit().putInt("accent",color).apply();toast("Цвет HUD изменён");});}
        LinearLayout performance=card(body);text(performance,"Performance",19,TEXT);text(performance,"Частота обновления системного HUD. Не изменяет производительность Minecraft.",12,MUTED);
        String[] modes={"Low · 3 s","Balanced · 1.5 s","High · 0.75 s"};for(int i=0;i<3;i++){final int n=i;button(performance,modes[i],"settings",store.prefs.getInt("performance",1)==i,()->{store.prefs.edit().putInt("performance",n).apply();render();});}
        LinearLayout versions=card(body);text(versions,"Version Manager",19,TEXT);text(versions,installed()+"\n"+Compatibility.status(this),12,MUTED);
        button(versions,"Проверить установленную игру","settings",false,this::versionManager);
        button(versions,updating?"Обновление…":"Обновить каталог GitHub","world",false,()->{if(updating)return;updating=true;new Thread(()->{String result;try{result=Compatibility.refresh(getApplicationContext());}catch(Exception e){result="Не удалось обновить каталог. Сохранённая информация остаётся доступной.";}final String msg=result;runOnUiThread(()->{updating=false;toast(msg);if(!isFinishing())render();});},"catalog-refresh").start();});
        button(versions,"Обновления Nevertime","world",false,()->safe(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/funpaysell71-stack/Newpro/releases"))));
        button(body,"Diagnostics","settings",false,()->new AlertDialog.Builder(this).setTitle("Diagnostics").setMessage("Nevertime 5.0.0-preview\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\nABI: "+String.join(", ",Build.SUPPORTED_ABIS)+"\n"+installed()+"\nOverlay: "+Settings.canDrawOverlays(this)+"\nService: "+OverlayService.running+"\n"+Compatibility.status(this)+"\n"+store.prefs.getString("last_error","")).setPositiveButton("OK",null).show());
        button(body,"Остановить меню N и HUD","close",false,()->stopService(new Intent(this,OverlayService.class)));
    }
    private void safe(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){toast("Не найдено приложение для этого действия");}}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
