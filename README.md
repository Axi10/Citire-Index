# Index Utilități - Gaz & Curent

Aplicație Android pentru transmiterea automată, prin robotul telefonic (IVR) al furnizorului, a indexului de gaze și curent electric. Are notificări lunare, grafice de consum, estimare de cost, rapoarte PDF și export CSV.

## Cum funcționează

Introduci indexul de pe contor (sau îl scanezi cu camera), iar aplicația apelează numărul TelVerde al furnizorului și trimite automat codul de client, indexul și confirmările (tonuri DTMF). Pauzele dintre taste sunt virgulele din șablonul de apel, de exemplu `0800800200,12345#,,,XXXX#,,1`, unde `XXXX` este înlocuit cu indexul. Șablonul se poate vedea și modifica din **Setări → Modifică → Setări avansate**.

⚠️ Aplicația nu primește confirmare de la robot. După apel afișează doar durata convorbirii (din istoricul apelurilor): verifică totuși că indexul a fost preluat.

## Prima pornire

1. Deschide **Setări** și apasă **Modifică** la Gaz și la Curent.
2. Completează **Codul de client** (se găsește pe factură). Codul nu este inclus în aplicație, ca să nu fie public în depozit; fără el transmiterea este oprită.
3. Verifică prețul pe unitate și ziua notificării.
4. Folosește **Teste rapide** din Setări: apelul de test sună robotul și se oprește când cere indexul, fără să transmită nimic.

## Istoric și copie de siguranță

- **Raport PDF**: pe perioada aleasă (luna curentă, ultimele 3 luni sau tot istoricul), pe mai multe pagini.
- **Export CSV**: tot istoricul, ca fișier care se deschide în Excel. Fii obișnuit să-l faci înainte de a dezinstala aplicația, pentru că dezinstalarea șterge datele.

## Cum obținezi APK-ul

### Metoda 1: Ultima versiune, din GitHub Actions
1. Deschide fila **Actions** și alege ultima rulare `Build Android APK` cu bifă verde.
2. La **Artifacts** descarcă `index-utilitati-apk` (arhivă ZIP cu APK-ul în ea).

Rularea pornește la fiecare push pe `main` sau manual (**Run workflow**). Fiecare build are numărul versiunii egal cu numărul rulării (`1.1.<număr>`), deci este mai nou decât cel anterior.

### Metoda 2: APK-ul precompilat din depozit
- [`public/index-utilitati.apk`](./public/index-utilitati.apk)
- [`release/index-utilitati.apk`](./release/index-utilitati.apk)

Pot fi mai vechi decât codul. Permite instalarea din surse necunoscute dacă ti se cere.

## Actualizarea peste versiunea instalată

Android instalează un APK peste aplicația existentă doar dacă are **aceeași semnătură**. Ca APK-urile din Actions să aibă mereu aceeași semnătură, workflow-ul citește o cheie din secretul `DEBUG_KEYSTORE_BASE64` al depozitului (Settings → Secrets and variables → Actions). Fără secret, fiecare build își generează o cheie temporară și nu se mai poate instala peste versiunea veche (trebuie dezinstalată mai întâi, ceea ce șterge datele: faţă întâi exportul CSV).

Cheia nu se pune niciodată în depozit: este un secret, și depozitul este public.

## Teste

Testele unitare (șablonul de apel, statusul de transmitere, evaluarea apelului) rulează în workflow după build, sau local cu:

```bash
./gradlew testDebugUnitTest --tests "com.example.unit.*"
```
