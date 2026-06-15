package com.example.soundy.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.soundy.model.BotonSonido

@Dao
interface BotonSonidoDao {

    @Insert
    suspend fun insertarBoton(botonSonido: BotonSonido): Long

    @Query("""
        SELECT * FROM botones_sonido_table
        WHERE usuarioId = :usuarioId
        ORDER BY fechaCreacion DESC
    """)
    suspend fun obtenerBotonesPorUsuario(usuarioId: Int): List<BotonSonido>

    @Delete
    suspend fun eliminarBoton(botonSonido: BotonSonido)
}
