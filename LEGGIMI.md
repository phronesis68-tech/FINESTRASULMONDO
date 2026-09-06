# Bottega "Una finestra sul mondo" — progetto Android

Questo non è ancora un file `.apk`: è il progetto da cui l'APK viene costruito.
La costruzione richiede l'SDK Android, circa 3 GB di strumenti che non è
necessario installare sul tuo computer: la fanno gratuitamente i server di
GitHub, e tu scarichi il risultato.

---

## Come ottenere l'APK — 15 minuti, una volta sola

**1. Crea un account su github.com**, se non ne hai già uno. È gratuito.

**2. Crea un archivio nuovo** (il pulsante verde *New* nella tua pagina).
Chiamalo `bottega-guineaction`. Lascialo pure privato: la compilazione
funziona lo stesso.

**3. Carica questa cartella.** Nella pagina dell'archivio appena creato
scegli *uploading an existing file* e trascina dentro **tutto il contenuto**
della cartella `bottega-android`, sottocartelle comprese. Il modo più
comodo è trascinare le cartelle `app` e `.github` insieme ai tre file
`build.gradle.kts`, `settings.gradle.kts` e `gradle.properties`.

> Attenzione alla cartella `.github`: comincia con un punto e su Windows
> potrebbe essere nascosta. Se non la vedi, attiva *Elementi nascosti* nella
> scheda *Visualizza* di Esplora file. Senza quella cartella la compilazione
> non parte.

**4. Aspetta.** Appena il caricamento è finito, GitHub avvia da solo la
compilazione. Vai nella scheda **Actions**: vedrai una riga con un pallino
giallo (in corso) che dopo 3–5 minuti diventa un segno di spunta verde.

**5. Scarica l'APK.** Clicca sulla riga verde, scorri in fondo alla pagina
fino al riquadro **Artifacts** e scarica `bottega-apk`. È un file `.zip`: al
suo interno c'è `bottega-guineaction-AAAAMMGG.apk`.

Se invece del segno verde compare una croce rossa, apri la riga e copia il
testo dell'ultimo passaggio fallito: serve per capire cosa correggere.

---

## Come installarlo sul telefono

1. Copia l'APK sul telefono, o mettilo nella cartella condivisa e scaricalo
   da lì.
2. Toccalo. Android 12 chiede di autorizzare l'installazione da questa
   applicazione (di solito *File* o il browser): è normale per le
   applicazioni che non arrivano dal Play Store. Concedi il permesso una
   volta e torna indietro.
3. Conferma l'installazione. L'icona compare fra le applicazioni.

Nota: la versione compilata così è firmata con la chiave di prova di GitHub.
Va benissimo per i vostri cinque dispositivi. L'unico vincolo è che **tutte
le installazioni successive devono venire dalla stessa origine**: se un
domani generiamo una chiave nostra, sui telefoni bisognerà disinstallare e
reinstallare, perdendo i dati locali — quindi prima si esporta una copia.

---

## Come pubblicare un aggiornamento

1. Modifica quello che serve (di solito solo `app/src/main/assets/index.html`).
2. Alza il numero di versione in `app/build.gradle.kts`: sia `versionCode`
   (un intero che cresce di uno) sia `versionName` (es. da `0.9.0` a `0.9.1`).
3. Carica il file modificato su GitHub: la compilazione riparte da sola.
4. Metti il nuovo APK nella cartella condivisa, insieme al file di versione
   che l'applicazione legge all'avvio.

---

## Che cosa c'è dentro

```
app/src/main/assets/index.html    l'applicazione vera e propria
                                  (lo stesso file provato sul computer,
                                  più la parte 18: memoria del telefono,
                                  salvataggio file, tasto indietro)
app/src/main/java/.../MainActivity.kt
                                  il contenitore Android: apre la pagina,
                                  gestisce il selettore dei file e la
                                  fotocamera
app/src/main/res/                 icona, colori, nome dell'applicazione
app/build.gradle.kts              numero di versione e requisiti
.github/workflows/compila-apk.yml istruzioni per la compilazione automatica
```

Per cambiare qualcosa nell'applicazione si tocca quasi sempre solo
`index.html`. Il Kotlin serve per le tre cose che una pagina web non può
fare da sola sul telefono.

---

## Dove finiscono i dati

Nella memoria privata dell'applicazione, sul telefono. Non nella cartella
condivisa: quella serve solo per i pacchetti di sincronizzazione e le copie
di sicurezza, che si esportano dalla sezione *Dati e sincronizzazione*.

Disinstallare l'applicazione cancella i dati. Prima di disinstallare,
esporta sempre una copia.
