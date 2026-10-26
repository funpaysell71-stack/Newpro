"""Exercise real Activity/WebView cold starts; no Minecraft or LSPosed required."""
import pathlib
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = 'dev.nevertime.client'
OUT = pathlib.Path('startup-results')
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, timeout=90)

def capture(name):
    with (OUT / name).open('wb') as f:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=f, check=True, timeout=30)

try:
    print(adb('install', '-r', sys.argv[1]))
    adb('logcat', '-c')
    for attempt in range(1, 4):
        adb('shell', 'am', 'force-stop', PACKAGE)
        started = adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
        (OUT / f'launch-{attempt}.txt').write_text(started)
        if 'Status: ok' not in started:
            raise AssertionError(started)
        time.sleep(3)
        if not adb('shell', 'pidof', PACKAGE).strip():
            raise AssertionError('App exited after cold start')
        adb('shell', 'uiautomator', 'dump', '/sdcard/nevertime-window.xml')
        xml = adb('shell', 'cat', '/sdcard/nevertime-window.xml')
        (OUT / f'window-{attempt}.xml').write_text(xml)
        root = ET.fromstring(xml)
        text = ' '.join(n.get('text', '') + ' ' + n.get('content-desc', '') for n in root.iter('node'))
        if 'Play' not in text or 'NeverTime' not in text:
            raise AssertionError('Launcher content was not exposed by the rendered WebView: ' + text)
        capture(f'launcher-{attempt}.png')
        print(f'Cold start {attempt}: process alive, launcher visible')
    crashes = adb('logcat', '-b', 'crash', '-d')
    if PACKAGE in crashes and 'FATAL EXCEPTION' in crashes:
        raise AssertionError('App crash found in logcat')
finally:
    (OUT / 'logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
    (OUT / 'crashes.txt').write_text(adb('logcat', '-b', 'crash', '-d'))
