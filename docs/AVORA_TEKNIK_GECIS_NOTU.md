# AVORA Teknik Kimlik Geçişi — Android

Durum: Android paket geçişi tamamlandı; saha doğrulaması sürüyor.

Uygulamanın kullanıcıya görünen markası ve Android teknik kimliği AVORA olarak
güncellendi:

- Release/internal paket adı: `com.alidogukan.avora`
- Debug paket adı: `com.alidogukan.avora.debug`
- Firebase Android uygulamaları yeni paket adlarıyla aynı `smartgarden-v2`
  projesinde oluşturuldu.
- Sürüm: `3.0.0` (`versionCode 33`)

Mevcut Raspberry Pi, Arduino ve Firebase verileriyle bağlantıyı bozmamak için
aşağıdaki çalışan protokol kimlikleri bilinçli olarak korunur:

- Firebase proje kimliği ve Realtime Database adresi
- Cihaz kimliği: `smartgarden-001`
- MQTT konuları: `smartgarden/...`
- Raspberry Pi servis adları: `smartgarden.service`, `smartgarden-vision.service`
- Depo ve proje klasörü adları

Bu adlar kullanıcıya görünen Android markası veya paket adı değildir. Bunları
yalnız isim benzerliği nedeniyle değiştirmek sensör, vana ve backend bağlantılarını
gereksiz yere keser.

## Tamamlanan geçiş

1. `com.ali.smartgarden` kaynak paketleri ve klasörleri
   `com.alidogukan.avora` altına taşındı.
2. Gradle namespace, applicationId, özel XML view referansları ve test kaynakları
   yeni paket adına geçirildi.
3. Release ve debug için ayrı Firebase Android uygulamaları oluşturuldu.
4. Kalıcı AVORA imza sertifikası release Firebase uygulamasına eklendi.
5. Debug sertifikası debug Firebase uygulamasına eklendi.
6. Birim testleri, lint, debug/internal APK, release APK ve AAB derlemeleri
   doğrulandı.

## Kalan doğrulama

1. Yeni Firebase uygulamalarında App Check sağlayıcılarını kaydet.
2. Yeni paketi telefona ve emülatöre yan yana kur.
3. Oluşan yeni Firebase kullanıcı kimliklerini AVORA cihazının yetki listesine ekle.
4. Sensör, vana, sulama, bildirim, geri bildirim ve Bitki Asistanı saha testlerini
   tamamla.
5. Çalışan sürümün son yedeğini, Git kaydını ve sürüm etiketini oluştur.

Eski Firebase Android uygulamaları saha doğrulaması tamamlanana kadar geri dönüş
seçeneği olarak korunur.
