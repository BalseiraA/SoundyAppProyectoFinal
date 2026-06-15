package com.example.soundy.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.soundy.converters.Converters
import com.example.soundy.dao.BotonSonidoDao
import com.example.soundy.dao.UsuarioDao
import com.example.soundy.model.BotonSonido
import com.example.soundy.model.Usuario

@Database(
    entities = [Usuario::class, BotonSonido::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun botonSonidoDao(): BotonSonidoDao
}
