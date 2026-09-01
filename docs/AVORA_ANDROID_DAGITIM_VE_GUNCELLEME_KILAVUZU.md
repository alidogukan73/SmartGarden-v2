# AVORA Android dağıtım ve güncelleme kılavuzu

Bu kılavuz AVORA'nın geliştirme sırasında iki ayrı cihazda güvenli biçimde
kullanılması için hazırlanmıştır:

- Android Studio emülatörü: `internal`
- Samsung telefon: `release`
- Telefon güncellemeleri: Firebase App Distribution
- Cihaz ve uygulama imzası kaydı: Android Developer Console

Bu düzende telefondaki AVORA kaldırılmaz. Yeni sürümler aynı paket adı ve aynı
uygulama imza anahtarıyla mevcut uygulamanın üzerine kurulur.

## 1. Değişmeyen uygulama kimlikleri

- Paket adı: `com.alidogukan.avora`
- Firebase uygulaması: `AVORA`
- Firebase uygulama kimliği:
  `1:1067555097897:android:e2f618ead2f5410a807b38`
- Kalıcı imza dosyası:
  `%USERPROFILE%\AVORA-Signing\avora-app-signing.jks`
- İmza ayarları:
  `%USERPROFILE%\AVORA-Signing\keystore.properties`

`internal` ve `release` aynı paket adını ve aynı kalıcı imzayı kullanır. Bu iki
sürüm aynı Android cihazına yan yana kurulamaz. Emülatörde `internal`, telefonda
`release` kullanıldığı için bu durum sorun oluşturmaz.

## 2. Emülatörde internal sürümü çalıştırma

1. Android Studio'da `D:\Projects\AVORA\Android` klasörünü açın.
2. **File > Sync Project with Gradle Files** seçeneğini çalıştırın.
3. **View > Tool Windows > Build Variants** bölümünü açın.
4. `:app` satırındaki **Active Build Variant** değerini `internal` yapın.
5. Kullanılacak emülatörü seçin ve **Run** düğmesine basın.

`internal` sürümü Firebase Debug App Check kullanır. Emülatör ilk kez
oluşturulduğunda veya verileri sıfırlandığında yeni bir Debug App Check anahtarı
üretebilir.

Anahtarı bulmak için Android Studio'da **Logcat** bölümünü açın ve şu sorguyu
kullanın:

```text
tag:DebugAppCheckProvider level:DEBUG
```

Logcat'te görünen UUID biçimindeki anahtar şu uygulamaya kaydedilir:

1. Firebase Console'da `avora-alidogukan` projesini açın.
2. **App Check > Apps > AVORA** yoluna gidin.
3. Üç nokta menüsünden **Manage debug tokens** bölümünü açın.
4. Anahtarı açıklayıcı bir adla ekleyin.

`internal`, `com.alidogukan.avora` paketini kullandığı için anahtar **AVORA**
uygulamasına eklenir. `AVORA Debug` uygulamasına eklenmez.

## 3. Yeni telefon sürümünü hazırlama

Her gerçek telefon güncellemesinde önce sürüm numaraları artırılır. Dosya:

`Android/app/build.gradle.kts`

Örnek:

```kotlin
versionCode = 35
versionName = "3.0.2"
```

Kurallar:

- `versionCode` her telefon güncellemesinde mutlaka önceki değerden büyük olmalıdır.
- `versionName` kullanıcıya gösterilen sürümdür.
- Daha düşük veya aynı `versionCode`, Firebase'de yeni bir güncelleme gibi
  görünmeyebilir.
- Paket adı `com.alidogukan.avora` değiştirilmemelidir.
- Kalıcı release imza anahtarı değiştirilmemelidir.

## 4. İmzalı release APK üretme

### Kolay yöntem: Android Studio

1. **Build Variants** bölümünde `release` seçin.
2. **Build > Generate Signed App Bundle or APK** yolunu açın.
3. **APK** seçip devam edin.
4. Keystore olarak
   `%USERPROFILE%\AVORA-Signing\avora-app-signing.jks` dosyasını seçin.
