# Airsoft Detonacija — Android 13+

Nativni Android Studio projekt (Kotlin), namijenjen za offline upotrebu.

## Izrada APK-a
1. Instalirajte Android Studio (JDK 17 i Android SDK 35).
2. Otvorite folder `AirsoftDetonacija` kroz **Open**.
3. Sačekajte Gradle Sync i instalirajte ponuđene SDK komponente.
4. Izaberite **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK je u `app/build/outputs/apk/debug/app-debug.apk`.
6. Prenesite APK na telefon i dopustite instalaciju iz tog izvora.

## Funkcije
- 20 minuta za postavljanje, potom 15 minuta do simulirane detonacije.
- Pet šifri: 2846, 7193, 4528, 9361, 6052; nasumičan redoslijed bez ponavljanja dok se ne iscrpe sve.
- Kazna 30 sekundi nakon pogrešnog unosa.
- Trajna Android obavijest i foreground service za rad dok je ekran ugašen.
- Vrijeme završetka i status se čuvaju u SharedPreferences; nakon ponovnog pokretanja se računa proteklo vrijeme.
- Ponavljajući alarm za istek postavljanja i zvuk simulirane eksplozije za istek aktivnog tajmera.
- Full-screen immersive mode, potvrda izlaska, potvrda prekida aktivne runde.

## Ograničenja i testiranje
- Ovaj projekt nije kompajliran ni testiran na fizičkom Android uređaju u ovom okruženju.
- Android 13+ zahtijeva dozvolu za obavijesti; omogućite je i isključite restrikcije baterije za ovu aplikaciju.
- Android može zaustaviti uslugu ako je aplikacija prisilno zaustavljena ili je uređaj restartovan. Nakon ponovnog otvaranja stanje i preostalo vrijeme se obnavljaju, ali alarm ne može garantovano svirati dok je aplikacija prisilno zaustavljena.
- Sistemsku jačinu zvuka podesite ručno; testirajte dalje od ušiju.
- Ako želite spriječiti slučajan izlazak, uključite **App Pinning / Prikvači aplikaciju** u Android postavkama.
- Nakon ponovnog pokretanja telefona servis se ne pokreće automatski; otvorite aplikaciju za obnovu runde.

## APK bez Android Studija (GitHub Actions)
1. Napravite besplatan GitHub račun na https://github.com.
2. Otvorite https://github.com/new i napravite novi **privatni** repozitorij `AirsoftDetonacija` (bez automatskog README-a).
3. Na računaru raspakujte ZIP. U novom repozitoriju kliknite **uploading an existing file** ili **Add file > Upload files**. Prevucite **sadržaj** foldera `AirsoftDetonacija` (uključujući skriveni `.github` folder), tako da `settings.gradle.kts` bude u korijenu repozitorija. Potvrdite **Commit changes**.
4. Otvorite karticu **Actions** i odaberite **Napravi Android APK**. Kliknite **Run workflow** ako se izrada nije automatski pokrenula.
5. Kada se izvršavanje završi zelenom oznakom, otvorite taj run i pod **Artifacts** preuzmite `AirsoftDetonacija-APK`. Raspakujte preuzeti ZIP i pronađite `app-debug.apk`.
6. Prebacite APK na Android i instalirajte. Telefon će možda tražiti dozvolu **Install unknown apps / Instaliraj nepoznate aplikacije** za aplikaciju iz koje otvarate APK.

**Napomena:** GitHub automatski kompajlira projekt, ali ovaj izvorni kod nije prethodno testiran na fizičkom uređaju. Ako build ne uspije, otvorite crveni run i kopirajte poruku greške iz koraka `Kompajliraj APK` radi ispravke. Debug APK je namijenjen za lično testiranje, a ne za objavu u Play Storeu.
