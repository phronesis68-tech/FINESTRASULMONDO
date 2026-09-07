package org.guineaction.bottega

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * L'applicazione della bottega gira dentro una WebView che carica il file
 * assets/index.html — lo stesso prototipo provato sul computer.
 *
 * Il Kotlin serve solo per le tre cose che una pagina web da sola non può fare
 * sul telefono:
 *   1. salvare un file nella cartella condivisa (pacchetti di sincronizzazione,
 *      copie di sicurezza, esportazioni per la contabilità);
 *   2. rileggere un file scelto dall'utente;
 *   3. aprire fotocamera e galleria quando si tocca il campo della fotografia.
 *
 * I dati restano nella memoria della WebView (localStorage), che a differenza
 * di una pagina aperta dal browser è privata dell'applicazione e persiste fra
 * un avvio e l'altro.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    /** File in attesa di essere scritto quando l'utente sceglie dove salvarlo */
    private var contenutoDaSalvare: String? = null

    /** Callback della pagina in attesa del contenuto di un file da leggere */
    private var callbackLettura: String? = null

    /** Callback della WebView per il campo <input type="file"> */
    private var callbackFoto: ValueCallback<Array<Uri>>? = null

    companion object {
        private const val RICHIESTA_SALVA = 101
        private const val RICHIESTA_APRI = 102
        private const val RICHIESTA_FOTO = 103
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(salvato: Bundle?) {
        super.onCreate(salvato)

        web = WebView(this)
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // serve a conservare i dati fra un avvio e l'altro
            allowFileAccess = false           // la pagina sta negli assets, non serve leggere il disco
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            textZoom = 100
        }
        WebView.setWebContentsDebuggingEnabled(false)

        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Ponte(), "Android")

        // Apre fotocamera e galleria quando si tocca il campo della fotografia
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                vista: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                parametri: FileChooserParams?
            ): Boolean {
                callbackFoto?.onReceiveValue(null)
                callbackFoto = callback
                return try {
                    startActivityForResult(
                        parametri?.createIntent() ?: return false, RICHIESTA_FOTO
                    )
                    true
                } catch (e: Exception) {
                    callbackFoto = null
                    avvisa("Non riesco ad aprire la fotocamera.")
                    false
                }
            }
        }

        // Il tasto indietro naviga dentro l'applicazione, non la chiude subito
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(function(){ return tornaIndietro(); })();") { esito ->
                    if (esito == "false") {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        web.loadUrl("file:///android_asset/index.html")
    }

    private fun avvisa(testo: String) =
        Toast.makeText(this, testo, Toast.LENGTH_LONG).show()

    /** Funzioni che la pagina può chiamare come Android.nomeFunzione(...) */
    inner class Ponte {

        /** Chiede all'utente dove salvare e scrive il contenuto lì */
        @JavascriptInterface
        fun salvaFile(nomeProposto: String, contenuto: String, tipo: String) {
            contenutoDaSalvare = contenuto
            val intento = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (tipo.isBlank()) "application/json" else tipo
                putExtra(Intent.EXTRA_TITLE, nomeProposto)
            }
            runOnUiThread { startActivityForResult(intento, RICHIESTA_SALVA) }
        }

        /**
         * Chiede all'utente quale file aprire e restituisce il contenuto
         * chiamando la funzione JavaScript indicata in nomeCallback.
         */
        @JavascriptInterface
        fun apriFile(nomeCallback: String, tipo: String) {
            callbackLettura = nomeCallback
            val intento = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (tipo.isBlank()) "*/*" else tipo
            }
            runOnUiThread { startActivityForResult(intento, RICHIESTA_APRI) }
        }

        /** Segnala che l'applicazione gira dentro l'app e non nel browser */
        @JavascriptInterface
        fun dentroApp(): Boolean = true

        @JavascriptInterface
        fun versioneApp(): String = BuildConfig.VERSION_NAME
    }

    override fun onActivityResult(richiesta: Int, esito: Int, dati: Intent?) {
        super.onActivityResult(richiesta, esito, dati)

        if (richiesta == RICHIESTA_FOTO) {
            callbackFoto?.onReceiveValue(
                if (esito == Activity.RESULT_OK && dati?.data != null) arrayOf(dati.data!!)
                else null
            )
            callbackFoto = null
            return
        }

        if (esito != Activity.RESULT_OK || dati?.data == null) {
            contenutoDaSalvare = null
            callbackLettura = null
            return
        }

        when (richiesta) {
            RICHIESTA_SALVA -> {
                val contenuto = contenutoDaSalvare
                contenutoDaSalvare = null
                if (contenuto == null) return
                try {
                    contentResolver.openOutputStream(dati.data!!, "wt")?.use {
                        it.write(contenuto.toByteArray(Charsets.UTF_8))
                    }
                    avvisa("File salvato.")
                } catch (e: Exception) {
                    avvisa("Non sono riuscito a salvare: ${e.message}")
                }
            }

            RICHIESTA_APRI -> {
                val callback = callbackLettura
                callbackLettura = null
                if (callback == null) return
                try {
                    val testo = contentResolver.openInputStream(dati.data!!)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    // Il contenuto viaggia come stringa JSON per non rompersi
                    // su apici, accenti e a capo.
                    val racchiuso = org.json.JSONObject().put("t", testo).toString()
                    web.evaluateJavascript("$callback(JSON.parse($racchiuso).t);", null)
                } catch (e: Exception) {
                    avvisa("Non sono riuscito a leggere il file: ${e.message}")
                }
            }
        }
    }
}
