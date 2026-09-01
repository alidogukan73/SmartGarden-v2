# AVORA teknik kimlik ve altyapı geçişi

Durum: Kod ve yeni Firebase altyapısı hazır; Raspberry Pi, ESP32, emülatör ve
telefon saha geçişi tamamlanmadan eski çalışan altyapı geri dönüş için korunur.

## Hedef kimlikler

- Release/internal Android paketi: com.alidogukan.avora
- Debug Android paketi: com.alidogukan.avora.debug
- Firebase proje kimliği: avora-alidogukan
- Realtime Database:
  https://avora-alidogukan-default-rtdb.europe-west1.firebasedatabase.app
- Cihaz kimliği: avora-001
- MQTT konuları: avora/...
- Raspberry Pi servisleri: avora.service ve avora-vision.service
- Raspberry Pi gizli ayar dizini: /etc/avora
- Depo ve çalışma klasörü: AVORA
- Sürüm: 3.0.1 (versionCode 34)

## Tamamlananlar

1. Android paket adı, kaynaklar, temalar ve testler AVORA adına geçirildi.
2. Yeni Firebase projesi ile release ve debug Android uygulamaları oluşturuldu.
3. Anonymous Authentication etkinleştirildi.
4. Realtime Database oluşturuldu ve sahiplik kuralları yayımlandı.
5. Eski cihaz verisi silinmeden devices/avora-001 yoluna kopyalandı; kaynak ve
   hedef dışa aktarımlarının boyutu ve SHA-256 özeti birebir eşleşti.
6. Release uygulamasında Play Integrity; cihaz bütünlüğü zorunlu, Play lisansı
   zorunlu değil ve APK sürümleri desteklenecek biçimde yapılandırıldı.
7. Emülatörün internal Debug App Check anahtarı yeni release Firebase
   uygulamasına kaydedildi.
8. Emülatör kullanıcısına avora_device_id=avora-001 yetkisi verildi ve yeni
   veritabanındaki bölgeleri okuyabildiği doğrulandı.
9. Yeni Firebase Admin anahtarı oluşturuldu; Database ve Auth yönetim erişimi
   doğrulandı ve etkinleştirilmeden Raspberry Pi'ye güvenli biçimde aktarıldı.
10. GitHub deposu AVORA olarak yeniden adlandırıldı; bilgisayar ve Raspberry Pi
    origin adresleri güncellendi.
11. Android birim testi, lint, internal ve release derlemeleri geçti.
12. Firebase kurallarının dört sahiplik ve geri bildirim senaryosu geçti.
13. Raspberry Pi Python kaynaklarının sözdizimi kontrolü geçti.

## Güvenli geçiş sırası

1. Yerel değişiklikleri son kez doğrula; kullanıcı gönder dediğinde main dalına
   kaydet ve gönder.
2. Raspberry Pi'de yeni kodu al, hazırlanmış Admin anahtarını etkinleştir;
   klasör, ortam değişkenleri ve systemd servislerini AVORA
   adlarına geçir.
3. ESP32 aygıt yazılımını yeni avora/... MQTT konularıyla yükle.
4. Telefonun yeni Firebase kullanıcı kimliğine
   avora_device_id=avora-001 yetkisini ver.
5. Internal ve release sürümlerde sensör, sulama, geri bildirim ve Bitki
   Asistanı uçtan uca testlerini tamamla.
6. Başarılı saha testinden sonra Realtime Database App Check zorlamasını aç.
7. Son olarak yerel çalışma klasörünü AVORA olarak yeniden adlandır.

Geçiş tamamlanana kadar eski Firebase projesi, çalışan Raspberry Pi servisi ve
ESP32 aygıt yazılımı silinmez. Böylece her aşamada geri dönüş mümkündür.
