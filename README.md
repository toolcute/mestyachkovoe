# Местячковое

Android-приложение с региональными новостями Урала на Kotlin и Jetpack Compose.

Текущая версия: `1.09` (`versionCode = 109`).

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

Готовый APK: `app\build\outputs\apk\debug\app-debug.apk`.

## Иконки приложения

Новая основная иконка (по умолчанию) генерируется из:

```
C:\CuteDroid\UraliconMya.png
```

Предыдущая иконка:

```
C:\CuteDroid\IconMes.png
```

Перегенерация новой иконки по умолчанию:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\generate_icons.ps1
```

Перегенерация предыдущей иконки:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\generate_icons.ps1 -Source "C:\CuteDroid\IconMes.png" -Prefix "ic_launcher_old"
```

Скрипт создаёт `ic_launcher*.png` в `app\src\main\res\mipmap-*`, включая
adaptive foreground и монохромный слой для Material You. Путь к исходнику можно
передать параметром `-Source`.

В приложении название и иконка ярлыка выбираются независимо через
`activity-alias` в `AndroidManifest.xml` (3 названия × 2 иконки).

## Обновления

Проверка обновлений использует GitHub Releases:

```
https://api.github.com/repos/toolcute/mestyachkovoe/releases/latest
```

При наличии более нового тега приложение скачивает APK из релиза и открывает
системный установщик Android. Для установки нужно разрешение «Установка
неизвестных приложений» для Местячкового.

## Публикация релиза

```powershell
gh auth login
gh repo create toolcute/mestyachkovoe --public --source . --remote origin --push
git tag v1.09
git push origin v1.09
gh release create v1.09 "app\build\outputs\apk\debug\app-debug.apk" --title "v1.09" --notes "Первый релиз Местячкового"
```
