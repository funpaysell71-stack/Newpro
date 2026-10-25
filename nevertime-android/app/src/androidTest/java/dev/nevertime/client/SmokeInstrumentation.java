package dev.nevertime.client;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import java.io.*;

public class SmokeInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean ok,String description){checks++;if(!ok)throw new AssertionError(description);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;SettingsStore store=new SettingsStore(getTargetContext());String original=store.snapshot();
        try{
            check(store.locked(),"HUD locked by default");
            store.enabled("battery",false);store.prefs.edit().putInt("scale",140).apply();store.saveProfile("smoke-profile");store.enabled("battery",true);store.prefs.edit().putInt("scale",80).apply();store.loadProfile("smoke-profile");
            check(!store.enabled("battery")&&store.scale()==140,"Profile restores modules and scale");
            check(new SettingsStore(getTargetContext()).scale()==140,"Preferences persist across store recreation");
            String saved=store.snapshot();try{store.restore("{}");throw new AssertionError("Bad profile accepted");}catch(org.json.JSONException expected){}
            check(saved.equals(store.snapshot()),"Invalid profile does not partially modify settings");
            check(!Compatibility.NATIVE_ADAPTER_BUILT,"No phantom native adapter");
            try{Compatibility.validate("{\"schema\":999}");throw new AssertionError("Invalid catalog accepted");}catch(org.json.JSONException|IOException expected){}
            Activity a=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();check(a!=null,"Launcher starts");shot("01-home");
            tap(a,"Modules");shot("02-modules");tap(a,"HUD Editor");shot("03-editor");tap(a,"Settings");shot("04-settings");tap(a,"PLAY");
            check(android.provider.Settings.canDrawOverlays(getTargetContext()),"Overlay permission granted for test");
            runOnMainSync(()->{store.lock(true);for(String id:SettingsStore.IDS)store.enabled(id,false);getTargetContext().startForegroundService(new Intent(getTargetContext(),OverlayService.class).setAction(OverlayService.SHOW));});SystemClock.sleep(1500);
            check(OverlayService.running&&OverlayService.controls()!=null,"N button starts even with all HUD modules disabled");
            runOnMainSync(()->OverlayService.controls().buttonView().performClick());SystemClock.sleep(250);check(OverlayService.controls().isMenuOpen(),"N opens ClickGUI");shot("05-clickgui");
            runOnMainSync(()->find(OverlayService.controls().menuView(),"World").performClick());SystemClock.sleep(250);shot("06-world-unavailable");
            runOnMainSync(()->find(OverlayService.controls().menuView(),"Visual").performClick());SystemClock.sleep(250);shot("07-visual-unavailable");
            runOnMainSync(()->find(OverlayService.controls().menuView(),"HUD").performClick());SystemClock.sleep(100);
            runOnMainSync(()->find(OverlayService.controls().menuView(),"HUD Editor").performClick());SystemClock.sleep(250);check(!store.locked()&&!OverlayService.controls().isMenuOpen(),"Editor unlocks HUD and closes menu");
            runOnMainSync(()->{for(String id:SettingsStore.IDS)store.enabled(id,true);});SystemClock.sleep(350);shot("08-editing-live-hud");
            runOnMainSync(()->OverlayService.controls().buttonView().performClick());SystemClock.sleep(200);
            runOnMainSync(()->find(OverlayService.controls().menuView(),"Save HUD").performClick());SystemClock.sleep(250);check(store.locked(),"Save fixes HUD positions");
            runOnMainSync(()->{View b=OverlayService.controls().buttonView();long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,0,10,10,0),move=MotionEvent.obtain(now,now+20,2,90,90,0),up=MotionEvent.obtain(now,now+40,1,90,90,0);b.dispatchTouchEvent(down);b.dispatchTouchEvent(move);b.dispatchTouchEvent(up);down.recycle();move.recycle();up.recycle();});
            check(store.prefs.contains("n.x")&&store.prefs.contains("n.y"),"N drag position persists");
            runOnMainSync(()->getTargetContext().stopService(new Intent(getTargetContext(),OverlayService.class)));SystemClock.sleep(350);check(!OverlayService.running&&OverlayService.controls()==null,"Service removes HUD and controls");
            result.putString("stream","PASS: "+checks+" Android smoke assertions\n");code=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n"+android.util.Log.getStackTraceString(e));}
        finally{getTargetContext().stopService(new Intent(getTargetContext(),OverlayService.class));store.deleteProfile("smoke-profile");try{store.restore(original);}catch(Exception ignored){}store.prefs.edit().commit();}
        finish(code,result);
    }
    private void tap(Activity a,String label){runOnMainSync(()->{View v=find(a.getWindow().getDecorView(),label);check(v!=null&&v.performClick(),"Navigate "+label);});waitForIdleSync();}
    private View find(View view,String label){if(view instanceof Button&&((Button)view).getText().toString().equals(label))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View f=find(((ViewGroup)view).getChildAt(i),label);if(f!=null)return f;}return null;}
    private void shot(String name)throws Exception{SystemClock.sleep(250);Bitmap b=getUiAutomation().takeScreenshot();if(b==null)throw new AssertionError("Screenshot unavailable");File folder=new File(getTargetContext().getExternalFilesDir(null),"smoke-screenshots");if(!folder.exists()&&!folder.mkdirs())throw new IOException("Cannot create screenshot dir");try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
}
