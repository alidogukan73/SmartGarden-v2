# Raspberry Pi geri bildirim e-posta kurulumu

Android uygulaması geri bildirimleri Realtime Database altında
`devices/smartgarden-001/user_feedback` yoluna kaydeder. Gönderen ve alıcı
aynı Gmail hesabıysa Raspberry Pi, iletiyi TLS korumalı Gmail IMAP
bağlantısıyla doğrudan `INBOX` posta kutusuna ekler. Farklı bir alıcı
kullanılırsa güvenli Gmail SMTP teslimine otomatik geçer.

Bu yöntem Firebase Blaze planı veya Cloud Functions gerektirmez.

## Güvenlik ve teslim davranışı

- Gmail ana hesap parolası kullanılmaz; yalnız AVORA için oluşturulan
  16 karakterli Google uygulama şifresi kullanılır.
- Aynı Gmail hesabına teslimde `gmail_inbox`, farklı hesaba teslimde
  `smtp` modu otomatik seçilir.
- Uygulama şifresi `/etc/smartgarden/feedback-email.env` dosyasında
  yalnız root tarafından okunabilecek `0600` izniyle tutulur.
- Şifre Git deposuna, Firebase'e ve uygulama günlüklerine yazılmaz.
- Firebase işlemi her kaydı göndermeden önce atomik olarak kilitler.
- Başarılı teslim `email_delivery/status=sent` olarak kaydedilir.
- Geçici ağ veya Gmail hataları artan bekleme süresiyle yeniden denenir.
- İlk kurulumdan önceki deneme geri bildirimleri topluca gönderilmez.
  Kurulumdan sonra Pi kapalıyken oluşan kayıtlar açılınca gönderilir.

## Raspberry Pi üzerinde bir kerelik kurulum

Depo güncellendikten sonra:

```bash
cd ~/SmartGarden-v2/RaspberryPi
sudo .venv/bin/python tools/configure_feedback_email.py
```

Araç `AVORA Gmail uygulama şifresi:` diye sorunca 16 karakterli uygulama
şifresini yapıştırın. Giriş ekranda görünmez.

Güncel servis tanımını yükleyip AVORA'yı yeniden başlatın:

```bash
sudo cp deploy/smartgarden.service /etc/systemd/system/smartgarden.service
sudo systemctl daemon-reload
sudo systemctl restart smartgarden
systemctl status smartgarden --no-pager
```

Şifrenin kendisini göstermeden yapılandırmayı doğrulayın:

```bash
sudo test -s /etc/smartgarden/feedback-email.env
sudo stat -c '%a %U %G' /etc/smartgarden/feedback-email.env
sudo journalctl -u smartgarden -n 100 --no-pager | grep 'Feedback email'
```

Dosya izin çıktısı `600 root root` olmalıdır. Günlükte
`Feedback email delivery started.` ve ilk çalışmada
`Feedback email activation loaded.` mesajları görülmelidir.

## Uçtan uca deneme

Android uygulamasında **Ayarlar > Geri Bildirim Gönder** ekranından yeni bir
deneme kaydı gönderin. En geç yaklaşık 15 saniye sonra:

1. E-posta `alidogukan@gmail.com` gelen kutusuna ulaşmalıdır.
2. Firebase kaydında `email_delivery/status` değeri `sent` olmalıdır.
3. Pi günlüğünde `Feedback email delivered` satırı görünmelidir.

E-posta görünmezse:

```bash
sudo journalctl -u smartgarden -n 150 --no-pager
```

`configuration is invalid` mesajı varsa yapılandırma aracını yeniden
çalıştırın. Gmail kimlik doğrulama hatasında AVORA uygulama şifresini iptal
edip yenisini oluşturun; normal Gmail parolasını hiçbir zaman kullanmayın.
