package com.refreshme.stylist

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshme.ui.theme.RefreshMeTheme

class NfcWriterActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var profileUrl: String = ""

    private var nfcStatus by mutableStateOf("Ready to scan")
    private var isWriting by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uid = intent.getStringExtra(EXTRA_UID) ?: return finish()
        profileUrl = "https://refreshme-74f79.web.app/stylist/$uid"

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            nfcStatus = "NFC is not supported on this device."
        } else if (!nfcAdapter!!.isEnabled) {
            nfcStatus = "NFC is disabled. Please enable it in Settings."
        }

        // Setup Foreground Dispatch for NFC scanning
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        setContent {
            RefreshMeTheme {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Write to NFC Card") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(if (isWriting) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Contactless,
                                contentDescription = "NFC",
                                modifier = Modifier.size(60.dp),
                                tint = if (isWriting) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Tap your NFC card or tag to the back of your phone",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "When someone taps this tag, they will be taken directly to your RefreshMe booking profile.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = nfcStatus,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null && nfcAdapter!!.isEnabled) {
            val intentFiltersArray = arrayOf(
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            )
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null)
            nfcStatus = "Ready to scan. Tap tag to phone."
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || 
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED || 
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                writeToTag(tag)
            }
        }
    }

    private fun writeToTag(tag: Tag) {
        isWriting = true
        nfcStatus = "Writing to tag..."
        
        try {
            val uriRecord = NdefRecord.createUri(profileUrl)
            val ndefMessage = NdefMessage(arrayOf(uriRecord))

            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    nfcStatus = "Error: Tag is read-only."
                    isWriting = false
                    return
                }
                if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    nfcStatus = "Error: Tag capacity is too small."
                    isWriting = false
                    return
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                nfcStatus = "Success! Profile linked to tag."
                Toast.makeText(this, "NFC Tag written successfully!", Toast.LENGTH_LONG).show()
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    nfcStatus = "Success! Tag formatted and linked."
                    Toast.makeText(this, "NFC Tag written successfully!", Toast.LENGTH_LONG).show()
                } else {
                    nfcStatus = "Error: Tag does not support NDEF."
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            nfcStatus = "Failed to write to tag."
        }
        
        isWriting = false
    }

    companion object {
        private const val EXTRA_UID = "extra_uid"
        
        fun newIntent(context: Context, stylistUid: String): Intent {
            return Intent(context, NfcWriterActivity::class.java).apply {
                putExtra(EXTRA_UID, stylistUid)
            }
        }
    }
}