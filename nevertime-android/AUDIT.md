# Аудит и принятые решения — 25.10.2026

## Исходное состояние

Основной проект содержал два Java-класса: меню с пятью кнопками и часы в TYPE_APPLICATION_OVERLAY. Min/target SDK 32, compile SDK 35. Сервис не был foreground, координаты и настройки не сохранялись, простое нажатие убирало HUD. Gradle Wrapper отсутствовал, workflow полагался на неопределённый системный Gradle. `bedrock-android` содержал NOVA-меню с сессионным Map настроек и оценкой скорости, предполагавшей 20 TPS.

Корневой AGENTS.md и .github/agents описывают команду для генерации сайтов. Данный проект — Android; сайт и его pipeline не менялись. Публикация/развёртывание не выполнялись.

## Архив-ориентир

Изучены список ZIP, apk+.json, APK manifest/resources metadata и наличие лицензий. Архив содержит base.apk и split APK для arm64-v8a, русского языка и xhdpi. Метаданные: `xyz.flarial.client`, версия 1.4.17 (2026101001), min SDK 32, compile/target SDK 36. В base.apk есть Compose/Material3 metadata, рекламные SDK assets и уведомление о производной account-switcher реализации LeviLaunchroid.

Это бинарная поставка, а не исходный проект. Её интерфейс не запускался; список фактически работающих игровых модулей и совместимость с Minecraft из этих метаданных подтвердить нельзя. Ресурсы, закрытый код, иконки и APK не копировались в Nevertime. Оригинальный дизайн создан по заданной палитре и требованиям пользователя. Исходный архив не добавлялся в git.

## Архитектура

1. MainActivity: native Android UI без runtime-зависимостей, системные отступы, прокрутка, навигация, permission/launcher/export flows.
2. SettingsStore: ограниченные значения и относительные координаты, атомарное применение валидного профиля, сохранение на каждое действие.
3. OverlayService: foreground, пять окон с независимыми параметрами, удержание только нужных callbacks/receiver, освобождение на остановке, без автоматического фонового перезапуска.
4. HudMath: чистые расчёты, проверяемые JVM без Android.
5. Bedrock: отдельные чистые функции форматирования + Script API adapters. Сохранение в player dynamic property, выключение action bar, защита от повторных форм.
6. Gradle Wrapper + CI: компиляция, lint, регрессионные тесты и эмулятор. Подписанный debug APK для тестирования; release signing не выдумывается.

## Источники технических решений

- https://developer.android.com/about/versions/14/changes/fgs-types-required — foreground service specialUse.
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start — запуск FGS только по действию в видимой Activity.
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE — правила пропуска касаний через overlays.
- https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/minecraft-server?view=minecraft-bedrock-stable
- https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server-ui/modalformdata?view=minecraft-bedrock-stable
- https://feedback.minecraft.net/hc/en-us/articles/49175370527501-Minecraft-Bedrock-Edition-26-52-Hotfix-Changelog
