package dev.nevertime.client;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import android.view.View;
public class MainActivity extends Activity {
 LinearLayout root; TextView status;
 GradientDrawable bg(){GradientDrawable d=new GradientDrawable();d.setColor(0xFF263850);d.setCornerRadius(24);return d;}
 void label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=22;root.addView(t,p);}
 void button(String s,View.OnClickListener click){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(-1);b.setBackground(bg());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=20;root.addView(b,p);b.setOnClickListener(click);}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xFF0A1020);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(42,70,42,32);root.setBackgroundColor(0xFF0A1020);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 label("NEVERTIME",34,0xFF6DE5FC);label("CLIENT • ANDROID",17,0xFF9FAFC6);label("Minecraft Bedrock companion",15,0xFFE7F1FC);
 status=new TextView(this);status.setText("Floating HUD is off");status.setTextColor(0xFF9FAFC6);root.addView(status);
 button("Allow floating HUD",v->{if(!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));else status.setText("Permission granted");});
 button("Start HUD",v->{if(!Settings.canDrawOverlays(this)){status.setText("Allow overlay permission first");return;}startService(new Intent(this,OverlayService.class));status.setText("HUD enabled");});
 button("Stop HUD",v->{stopService(new Intent(this,OverlayService.class));status.setText("HUD disabled");});
 button("Launch Minecraft",v->{Intent i=getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");if(i!=null)startActivity(i);else status.setText("Minecraft Bedrock not found");});
 button("Проверка подключения",v->startActivity(new Intent(this,dev.nevertime.client.bridge.BridgeActivity.class)));
 label("Drag HUD to move it. Tap to hide. Minecraft FPS and coordinates require game integration.",13,0xFF8394AB);
 }
}
