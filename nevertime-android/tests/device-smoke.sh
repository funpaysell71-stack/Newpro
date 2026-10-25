#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/build/device-test
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell appops set dev.nevertime.client SYSTEM_ALERT_WINDOW allow
# Notification permission only exists on API 33+.
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
if [ "$api" -ge 33 ]; then adb shell pm grant dev.nevertime.client android.permission.POST_NOTIFICATIONS; fi
adb logcat -c
adb shell am instrument -w dev.nevertime.client.test/dev.nevertime.client.SmokeInstrumentation | tee app/build/device-test/instrumentation.txt
rg_cmd=grep
$rg_cmd -q 'PASS:' app/build/device-test/instrumentation.txt
adb shell am start -n dev.nevertime.client/.MainActivity
sleep 2
adb exec-out screencap -p > app/build/device-test/home.png
adb logcat -d > app/build/device-test/logcat.txt
if grep -q 'FATAL EXCEPTION' app/build/device-test/logcat.txt; then exit 1; fi
