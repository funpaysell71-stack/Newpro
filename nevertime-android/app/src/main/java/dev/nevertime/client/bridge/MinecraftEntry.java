package dev.nevertime.client.bridge;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class MinecraftEntry implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String TARGET = "com.mojang.minecraftpe";
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static String modulePath;
    @Override public void initZygote(StartupParam param) { modulePath = param.modulePath; }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (!TARGET.equals(param.packageName) || !TARGET.equals(param.processName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam hook) {
                if (hook.hasThrowable() || !STARTED.compareAndSet(false, true)) return;
                Context context = (Context) hook.args[0];
                Thread worker = new Thread(() -> probe(context), "Nevertime-Probe");
                worker.setDaemon(true);
                worker.start();
            }
        });
    }

    private static void probe(Context context) {
        Bundle state = new Bundle();
        try {
            state.putString("version", context.getPackageManager().getPackageInfo(TARGET, 0).versionName);
            report(context, state, "HOOK_ATTACHED", "Модуль запущен в процессе Minecraft.");
            if (!Process.is64Bit()) throw new IllegalStateException("ARM64 process required");
            if (modulePath == null || modulePath.isEmpty()) throw new IllegalStateException("Missing module APK path");
            System.load(modulePath + "!/lib/arm64-v8a/libnevertime_bridge.so");
            report(context, state, "WAITING_ENGINE", "Нативный модуль загружен. Ожидание движка.");
            long deadline = SystemClock.elapsedRealtime() + 120_000L;
            String[] snapshot = null;
            while (SystemClock.elapsedRealtime() < deadline) {
                snapshot = NativeProbe.snapshot();
                if (snapshot != null && snapshot.length == 3 && !snapshot[0].isEmpty()) break;
                Thread.sleep(1000);
            }
            if (snapshot == null || snapshot.length != 3 || snapshot[0].isEmpty()) {
                report(context, state, "ENGINE_TIMEOUT", "Движок не обнаружен за 120 секунд. Перезапустите Minecraft для повтора.");
                return;
            }
            state.putString("enginePath", snapshot[0]);
            state.putString("loadBias", snapshot[1]);
            state.putString("executableSegments", snapshot[2]);
            if (Integer.parseInt(snapshot[2]) < 1) throw new IllegalStateException("Engine has no executable segments");
            report(context, state, "HASHING_ENGINE", "Движок обнаружен. Проверяется SHA-256.");
            String hash = LibraryIdentity.hashLoadedPath(snapshot[0]);
            state.putString("sha256", hash);
            String phase = LibraryIdentity.matches(hash) ? "LIBRARY_MATCHED" : "LIBRARY_MISMATCH";
            String detail = LibraryIdentity.matches(hash)
                ? "Библиотека совпадает с исследованной. Игровые функции ещё не реализованы."
                : "Другая сборка библиотеки. Игровые функции не активируются.";
            // Heartbeat means only that this diagnostic worker is alive, not that gameplay hooks work.
            while (!Thread.currentThread().isInterrupted()) {
                report(context, state, phase, detail);
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable error) {
            report(context, state, "ERROR", error.getClass().getSimpleName() + ": " + error.getMessage());
            XposedBridge.log(error);
        }
    }

    private static void report(Context context, Bundle state, String phase, String detail) {
        state.putString("phase", phase);
        state.putString("detail", detail);
        try {
            context.getContentResolver().call(Uri.parse("content://dev.nevertime.client.bridge"), "report", null, state);
        } catch (Throwable error) {
            // IPC may be blocked by package visibility/ROM restrictions; retain a useful LSPosed log.
            XposedBridge.log("Nevertime " + phase + ": " + detail + "; report failed: " + error);
        }
    }
}
