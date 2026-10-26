# Game Bridge: повторная проверка, 26 октября 2026

## Результат

Рабочий Game Bridge **не реализован**. Реальные Coordinates, Chunk Borders и Slime Chunks не проверены и не включены. Дизайн и Android-runtime не менялись. Добавлены воспроизводимая инвентаризация APK, тесты инструмента и этот отчёт; они не являются игровыми тестами.

Важное исправление прежнего вывода: **sandbox сам по себе не доказывает невозможность стороннего клиента без root**. Есть открытая архитектура запуска движка внутри процесса лаунчера — LeviLaunchroid. Она принципиально отличается от запуска отдельно установленного приложения через Intent. Полностью проверенного варианта, удовлетворяющего всем ограничениям Nevertime, в этой работе не получено. Нельзя честно объявить ни универсальную невозможность, ни готовую интеграцию.

## Что проверено в Nevertime

- `MainActivity.play()` вызывает `getLaunchIntentForPackage("com.mojang.minecraftpe")`: игра запускается в своём приложении/UID.
- `OverlayService` читает Android BatteryManager, ActivityManager и Display; это не игровые данные.
- `Compatibility.NATIVE_ADAPTER_BUILT` остаётся false, список проверенных нативных версий пуст.
- `bedrock-android/behavior_pack/scripts/core.js` использует `player.location`, но это Script API внутри behavior pack. В APK 5.0 этот код не загружается; возвращать его тайно нельзя.

## Доказательство ограничения текущей архитектуры

