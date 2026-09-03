# AVORA ağ yapılandırma yardımcısı

Android uygulaması doğrudan sistem komutu çalıştırmaz. Firebase'e sınırlı bir IPv4 isteği
yazar; AVORA Raspberry Pi hizmeti isteği doğrular ve bu root-owned yardımcıyı çağırır.

## Kurulum

Raspberry Pi üzerinde AVORA deposu güncellendikten sonra:

```sh
cd /home/ali/AVORA/RaspberryPi
sudo ./deploy/install-network-helper.sh
sudo systemctl restart avora.service
```

Yardımcı yalnız etkin NetworkManager profilinin IPv4 yöntemini, adresini, ağ geçidini ve
DNS alanlarını değiştirebilir. Shell metni kabul etmez. Değişiklikten önce mevcut profil
`/var/lib/avora/network` altında root-only olarak saklanır.

Yeni bağlantı 90 saniye içinde Firebase'e erişemezse yerel systemd zamanlayıcısı eski
profili otomatik yükler. Başarılı doğrulamada AVORA hizmeti geri dönüş zamanlayıcısını
iptal eder ve geçici yedeği siler.

Ana AVORA hizmeti `ali` kullanıcısıyla çalışmaya devam eder. Verilen sudo yetkisi yalnız
`/usr/local/sbin/avora-network-config` programıyla sınırlıdır.
