package dev.nevertime.client;
import android.app.Service;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import static dev.nevertime.client.Design.*;

/** Touch-capturing windows are limited to the N button and the open menu rectangle. */
final class OverlayControls {
    private final Service context;private final WindowManager wm;private final SettingsStore store;
    private ImageButton bubble;private LinearLayout menu,content;private WindowManager.LayoutParams bp,mp;
    private int tab;private boolean closing;
    OverlayControls(Service context,WindowManager wm,SettingsStore store){this.context=context;this.wm=wm;this.store=store;}
    private Rect area(){
        if(Build.VERSION.SDK_INT>=30){WindowMetrics metrics=wm.getCurrentWindowMetrics();Rect r=new Rect(metrics.getBounds());Insets i=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());r.left+=i.left;r.top+=i.top;r.right-=i.right;r.bottom-=i.bottom;return r;}
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getMetrics(m);return new Rect(0,0,m.widthPixels,m.heightPixels);
    }
    private WindowManager.LayoutParams params(int width,int height){WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.LEFT;return p;}
    @android.annotation.SuppressLint({"ClickableViewAccessibility","RtlHardcoded"})
    void attach(){
        bubble=new ImageButton(context);bubble.setImageResource(R.drawable.ic_nevertime);bubble.setContentDescription("Nevertime N — открыть меню");bubble.setPadding(dp(context,5),dp(context,5),dp(context,5),dp(context,5));bubble.setBackground(surface(context,245));
        bp=params(dp(context,52),dp(context,52));Rect r=area();bp.x=HudMath.position(store.prefs.getFloat("n.x",.92f),r.width(),bp.width);bp.y=HudMath.position(store.prefs.getFloat("n.y",.10f),r.height(),bp.height);
        bubble.setOnClickListener(v->{if(menu==null)open();else close();});
        bubble.setOnTouchListener(new View.OnTouchListener(){float x,y;int ox,oy;boolean moved;
            public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:x=e.getRawX();y=e.getRawY();ox=bp.x;oy=bp.y;moved=false;return true;
                case MotionEvent.ACTION_MOVE:float dx=e.getRawX()-x,dy=e.getRawY()-y;if(Math.hypot(dx,dy)>dp(context,6))moved=true;if(moved){Rect a=area();bp.x=HudMath.clamp(ox+Math.round(dx),0,Math.max(0,a.width()-bp.width));bp.y=HudMath.clamp(oy+Math.round(dy),0,Math.max(0,a.height()-bp.height));update(bubble,bp);}return true;
                case MotionEvent.ACTION_UP:if(!moved)v.performClick();saveButton();return true;
                case MotionEvent.ACTION_CANCEL:saveButton();return true;
                default:return false;}}
        });wm.addView(bubble,bp);
    }
    private void saveButton(){Rect a=area();store.prefs.edit().putFloat("n.x",HudMath.normalized(bp.x,a.width(),bp.width)).putFloat("n.y",HudMath.normalized(bp.y,a.height(),bp.height)).apply();}
    private void update(View view,WindowManager.LayoutParams p){try{if(view.isAttachedToWindow())wm.updateViewLayout(view,p);}catch(RuntimeException e){context.stopSelf();}}
    View buttonView(){return bubble;}
    View menuView(){return menu;}
    boolean isMenuOpen(){return menu!=null&&!closing;}
    void open(){
        if(menu!=null)return;closing=false;Rect a=area();int width=Math.min(dp(context,560),Math.max(dp(context,200),a.width()-dp(context,84)));int height=Math.min(dp(context,420),Math.max(dp(context,160),a.height()-dp(context,24)));
        menu=column(context);menu.setPadding(dp(context,16),dp(context,12),dp(context,16),dp(context,12));menu.setBackground(surface(context,store.panelOpacity()*255/100));
        LinearLayout top=new LinearLayout(context);menu.addView(top);TextView heading=text(top,"NEVERTIME / 5.0",17,TEXT);heading.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));Button close=button(top,"Закрыть","close",false,this::close);close.setLayoutParams(new LinearLayout.LayoutParams(dp(context,118),dp(context,44)));
        LinearLayout tabs=new LinearLayout(context);menu.addView(tabs);String[] names={"HUD","World","Visual","Settings"},icons={"modules","world","visual","settings"};
        for(int i=0;i<4;i++){final int index=i;Button b=button(tabs,names[i],icons[i],tab==i,()->{tab=index;renderContent();});b.setTextSize(11);b.setPadding(dp(context,5),0,dp(context,5),0);b.setCompoundDrawablePadding(dp(context,3));b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(context,44),1));}
        ScrollView scroll=new ScrollView(context);content=column(context);content.setPadding(0,dp(context,8),0,0);scroll.addView(content);menu.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        mp=params(width,height);mp.x=Math.max(dp(context,72),(a.width()-width)/2);mp.x=Math.min(mp.x,Math.max(0,a.width()-width));mp.y=Math.max(0,(a.height()-height)/2);
        wm.addView(menu,mp);renderContent();menu.setAlpha(0);menu.setTranslationY(dp(context,10));menu.animate().alpha(1).translationY(0).setDuration(160).start();
    }
    private void renderContent(){if(content==null)return;content.removeAllViews();
        switch(tab){
            case 0:
                text(content,"DEVICE HUD · показатели телефона",11,CYAN);
                for(String id:SettingsStore.IDS)toggle(content,SettingsStore.NAMES[java.util.Arrays.asList(SettingsStore.IDS).indexOf(id)],store.enabled(id),v->store.enabled(id,v));
                unavailable(content,"Coordinates","Нет доступа к позиции игрока. Android HUD не читает состояние Minecraft.");
                button(content,store.locked()?"HUD Editor":"Save HUD","edit",true,()->{store.lock(!store.locked());close();});
                slider(content,"HUD Scale (%)",store.scale(),75,160,v->store.prefs.edit().putInt("scale",v).apply());break;
            case 1:for(String name:new String[]{"Chunk Borders","Slime Chunks","Ender Pearl Trajectory"})unavailable(content,name,"Требуется подтверждённый игровой адаптер. В этой сборке не реализован.");break;
            case 2:for(String name:new String[]{"Fullbright","Custom Sky","Fireflies","Visual Presets"})unavailable(content,name,"Нет подтверждённого доступа к игровому рендерингу.");break;
            default:
                text(content,Compatibility.status(context),12,MUTED);
                text(content,"Кнопка N может оставаться поверх других приложений. Закрытое меню не занимает весь экран.",12,MUTED);
                button(content,"Открыть настройки клиента","settings",false,()->{context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));close();});
                button(content,store.locked()?"HUD Editor":"Save HUD","edit",false,()->{store.lock(!store.locked());close();});
                button(content,"Остановить N и HUD","close",false,context::stopSelf);
        }
    }
    void close(){if(menu==null||closing)return;closing=true;final LinearLayout old=menu;old.animate().alpha(0).translationY(dp(context,8)).setDuration(140).withEndAction(()->{remove(old);if(menu==old){menu=null;content=null;closing=false;}}).start();}
    void recreate(){boolean shown=isMenuOpen();destroy();attach();if(shown)open();}
    private void remove(View v){try{if(v!=null&&v.isAttachedToWindow())wm.removeView(v);}catch(IllegalArgumentException ignored){}}
    void destroy(){if(menu!=null){menu.animate().cancel();remove(menu);}remove(bubble);menu=null;content=null;bubble=null;closing=false;}
}
