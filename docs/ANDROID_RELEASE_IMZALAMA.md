# AVORA Android release imzalama

AVORA, dogrudan dagitilan APK'lar ile gelecekteki Google Play surumlerinde ayni
uygulama imza kimligini kullanir.

## Yerel dosyalar

- Ana keystore: `%USERPROFILE%\AVORA-Signing\avora-app-signing.jks`
- Gradle gizli ayarlari: `%USERPROFILE%\AVORA-Signing\keystore.properties`
- Yerel yedek: `D:\AVORA-Signing-Backup\avora-app-signing.jks`
- Her iki klasor yalniz mevcut Windows kullanicisi ve SYSTEM tarafindan okunur.

Bu dosyalar Git deposunun disindadir. `keystore.properties` parola yoneticisine
veya sifreli, cevrimdisi bir ortama ayrica yedeklenmelidir. Anahtar veya parolalar
sohbete, Git'e, e-postaya ya da uygulama gunluklerine yazilmamalidir.

## Kendi cihazinda internal surum

`internal` yapisi kalici AVORA uygulama anahtariyla imzalanir, ancak Google Play
baglantisi tamamlanana kadar yalniz Firebase'e tek tek kaydedilen test cihazlarinda
App Check erisimi saglar. Bu APK herkese dagitilmaz. `release` yapisi her zaman
Play Integrity kullanir ve internal test saglayicisina geri donmez.

Internal APK ve gelecekteki Play release APK ayni uygulama imza anahtarini
kullandigi icin, daha yuksek `versionCode` degerine sahip Play surumu internal
surumun uzerine yerel verileri silmeden kurulabilir.

`debug` yapisi `com.alidogukan.avora.debug` paket adini ve `AVORA Debug` uygulama
adini kullanir. Android Studio Run islemi bu nedenle telefondaki kalici
`com.alidogukan.avora` internal/release kurulumunu kaldiramaz veya yerel verilerini
silemez. Debug Firebase yapilandirmasi `app/src/debug/google-services.json`
altinda yereldir ve Git tarafindan yok sayilir.

## Google Play gecisi

Ilk Play App Signing kurulumunda Google'in farkli bir uygulama imza anahtari
uretmesi yerine mevcut AVORA uygulama imza anahtarinin sifreli bir kopyasi
aktarilmalidir. Bu, dogrudan yuklenen APK'larin Play surumuyle guncellenebilmesini
saglar. Play kurulumu tamamlandiktan sonra gunluk paket yuklemeleri icin ayri bir
upload key olusturulmalidir.

## Anahtari yeniden olusturma

`Android/tools/configure_release_signing.ps1` mevcut keystore veya gizli ayar
dosyasi varsa islemi durdurur ve hicbir dosyanin uzerine yazmaz. Kalici uygulama
imza anahtari kaybedildiginde ayni kimlikle yeni dogrudan APK guncellemesi
yayinlanamaz.
