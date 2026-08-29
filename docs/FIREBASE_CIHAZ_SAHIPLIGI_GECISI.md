# Firebase cihaz sahipliği güvenli geçişi

Bu geçiş, anonim olarak oturum açan her Android istemcisinin bütün AVORA
cihazlarına erişebilmesi riskini kaldırır. Android kullanıcısına Raspberry Pi
hizmet hesabıyla `avora_device_id=smartgarden-001` özel yetkisi verilir.

## Önemli sıra

Yeni güvenlik kurallarını sahiplik yetkisi verilmeden yayımlamayın. Aksi halde
Android uygulaması Firebase erişimini kaybeder. Güvenli sıra şöyledir:

1. Son geri bildirim e-postasındaki `Kullanıcı kimliği` değerini alın.
2. Raspberry Pi üzerinde kullanıcıyı cihaza bağlayın.
3. Android uygulamasının bu değişiklikleri içeren sürümünü kurun.
4. Android uygulamasını tamamen kapatıp yeniden açın.
5. Uygulamanın bölgeleri okuyabildiğini ve bir ayarı kaydedebildiğini doğrulayın.
6. Son olarak yeni Realtime Database kurallarını yayımlayın.

## Raspberry Pi üzerinde sahiplik verme

```bash
cd ~/SmartGarden-v2
git pull --ff-only origin main
cd RaspberryPi
.venv/bin/python tools/configure_device_owner.py --uid 'FIREBASE_KULLANICI_KIMLIGI'
```

Araç, mevcut farklı özel yetkileri korur. Kullanıcı zaten başka bir AVORA
cihazına bağlıysa yanlışlıkla üzerine yazmaz.

Mevcut yetkili kullanıcıları değiştirmeden listelemek için:

```bash
cd ~/SmartGarden-v2/RaspberryPi
.venv/bin/python tools/configure_device_owner.py --list
```

## Kuralları önce emülatörde sınama

Proje kökünde:

```powershell
cd D:\Projects\SmartGarden-v2\firebase-rules-tests
npm.cmd install
npm.cmd test
```

Testler gerçek `smartgarden-v2` veritabanına bağlanmaz; yalnız
`demo-smartgarden-v2` emülatörünü kullanır.

## Kuralları yayımlama

Android sahipliği doğrulandıktan sonra proje kökünde:

```powershell
cd D:\Projects\SmartGarden-v2
npx.cmd --yes firebase-tools@15.28.2 deploy --only database --project smartgarden-v2
```

Bu işlem Cloud Functions veya Blaze planı gerektirmez.

## Telefon değişikliği veya uygulama verilerinin silinmesi

Anonim Firebase kullanıcı kimliği uygulama verileri silindiğinde değişebilir.
Yeni kimliğe sahiplik verdikten sonra eski kimliğin erişimini kaldırın:

```bash
cd ~/SmartGarden-v2/RaspberryPi
.venv/bin/python tools/configure_device_owner.py \
  --uid 'ESKI_FIREBASE_KULLANICI_KIMLIGI' \
  --remove
```

Yeni telefonun kimliği henüz e-postada görünmüyorsa güvenlik kurallarını geçici
olarak gevşetmeyin. Firebase Authentication kullanıcı listesinden yeni anonim
kimliği belirleyin ve Raspberry Pi aracıyla yetkilendirin.
