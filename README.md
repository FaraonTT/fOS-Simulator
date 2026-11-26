# fOS Simulator 1.1

Симулятор операционной системы **fOS** для Android. Kotlin + Jetpack Compose, Material 3 Expressive (фигуры `MaterialShapes`).

## Что внутри
- Bootanimation `fworld` (7 с) → анимация загрузки fOS (30 с при первом запуске, 10 с после настройки)
- Полная первоначальная настройка: язык, регион, Wi-Fi, дата/время, имя, защита (PIN / пароль / графический ключ), тема и обои, условия
- Экран блокировки со свайпом вверх; без защиты сразу рабочий стол
- Рабочий стол 5×4 с часами 2×4; свайп вниз — уведомления, ещё раз — панель управления; свайп вверх — меню приложений
- fStore (fGram, fTok), fGram (мессенджер), Настройки с разделом «О телефоне»
- **fTok 1.1** — настоящая лента с сервера, без заглушек. Плавающий док: Главная, Поиск, «+», Профиль

## fTok и сервер
Сервер: **[FaraonTT/fTok-Server](https://github.com/FaraonTT/fTok-Server)** (Node 22, хранилище — ветка `storage`, секрет только `GH_TOKEN`).
Запустите сервер, затем в fTok → Профиль укажите его адрес. Пока сервер не задан, лента пустая.

## Версии (проверены 26.11.2026)
| Компонент | Версия |
|---|---|
| Gradle | 9.9.2 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.11.01 (Compose 1.13, Material 3 1.5.0) |
| Media3 (ExoPlayer) | 1.11.1 |
| compileSdk / targetSdk / minSdk | 37.1 / 36 / 26 |
| JDK | 17+ (в CI — 21) |

## Сборка
Открыть проект в Android Studio (актуальной) либо:
```
gradle :app:assembleDebug   # Gradle 9.9.2
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. Нужен Android SDK Platform **37.1**.

> Проект ещё не компилировался в среде разработки автора (нет доступа к Maven/Google) — возможны мелкие ошибки сборки.

## Иконки
SVG-исходники лежат в `design/icons/`. В приложении они подключены как векторные drawable (`app/src/main/res/drawable/ic_*.xml`).