1. Живые координаты существуют в состоянии запущенного движка Minecraft. Nevertime не исполняет свой код в этом движке.
2. Android изолирует процессы разных UID; NDK не отменяет эту границу. [AOSP Application Sandbox](https://source.android.com/docs/security/app-sandbox), разделы Application Sandbox/Protections.
3. Для обмена нужен участник со стороны игры: документированный IPC, сервер, скрипт в мире либо код внутри собственного экземпляра движка. Публичный Android IPC-контракт официального Minecraft для X/Y/Z не найден. Это результат поиска, **не** полная проверка всех экспортированных компонентов официального APK: такого APK в рабочей среде нет.
4. Launch Intent не возвращает указатель Player/ClientInstance или поток координат. Overlay предоставляет поверхность другого приложения, а не матрицу камеры или доступ к игровому рендереру.
5. Документированный `Entity.location` относится к `@minecraft/server`, загружаемому через script-модуль behavior pack: [Entity](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/entity?view=minecraft-bedrock-stable), [введение в scripting](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/introduction?view=minecraft-bedrock-stable). Это не Android SDK, который можно подключить Gradle-зависимостью.

Следовательно, **к существующей схеме Intent + Overlay нельзя просто добавить C++ и получить координаты**. Недостающий компонент — производитель игровых данных, а затем игровой renderer/camera interface. Проверять floor(X/16) на придуманных входах не означало бы проверить Chunk Borders в игре.

## Flarial: что установлено, а что нет

- `flarialmc/Launchly`, snapshot `0d49046d568c3be5cedb4d00085566644d604098`: [PackageCoordinator.kt](https://github.com/flarialmc/Launchly/blob/0d49046d568c3be5cedb4d00085566644d604098/app/src/main/java/com/zeuroux/launchly/packageops/PackageCoordinator.kt) использует обычный launch Intent. Это version manager, не доказательство устройства Flarial Android.
- Flarial.Launcher snapshot `4d34c5b9f61a4bd3637f07153be70ac3e5f695d1` — Windows launcher; dll-oss snapshot `40ad1870efd021138241192894cfbfacef1aefa5` — Windows C++/Direct3D. GPL-3.0 / AGPL-3.0 соответственно; код не перенесён.
- В предоставленном `Flarial 1.4.17.zip` обнаружен ARM64 `libflarialclient.so`. Полная инвентаризация и SHA-256 архива: [flarial-apk-inventory.json](flarial-apk-inventory.json).
- `assets/licenses/LeviLaunchroid-NOTICE.txt` прямо ограничивает заявленное заимствование **account-switcher / Microsoft-Xbox authentication model / XAL storage format**. Наличие этого NOTICE не доказывает, что Flarial использует именно загрузчик LeviLaunchroid. Прежнюю догадку нельзя превращать в установленный факт.
- [Официальная страница Flarial Android](https://flarial.xyz/mcpe-client/) и Google Play подтверждают продукт/заявленные функции, но не документируют источник координат, ABI игровых структур и renderer bridge. Проверенного публичного исходника Android Coordinates Flarial не найдено.

## Реальная альтернатива: собственная среда запуска

Изучен [LiteLDev/LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid/tree/5a09ee6194bba09130dcd410020558c5fc045c1e), Apache-2.0, snapshot `5a09ee6194bba09130dcd410020558c5fc045c1e`.

Конкретная цепочка в `app/src/main/java/org/levimc/launcher/`:

- `core/minecraft/GamePackageManager.kt`: `createPackageContext`, извлечение native libraries, `System.load`. Флаг `CONTEXT_IGNORE_SECURITY` здесь документированный Android API для загрузки чужого кода в **свой** контекст, не root и не разрешение читать память отдельного приложения: [Context](https://developer.android.com/reference/android/content/Context#CONTEXT_IGNORE_SECURITY).
- `core/minecraft/MinecraftRuntimePreparer.kt`: подготовка Minecraft runtime, загрузка `libminecraftpe.so`, Preloader и enabled native mods.
- `core/minecraft/MinecraftActivity.kt`: собственная Activity, жизненный цикл и маршрутизация ввода.
- [preloader-android](https://github.com/LiteLDev/preloader-android/tree/5d108da350ce3e7f13f51415f81e65344ecd5d14): `src/pl/internal/NativeModLifecycle.cpp` загружает SO через dlopen; `src/pl/runtime/GameHooks.cpp` разрешает сигнатуры в libminecraftpe.so и устанавливает игровые hooks. Это механизм исполнения модулей в том же процессе, а не IPC с обычным Minecraft.
- Публичные headers Preloader предоставляют lifecycle/config/input/menu/memory APIs. Проверенного высокоуровневого `getLocalPlayerPosition()` и подходящего ABI игрока для доступного здесь игрового бинарника не найдено. Сигнатуры меню не являются сигнатурами координат.

[Документация совместимости](https://docs.levimc.org/LeviLaunchroid/guide/compatibility) заявляет Android 9+/ARM64 и отсекает версии Minecraft до 1.21.80. Это требования upstream, **не** доказательство работоспособности Nevertime на всех версиях после 1.21.80.

В snapshot preloader-android не найден отдельный LICENSE/COPYING/NOTICE через `git ls-files`; Apache-лицензию родительского лаунчера нельзя без проверки считать лицензией отдельного репозитория SDK. Перед переносом его кода требуется установить разрешение на использование.

### Почему не скопирован весь загрузчик

В inspected snapshot присутствуют бинарные `app/src/main/jniLibs/arm64-v8a/libgxcore.so` и `libinbuiltmods.so`. `util/NativeBridgeHelper.kt` объявляет JNI `nativeLoadMinecraftRuntime`, `nativeScanImage`, `nativeRewriteImage`, а соответствующая реализация не найдена в просмотренном source tree. `util/NativeImageGuard.java` вызывает rewriteImage и заменяет извлечённый SO-файл; `GamePackageManager.kt` вызывает этот guard.

Это конкретный **путь изменения извлечённых библиотек**, а не доказательство изменения исходного установленного APK или обхода Android sandbox. Назначение всех изменений и отсутствие обхода защит не установлены. Нельзя автоматически распространять эти бинарники или обещать соответствие ограничениям пользователя только потому, что верхний уровень проекта имеет Apache-лицензию. В Nevertime они не включены и не запускались. Не распространялись файлы Mojang.

## Какие условия нужны для продолжения

**Если требуется именно отдельно установленное официальное приложение:** нужен официальный/разрешённый кооперативный интерфейс со стороны игры. Без него нынешняя архитектура заблокирована. Разрешение overlay или INTERNET его не создаёт.

**Если допустим свой runtime из законно приобретённой копии:** запреты на root, ручные пакеты и изменение исходного APK не обязательно нужно отменять. Но необходимо принять иной способ запуска (движок внутри Nevertime), установить происхождение/лицензии всех компонентов и проверить загрузчик без недопустимых патчей/обходов. Потребуются конкретные официальные APK/split-файлы или доступ к установленной игре на ARM64-устройстве, build/hash привязка и тест движения игрока. В текущей среде нет официального Minecraft и ARM64 игрового устройства. Один только Flarial APK их не заменяет. Это перспективный исследуемый путь, а не доказанная невозможность и не готовый Game Bridge.

**Если допускается участие мира/сервера:** behavior pack даёт документированный player.location; собственный сервер может передавать своё состояние в приложение без клиентского pack, но только для подключённых к нему миров. Это изменение области задачи; произвольные offline-миры/чужие серверы не покрываются. Автоматический импорт pack всё равно остаётся pack, а не решением запрета.

Нельзя подменять ни один вариант OCR, чтением сохранения мира вместо живого состояния, обходом шифрования сетевого трафика или случайными offsets.

## Статусы модулей

| Компонент | Статус | Недостающее доказательство |
|---|---|---|
| Game Bridge | Not Implemented | Проверенная среда исполнения + источник player state |
| Coordinates | Unsupported в текущем Intent/Overlay | Изменяющиеся X/Y/Z реального игрока |
| Chunk Borders | Unsupported в текущем Intent/Overlay | Coordinates + world/camera renderer |
| Slime Chunks | Not Implemented в Android runtime | Проверенный Bedrock алгоритм/эталоны + реальная world visualization |
| Инвентаризация APK | Working | Проверена на предоставленном Flarial и четырёх тестах инструмента |

Игровые критерии успеха **не достигнуты**. APK остаётся лаунчером 5.0 Preview. Новый build проверяет, что исследовательские изменения не сломали сборку, а не доказывает появление игровых функций.

## Воспроизведение

```sh
python nevertime-android/tools/inspect_runtime.py '/path/to/Flarial 1.4.17.zip'
python -m unittest discover -s nevertime-android/tests -p 'test_*.py' -v
```

Инструмент читает имена библиотек и ELF machine, SHA-256 и пути уведомлений. Не загружает SO, не извлекает файлы на диск, не проверяет подпись APK или лицензии и не определяет автоматически работоспособность bridge. Синтетические ZIP/ELF в unit tests — только fixtures парсера; в приложение не попадают.

## Результат сборки этого изменения

Commit исходников: `3cb95e1f2a77da8f19ce53c82d16a61a02ce39b9`.

[GitHub Actions — success](https://github.com/funpaysell71-stack/Newpro/actions/runs/40258412068).
[APK в ZIP-артефакте](https://github.com/funpaysell71-stack/Newpro/actions/runs/40258412068/artifacts/12623540677).

Прошли компиляция APK/test APK, Lint, 4 теста инвентаризации, 11 проверок HudMath и 17 Android smoke assertions на API 35. Это проверки инструмента и прежнего лаунчера, не Minecraft. Исходники Android-приложения не менялись. Итоговый documentation-only commit не требует повторной компиляции.
