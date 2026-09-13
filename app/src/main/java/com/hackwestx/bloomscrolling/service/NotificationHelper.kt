package com.hackwestx.bloomscrolling.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "foreground_service_channel"
    private const val CHANNEL_NAME = "Serviço em segundo plano"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW // sem som/vibração, só ícone na barra
            ).apply {
                description = "Notificação persistente enquanto o serviço está ativo"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, contentText: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Serviço ativo")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // troca pelo ícone real do app depois
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}