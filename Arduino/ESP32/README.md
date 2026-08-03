# SmartGarden ESP32

Bu klasör, kablosuz toprak nem sensörlerini MQTT ile SmartGarden sistemine
gönderen ESP32/Arduino kaynak kodunu içerir.

Ana Arduino projesini bu klasöre, kendi klasörüyle birlikte ekleyin:

```text
Arduino/
  ESP32/
    SmartGardenSensors/
      esp32/
        esp32.ino
        secrets.h
```

Hedef donanım yapısı:

- ADS1115 `0x48`: `soil-001` ile `soil-004`
- ADS1115 `0x49`: `soil-005` ile `soil-008`
- MQTT konuları: `smartgarden/sensors/soil-001` ... `soil-008`

İkinci ADS1115 henüz bağlı değilken ilk dört sensör çalışmaya devam etmeli;
kalan dört kanal bağlantı bekliyor olarak ele alınmalıdır.

## Sensörü uzaktan açma / kapatma

ESP32, aşağıdaki MQTT konusundaki kalıcı (retained) ayarları dinler:

```text
smartgarden/config/esp32/sensors/<sensor_id>
```

Örnekler:

```text
smartgarden/config/esp32/sensors/soil-003  ->  1  (aç)
smartgarden/config/esp32/sensors/soil-003  ->  0  (kapat)
```

Bu bağlantıyı Android uygulaması Firebase üzerinden Raspberry Pi'ye
gönderecek; ESP32 yeniden başlatılsa bile retained ayar tekrar uygulanır.

## Kalibrasyonu uzaktan güncelleme

Kullanıcı Android uygulamasında sensörün kuru ve ıslak ham değerlerini
girdiğinde ESP32 aşağıdaki kalıcı ayarı alır:

```text
smartgarden/config/esp32/calibration/<sensor_id>
```

Mesaj biçimi `kuru,ıslak` şeklindedir:

```text
smartgarden/config/esp32/calibration/soil-003  ->  12480,620
```

ESP32 yalnızca kuru değer ıslak değerden büyük olduğunda kalibrasyonu kabul
eder. Böylece hatalı girişler sulama kararını etkileyemez.
