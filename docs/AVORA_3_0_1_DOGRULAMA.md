# AVORA 3.0.1 doğrulama kaydı

Tarih: 31 Ağustos 2026

## Sürüm kimliği

- Uygulama kimliği: `com.alidogukan.avora`
- Sürüm: `3.0.1`
- Sürüm kodu: `34`
- Debug kimliği: `com.alidogukan.avora.debug`
- Compile/target SDK: `36.1` / `36`

## Kod ve lint temizliği

- Android lint raporu `245` uyarıdan `0` uyarıya indirildi.
- Küçük metin boyutları erişilebilir alt sınır olan `11sp` değerine yükseltildi.
- Tema ile aynı rengi iki kez çizen Activity kök arka planları kaldırıldı.
- RTL boşluk eşleri, yatay düzen baseline ayarları ve AppCompat drawable kullanımları düzeltildi.
- Yoğunluktan bağımsız üç PNG `drawable-nodpi` altına taşındı.
- Bildirim küçük ikonu Android bildirim kurallarına uygun tek renkli vektöre geçirildi.
- Java derlemesinde `deprecation` ve `unchecked` uyarıları görünür hâle getirildi.
- Firebase 25.1.2'nin eski token API'si yalnız Raspberry Pi FCM gönderim sözleşmesi için iki dar kapsamlı açıklamayla korundu. FID geçişi Android ve Raspberry Pi birlikte değiştirildiğinde yapılmalıdır.
- Birleşik süre, yüzde, `x/y` ve çok sayaçlı metin şablonlarındaki `PluralsCandidate` önerileri kaynak bazında belgelendi; çalışma zamanı metinleri değiştirilmedi.
- Activity düzenlerinde geçersiz `<merge>` önerileri ve tasarım gereği korunmuş iki bileşik görünüm yalnız ilgili köklerde belgelendi.
- SDK 37 yerel kontrollü derleme ortamında bulunmadığı için SDK yükseltme uyarısı mesaj/dosya düzeyinde ertelendi.

## Otomatik doğrulama

- Android birim testleri: `152/152` başarılı.
- Pixel 7 Pro Android 17 / API 37 emülatör cihaz testi: `1/1` başarılı.
- Android lint: `0` hata, `0` uyarı.
- Debug APK, internal APK, imzalı release APK ve imzalı release AAB başarıyla üretildi.
- Firebase Realtime Database sahiplik ve geri bildirim kuralları: `4/4` başarılı.
- Raspberry Pi Python kaynak derlemesi başarılı.
- Firebase bağımlılığı gerektirmeyen Raspberry Pi sulama/AI test betikleri: `10/10` başarılı.

Windows doğrulama ortamında `firebase_admin` paketi bulunmadığından Raspberry Pi'nin Firebase'e bağlı test betikleri yeniden çalıştırılmadı. Bu sürümde Raspberry Pi kaynak kodu değiştirilmedi.

## Paket ve imza doğrulaması

- Internal APK: `com.alidogukan.avora`, `3.0.1-internal`, sürüm kodu `34`.
- Release APK: `com.alidogukan.avora`, `3.0.1`, sürüm kodu `34`.
- Internal ve release APK imza SHA-256 parmak izi:
  `9E:61:84:9E:3E:35:32:91:2F:FA:C2:9A:9E:9F:A5:EC:B8:4B:0D:21:67:D2:4B:BA:7A:54:3C:5D:C1:91:39:73`
- Release AAB imza doğrulaması başarılı; sertifika bitiş tarihi 15 Ocak 2054.

## Dağıtım dosyaları

- Internal APK SHA-256:
  `5B782B2CE0D2287176FCCF87742C7D0B86C94FE24B5058E9BFF033D9F7FD6E24`
- Release APK SHA-256:
  `0BA0DC6FEAE74AFBE3ADA9FD9F8E812C0D29F27B9E615CF64AFF0F609625EBA6`
- Release AAB SHA-256:
  `372472F4D3FD6103187E5711193B1A3F42179DA29DE104176F24EC0830D75858`

Dosyalar `Android/app/build/outputs/` altında üretilir ve Git'e eklenmez.
