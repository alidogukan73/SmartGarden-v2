# AVORA Teknik Kimlik Geçişi — Aşama 5

Durum: Bilinçli olarak beklemede.

Uygulamanın kullanıcıya görünen markası AVORA olarak güncellendi. Aşağıdaki teknik
kimlikler, çalışan sistemi riske atmamak için şimdilik korunur:

- Android paket adı: `com.ali.smartgarden`
- Firebase projesi ve Realtime Database adresi
- Cihaz kimliği: `smartgarden-001`
- MQTT konuları: `smartgarden/...`
- Raspberry Pi servis adları: `smartgarden.service`, `smartgarden-vision.service`
- Depo ve proje klasörü adları

## Ne zaman yapılacak?

AVORA sürümü birkaç gün kararlı kullanıldıktan ve güncel bir Git yedeği alındıktan
sonra, planlı bir bakım çalışmasında.

## Güvenli geçiş sırası

1. Android, Raspberry Pi ve Firebase verilerinin yedeğini al.
2. Yeni teknik adları ve geçiş kapsamını kesinleştir.
3. Firebase yapılandırması ve güvenlik kurallarını taşı.
4. Raspberry Pi servisleri, MQTT konuları ve cihaz kimliğini birlikte güncelle.
5. Android paket kimliği ile yapılandırma dosyalarını güncelle; yeni APK üret.
6. Sensör, vana, sulama, hava durumu ve Bitki Doktoru için uçtan uca test yap.
7. Eski teknik kimlikleri ancak geçiş doğrulandıktan sonra kaldır.

Not: Bu çalışma yalnızca isim değişikliği değildir; veri ve cihaz bağlantılarını
etkileyen ayrı bir bakım sürümüdür.
