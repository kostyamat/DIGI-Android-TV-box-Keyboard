package com.kostyamat.r2r_q

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import java.io.File
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AdbHelper {
    
    private val base64Impl = object : AdbBase64 {
        override fun encodeToString(data: ByteArray): String {
            return Base64.encodeToString(data, Base64.NO_WRAP)
        }
    }

    private fun getCrypto(context: Context): AdbCrypto {
        val pubKeyFile = File(context.filesDir, "pub.key")
        val privKeyFile = File(context.filesDir, "priv.key")
        
        return if (pubKeyFile.exists() && privKeyFile.exists()) {
            AdbCrypto.loadAdbKeyPair(base64Impl, privKeyFile, pubKeyFile)
        } else {
            val crypto = AdbCrypto.generateAdbKeyPair(base64Impl)
            crypto.saveAdbKeyPair(privKeyFile, pubKeyFile)
            crypto
        }
    }

    suspend fun autoGrantPermissions(context: Context): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        var connection: AdbConnection? = null
        try {
            val crypto = getCrypto(context)
            socket = Socket("127.0.0.1", 5555)
            socket.soTimeout = 5000
            
            connection = AdbConnection.create(socket, crypto)
            connection.connect()

            // Execute the commands
            executeCommand(connection, "pm grant com.kostyamat.r2r_q android.permission.WRITE_SECURE_SETTINGS")
            executeCommand(connection, "appops set com.kostyamat.r2r_q ACCESS_RESTRICTED_SETTINGS allow")

            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        } finally {
            try { connection?.close() } catch (e: Exception) {}
            try { socket?.close() } catch (e: Exception) {}
        }
    }

    private fun executeCommand(connection: AdbConnection, command: String) {
        var stream: AdbStream? = null
        try {
            stream = connection.open("shell:$command")
            // Read response until closed
            while (!stream.isClosed) {
                val response = stream.read()
                // We just read to flush the buffer
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { stream?.close() } catch (e: Exception) {}
        }
    }
}
