# Index Utilități - Gaz & Curent

Aplicație Android pentru transmiterea automată prin IVR (robot telefonic) a indexului de gaze și curent electric, cu notificări lunare, grafice de consum, estimare a costurilor și rapoarte PDF.

## Cum funcționează

Introduci indexul de pe contor, iar aplicația apelează numărul TelVerde al furnizorului și trimite automat codul de client, indexul și confirmările (tonuri DTMF) către robot. Pauzele dintre taste sunt virgulele din șablonul de apel, de exemplu `0800800200,3730081#,,,XXXX#,,1`, unde `XXXX` este înlocuit cu indexul.

⚠️ Aplicația nu primește confirmare de la robot. După apel afișează doar durata convorbirii (din istoricul apelurilor); verifică totuși că indexul a fost preluat.

## 📱 Cum obținezi APK-ul

### Metoda 1: APK-ul precompilat din depozit
- [`public/index-utilitati.apk`](./public/index-utilitati.apk)
- [`release/index-utilitati.apk`](./release/index-utilitati.apk)

Apasă pe fișier, apoi pe **Download**, și deschide-l pe Android (permite instalarea din surse necunoscute dacă ți se cere). APK-ul din depozit poate fi mai vechi decât codul.

### Metoda 2: Ultima versiune, din GitHub Actions
1. Deschide fila **Actions** și alege ultima rulare `Build Android APK` cu bifă verde.
2. La **Artifacts** descarcă `index-utilitati-apk` (arhivă ZIP cu APK-ul în ea).

Rularea pornește la fiecare push pe `main` sau manual (**Run workflow**).

### Actualizarea aplicației
APK-ul din Actions este semnat cu un keystore de debug generat la fiecare rulare, deci un APK nou nu se poate instala peste unul vechi: Android refuză actualizarea din cauza semnăturii diferite, iar dezinstalarea șterge datele aplicației. Pentru actualizări peste versiunea instalată, adaugă un keystore fix în repo ca `debug.keystore.base64` (workflow-ul îl folosește dacă există).
