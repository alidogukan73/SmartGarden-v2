# AVORA 3.0.0 doğrulama kaydı

Tarih: 2026-08-31

## Tamamlanan kontroller

- Android release/internal paket adı: `com.alidogukan.avora`
- Android debug paket adı: `com.alidogukan.avora.debug`
- Sürüm: `3.0.0` (`versionCode 33`)
- Uygulama etiketi: `AVORA`
- Java kaynak, test ve varyant klasörleri yeni paket yoluna taşındı.
- Eski paket adına ait kod ve XML referansları temizlendi.
- Release ve debug Firebase Android uygulamaları oluşturuldu.
- Release imza parmak izi Firebase ve Android Developer Console üzerinde doğrulandı.
- Release uygulaması için Play Integrity App Check yapılandırıldı.
- Debug uygulaması için emülatör App Check anahtarı kaydedildi.
- Realtime Database App Check zorlaması açık olarak doğrulandı.
- Emülatör Firebase kullanıcısı AVORA cihazına yetkilendirildi.
- Birim testleri, Android cihaz testi ve lint görevi başarıyla tamamlandı.
- Debug, internal ve release APK ile release AAB başarıyla üretildi.
- Emülatörde App Check, Firebase yetkisi ve uygulama çökme sayısı sıfırlandı.
- Ana ekranda gerçek `smartgarden-001` cihazı `BAĞLI` olarak görüntülendi.

## Bilinçli olarak ertelenen kontrol

Kullanıcının isteğiyle fiziksel telefon USB üzerinden bağlanmadı. Bu nedenle
release APK'nın gerçek telefondaki Play Integrity doğrulaması ve fiziksel saha
testi bu kayıt kapsamında yapılmadı. APK telefona daha sonra elle kurulabilir;
yeni paket eski `com.ali.smartgarden` uygulamasının yanında ayrı uygulama olarak
çalışır.

## Korunan çalışan protokol kimlikleri

Aşağıdaki adlar Android paketi veya kullanıcıya görünen marka değildir ve mevcut
Raspberry Pi/Arduino bağlantısını bozmamak için değiştirilmemiştir:

- Firebase proje kimliği: `smartgarden-v2`
- Cihaz kimliği: `smartgarden-001`
- MQTT konuları: `smartgarden/...`
- Raspberry Pi servis adları

`google-services.json` ve imzalama dosyaları Git'e eklenmez. Yerel tam yedek ve
ayrı imzalama yedeği korunmalıdır.
