package com.cantina.pagamentos

import android.app.Application
import com.google.firebase.FirebaseApp

class CantinaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        println("🔥 [CantinaApplication] onCreate iniciado")
        
        // Inicializa o Firebase
        try {
            FirebaseApp.initializeApp(this)
            println("🔥 [CantinaApplication] Firebase inicializado com sucesso")
        } catch (e: Exception) {
            println("🔥 [CantinaApplication] ERRO ao inicializar Firebase: ${e.message}")
            e.printStackTrace()
        }
    }
} 
