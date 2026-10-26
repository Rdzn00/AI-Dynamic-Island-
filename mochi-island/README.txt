Mochi Dynamic Island v0.5.5 - sandaran kod sumber penuh
=====================================================

Isi:
- app/, build.gradle.kts, settings.gradle.kts, gradle.properties : projek Android (Kotlin + Jetpack Compose), siap untuk dibina.
- signing/debug.keystore : kunci tandatangan tetap (alias androiddebugkey, kata laluan android).
  Guna kunci ini pada setiap binaan supaya APK baru boleh dipasang terus di atas yang lama
  tanpa uninstall dan kebenaran kekal. Letakkan di ~/.android/debug.keystore sebelum bina.
- ci/github-build.yml : contoh workflow GitHub Actions biasa (tanpa fail teks base64).
  Salin ke .github/workflows/build.yml dalam repo.

Cara bina (mana-mana CI Linux dengan Java 17 + Android SDK + Gradle 8.11.1):
  1. mkdir -p ~/.android && cp signing/debug.keystore ~/.android/debug.keystore
  2. gradle assembleDebug
  3. APK: app/build/outputs/apk/debug/app-debug.apk

Nama pakej: com.aiisland.app. Jangan ubah, supaya kemas kini boleh dipasang di atas app sedia ada.
