package dev.nevertime.client;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.text.*;
public class OverlayService extends Service {
 WindowManager wm;LinearLayout panel;WindowManager.LayoutParams params;TextView clock;
 Handler handler=new Handler(Looper.getMainLooper());
 Runnable tick=new Runnable(){public void run(){if(clock!=null)clock.setText("TIME  "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));handler.postDelayed(this,1000);}};
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(panel!=null)return START_STICKY;
  if(!android.provider.Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(26,20,26,20);
  GradientDrawable d=new GradientDrawable();d.setColor(0xE9111B2E);d.setCornerRadius(22);d.setStroke(2,0xFF6DE5FC);panel.setBackground(d);
  TextView title=new TextView(this);title.setText("◆ NEVERTIME");title.setTextColor(0xFF6DE5FC);title.setTextSize(17);panel.addView(title);
  clock=new TextView(this);clock.setTextColor(-1);clock.setTextSize(14);panel.addView(clock);
  TextView help=new TextView(this);help.setText("Drag to move • Tap to hide");help.setTextColor(0xFFB4C4D7);help.setTextSize(11);panel.addView(help);
  params=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.START;params.x=30;params.y=150;
  panel.setOnTouchListener(new View.OnTouchListener(){float x,y;int ox,oy;boolean moved;public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:x=e.getRawX();y=e.getRawY();ox=params.x;oy=params.y;moved=false;return true;
   case MotionEvent.ACTION_MOVE:if(Math.abs(e.getRawX()-x)+Math.abs(e.getRawY()-y)>12)moved=true;params.x=ox+(int)(e.getRawX()-x);params.y=oy+(int)(e.getRawY()-y);wm.updateViewLayout(panel,params);return true;
   case MotionEvent.ACTION_UP:if(!moved)stopSelf();return true;
  }return false;}});
  wm.addView(panel,params);handler.post(tick);return START_STICKY;
 }
 @Override public void onDestroy(){handler.removeCallbacks(tick);if(wm!=null&&panel!=null){try{wm.removeView(panel);}catch(Exception ignored){}}panel=null;clock=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
