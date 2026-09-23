package com.example.telecom

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

object CallHelper {

    /**
     * Answers the user's technical question regarding automated calls on Android:
     * - ACTION_CALL initiates the call IMMEDIATELY without pressing the call button in dialer.
     * - Commas (,) in dial strings are pauses that Android's modem automatically sends as DTMF tones.
     * - Android security requires the system In-Call screen to be visible during a live cellular call;
     *   no third-party app is allowed to hide cellular calls into a fully silent background thread.
     */
    const val CALL_EXPLANATION = """
Cum funcționează apelul automat pe Android:
1. Apelare Directă (ACTION_CALL): Cu permisiunea de apelare aprobată, aplicația inițiază apelul automat, fără să mai fie nevoie să apeși butonul verde 'Suna'!
2. Transmiterea automată a tastelor (Virgulele ','): Fiecare virgulă adaugă o pauză de 2-3 secunde. Telefonul tău va transmite automat codul de client, indexul contorului și confirmările DTMF direct robotului telefonic (IVR).
3. Poate rula în fundal complet? Din motive stricte de securitate și confidențialitate Android OS, ecranul nativ de convorbire va fi afișat în prim-plan în timp ce se vorbește, dar secvența se transmite singură fără intervenția ta manuală!
"""

    /**
     * Checks if CALL_PHONE permission is granted.
     */
    fun hasCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Directly places the phone call without dialer confirmation using Intent.ACTION_CALL.
     * Notice Uri.encode is required so that special DTMF characters like '#' and ',' are preserved.
     */
    fun makeDirectCall(context: Context, sequence: String): Boolean {
        return try {
            if (!hasCallPermission(context)) {
                // Fallback to opening dialer if permission is missing
                openInDialer(context, sequence)
                return false
            }

            val encodedSequence = Uri.encode(sequence)
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$encodedSequence")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to dialer if direct call fails
            openInDialer(context, sequence)
            false
        }
    }

    /**
     * Opens the system dialer with the sequence pre-filled (ACTION_DIAL).
     * Does not require CALL_PHONE permission.
     */
    fun openInDialer(context: Context, sequence: String) {
        try {
            val encodedSequence = Uri.encode(sequence)
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$encodedSequence")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Nu s-a putut deschide tastatura telefonului", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the full sequence to clipboard for manual pasting if desired.
     */
    fun copyToClipboard(context: Context, sequence: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Secvență IVR Index", sequence)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Secvența copiată în clipboard!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
