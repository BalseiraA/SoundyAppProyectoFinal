package com.example.soundy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.soundy.model.Usuario

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarUsuario(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios_table WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    suspend fun obtenerUsuarioPorNombre(nombre: String): Usuario?

    @Query("""
        SELECT * FROM usuarios_table
        WHERE LOWER(nombre) = LOWER(:nombre)
        AND password = :password
        LIMIT 1
    """)
    suspend fun login(nombre: String, password: String): Usuario?
}