5. Mevcut AVORA anahtar adını ve güvenli biçimde saklanan parolaları kullanın.
6. Build type olarak `release` seçip APK'yı oluşturun.

### Proje terminaliyle kullandığımız yöntem

PowerShell'de:

```powershell
cd D:\Projects\AVORA\Android
.\gradlew.bat :app:assembleRelease
```

Başarılı olduğunda APK burada oluşur:

```text
D:\Projects\AVORA\Android\app\build\outputs\apk\release\app-release.apk
```

Derleme sonunda `BUILD SUCCESSFUL` görülmelidir. İmza dosyası bulunamazsa veya
imza doğrulaması başarısız olursa APK dağıtılmamalıdır.

## 5. Firebase App Distribution ile telefona gönderme

Android Developer Console APK dosyasını barındırmaz ve telefona güncelleme
göndermez. O bölüm paket adını, imza anahtarını ve yetkili cihazları kaydeder.
Gerçek APK teslimatı Firebase App Distribution üzerinden yapılır.

### Firebase Console ile

1. [Firebase Console](https://console.firebase.google.com/) sayfasını açın.
2. `avora-alidogukan` projesini seçin.
3. Sol menüden **App Distribution** bölümünü açın.
4. Uygulama olarak **AVORA (`com.alidogukan.avora`)** seçili olmalıdır.
5. **Releases** bölümünde yeni release yükleme düğmesine basın.
6. `app-release.apk` dosyasını yükleyin.
7. Test kullanıcısı olarak `alidogukan@gmail.com` hesabını seçin.
8. Yapılan değişiklikleri kısa bir sürüm notu olarak yazın.
9. **Distribute / Dağıt** düğmesine basın.

Yanlışlıkla `internal` veya `debug` APK yüklemeyin. Telefon dağıtımında dosya
adı `app-release.apk`, paket adı `com.alidogukan.avora` olmalıdır.

### Firebase CLI ile

Firebase CLI kurulmuş ve doğru Google hesabıyla oturum açılmışsa proje kökünde
şu komut kullanılabilir:

```powershell
firebase appdistribution:distribute `
  "Android\app\build\outputs\apk\release\app-release.apk" `
  --app "1:1067555097897:android:e2f618ead2f5410a807b38" `
  --testers "alidogukan@gmail.com" `
  --release-notes "AVORA sürüm notu"
```

İşlem sonunda `uploaded`, `added release notes` ve `distributed` adımlarının
başarılı olduğu görülmelidir.

## 6. Telefonda güncellemeyi kurma

1. Telefonda `alidogukan@gmail.com` hesabının Gmail kutusunu açın.
2. Firebase App Distribution bildirimini açın.
3. **Get started / Başlayın** bağlantısına basın.
4. Aynı Google hesabıyla giriş yapın.
5. AVORA'nın yeni sürümünde **Download / İndir** seçeneğine basın.
6. Android yükleme ekranında **Update / Güncelle** seçeneğini kullanın.

Önemli:

- AVORA'yı güncellemeden önce kaldırmayın.
- **Kaldır** yerine her zaman **Güncelle** seçeneğini kullanın.
- Android, aynı paket adı ve aynı imza sayesinde uygulamayı mevcut verilerin
  üzerine günceller.
- Telefonun Firebase kullanıcı kimliği ve yerel uygulama verileri korunur.

İlk kullanımda tarayıcı için **Bilinmeyen uygulamaları yükleme izni** istenebilir.
İzin yalnız APK'yı indirdiğiniz güvenilir uygulamaya verilmeli ve istenirse
yükleme sonrasında tekrar kapatılmalıdır.

## 7. Güncelleme sonrası kontrol listesi

Yeni release açıldıktan sonra şunları kontrol edin:

- AVORA açılıyor ve önceki ayarlar duruyor.
- `avora-001` çevrimiçi görünüyor.
- Sensör verileri geliyor.
- Bölgeler ve bitkiler görünüyor.
- Manuel ve otomatik sulama ayarları korunuyor.
- Bitki Asistanı analiz sonucu üretiyor.
- Geri bildirim gönderilebiliyor.
- **Cihaz yetkilendirmesi gerekli** penceresi görünmüyor.

Yetkilendirme penceresi çıkarsa uygulamayı kaldırmayın. Penceredeki kimliği
kontrol edin ve Raspberry Pi cihaz sahipliği listesiyle karşılaştırın.

## 8. Android Developer Console'un görevi

Android Developer Console'da şu kayıtlar korunur:

- Paket adı: `com.alidogukan.avora`
- Doğrulanmış AVORA imza parmak izi
- Yetkili telefon: Samsung S24 Ultra

Sınırlı dağıtım hesabı en fazla 20 yetkili cihaz içindir. Telefon buradan
kaldırılmadığı sürece uygulama ve imza doğrulaması korunur. Ancak yeni APK yine
Firebase App Distribution veya başka güvenli bir dosya kanalıyla teslim edilir.

## 9. Sık karşılaşılan sorunlar

### "Uygulama yüklenemedi"

- Yeni APK farklı bir anahtarla imzalanmış olabilir.
- `versionCode` önceki sürümden düşük olabilir.
- Yanlış paket veya yanlış build variant kullanılmış olabilir.

Çözüm olarak mevcut uygulamayı hemen kaldırmayın. Önce APK'nın `release` olduğu,
paket adının ve imza anahtarının değişmediği doğrulanmalıdır.

### Firebase bağlantı hatası yalnız emülatörde

- Emülatörde `internal` seçildiğini doğrulayın.
- Logcat'teki güncel Debug App Check anahtarını Firebase'deki **AVORA**
  uygulamasına kaydedin.
- Emülatör verilerini gereksiz yere sıfırlamayın; sıfırlama yeni Firebase kullanıcı
  kimliği ve yeni Debug App Check anahtarı oluşturabilir.

### Firebase bağlantı hatası yalnız telefonda

- Telefona `release` APK kurulduğunu doğrulayın.
- İnternet ve Tailscale bağlantısını kontrol edin.
- Firebase App Check ve Play Integrity ayarlarını değiştirmeden önce uygulamanın
  sürümünü ve cihaz yetkilendirme kimliğini kontrol edin.

### Yeni sürüm görünmüyor

- `versionCode` artırılmış olmalıdır.
- Firebase App Distribution'da doğru uygulama ve doğru test kullanıcısı seçilmiş
  olmalıdır.
- Telefonda App Distribution sayfası `alidogukan@gmail.com` hesabıyla açılmalıdır.

## 10. Güvenlik kuralları

- Keystore ve `keystore.properties` Git'e eklenmez.
- Keystore parolaları sohbete, ekran görüntüsüne veya günlük dosyasına yazılmaz.
- Firebase Debug App Check anahtarları yalnız geliştirme cihazlarında kullanılır.
- Telefon release sürümünde Debug App Check anahtarı kullanılmaz; Play Integrity
  kullanılır.
- Release APK yalnız güvenilir Firebase projesine ve yetkili test hesabına
  gönderilir.
- Her dağıtımdan önce çalışan imza yedeğinin varlığı korunur.

## 11. Mevcut geçiş kaydı

1 Eylül 2026 tarihinde AVORA 3.0.1 (versionCode 34) için imzalı release ve
internal APK'lar başarıyla üretildi. Yeni avora-alidogukan Firebase projesi,
uygulama kimliği, Auth, Realtime Database ve App Check yapılandırması hazırlandı.

Yeni Firebase projesindeki ilk telefon dağıtımı, Raspberry Pi ve ESP32 saha
geçişi tamamlandıktan sonra yapılacaktır. Sonraki gerçek telefon güncellemesi en
az versionCode 35 olmalıdır.

## Resmî kaynaklar

- [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)
- [APK'yı Firebase Console ile dağıtma](https://firebase.google.com/docs/app-distribution/android/distribute-console)
- [APK'yı Firebase CLI ile dağıtma](https://firebase.google.com/docs/app-distribution/android/distribute-cli)
- [Android sınırlı cihaz dağıtımı](https://developer.android.com/developer-verification/guides/limited-distribution)
