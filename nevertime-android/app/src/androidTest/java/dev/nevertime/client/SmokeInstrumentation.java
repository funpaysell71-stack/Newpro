package dev.nevertime.client;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.AtomicReference;

/** Run on a disposable emulator: adb shell am instrument -w dev.nevertime.client.test/dev.nevertime.client.SmokeInstrumentation */
public class SmokeInstrumentation extends Instrumentation {
    private int checks;
    private void check(boolean result,String message) { checks++; if(!result)throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        Activity activity=null;
        SettingsStore store=new SettingsStore(getTargetContext());
        String original=store.snapshot();
        try {
            store.enabled("battery",false); store.saveProfile("instrumentation-test"); store.enabled("battery",true);
            store.loadProfile("instrumentation-test"); check(!store.enabled("battery"),"Profile restores disabled module");
            check(!new SettingsStore(getTargetContext()).enabled("battery"),"Settings survive store recreation");
            String before=store.snapshot();
            try { store.restore("{}"); throw new AssertionError("Invalid profile accepted"); } catch(org.json.JSONException expected) {}
            check(store.snapshot().equals(before),"Invalid profile must not partially modify settings");
            store.position("clock",-4,3); check(store.x("clock")==0 && store.y("clock")==1,"Position is bounded");
            store.enabled("clock",true);
            activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync(); final Activity target=activity;
            check(activity!=null,"Activity starts");
            runOnMainSync(() -> click(target,"HUD")); waitForIdleSync();
            runOnMainSync(() -> click(target,"Профили")); waitForIdleSync();
            runOnMainSync(() -> click(target,"Игра")); waitForIdleSync();
            runOnMainSync(() -> click(target,"Открыть Minecraft")); waitForIdleSync();
            check(!target.isFinishing(),"Missing Minecraft handled without closing activity");
            runOnMainSync(() -> click(target,"Главная")); waitForIdleSync();
            if(android.provider.Settings.canDrawOverlays(getTargetContext())) {
                runOnMainSync(() -> click(target,"Включить HUD")); SystemClock.sleep(1500);
                check(OverlayService.running,"Foreground HUD starts");
                runOnMainSync(() -> store.prefs.edit().putBoolean("locked",true).apply()); SystemClock.sleep(300);
                check(OverlayService.running,"Lock update keeps service alive");
                runOnMainSync(() -> getTargetContext().stopService(new Intent(getTargetContext(),OverlayService.class))); SystemClock.sleep(300);
                check(!OverlayService.running,"Foreground HUD stops");
                runOnMainSync(() -> { for(String id:SettingsStore.IDS)store.enabled(id,false); });
                runOnMainSync(() -> click(target,"Включить HUD")); SystemClock.sleep(300);
                check(!OverlayService.running,"Empty configuration cannot start HUD");
            } else throw new AssertionError("Grant overlay app-op before running integration checks");
            result.putString("stream","PASS: "+checks+" Android smoke assertions\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable error) {
            result.putString("stream","FAIL: "+error+"\n"+android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED,result);
        } finally {
            getTargetContext().stopService(new Intent(getTargetContext(),OverlayService.class));
            store.deleteProfile("instrumentation-test");
            try { store.restore(original); } catch(Exception ignored) {}
        }
    }
    private void click(Activity activity,String text) {
        View view=find(activity.getWindow().getDecorView(),text);
        if(view==null)throw new AssertionError("Missing button: "+text);
        check(view.performClick(),"Click "+text);
    }
    private View find(View v,String text) {
        if(v instanceof Button && ((Button)v).getText().toString().equals(text))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++) { View found=find(((ViewGroup)v).getChildAt(i),text); if(found!=null)return found; }
        return null;
    }
}
