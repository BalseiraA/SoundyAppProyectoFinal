package com.example.soundy

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundy.databinding.ActivitySoundBoardBinding
import com.example.soundy.model.BotonSonido
import com.example.soundy.storage.AudioFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SoundBoardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USUARIO_ID = "EXTRA_USUARIO_ID"
        const val EXTRA_USUARIO_NOMBRE = "EXTRA_USUARIO_NOMBRE"
    }

    private lateinit var binding: ActivitySoundBoardBinding
    private var usuarioId: Int = -1
    private var usuarioNombre: String = ""
    private var editOptionsVisible = false
    private var editMode = false
    private var botones: List<BotonSonido> = emptyList()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySoundBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, -1)
        usuarioNombre = intent.getStringExtra(EXTRA_USUARIO_NOMBRE).orEmpty()

        if (usuarioId == -1) {
            Toast.makeText(this, getString(R.string.msg_user_not_received), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvwWelcome.text = getString(R.string.welcome_user, usuarioNombre)

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnEditButtons.setOnClickListener {
            if (editMode) {
                exitEditMode()
            } else {
                toggleEditOptions()
            }
        }

        binding.btnAddButton.setOnClickListener {
            closeEditOptionsMenu()

            val intent = Intent(this, AddButtonActivity::class.java).apply {
                putExtra(AddButtonActivity.EXTRA_USUARIO_ID, usuarioId)
            }

            startActivity(intent)
        }

        binding.btnEnterEditMode.setOnClickListener {
            enterEditMode()
        }
    }

    override fun onResume() {
        super.onResume()
        loadButtons()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun logout() {
        releasePlayer()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun toggleEditOptions() {
        editOptionsVisible = !editOptionsVisible

        binding.layoutEditOptions.visibility =
            if (editOptionsVisible) View.VISIBLE else View.GONE

        binding.btnEditButtons.text =
            if (editOptionsVisible) getString(R.string.delete_cross)
            else getString(R.string.btn_edit_buttons)
    }

    private fun enterEditMode() {
        editMode = true
        editOptionsVisible = false
        binding.layoutEditOptions.visibility = View.GONE
        binding.btnEditButtons.text = getString(R.string.btn_cancel_edit)
        renderButtons()
    }

    private fun exitEditMode() {
        editMode = false
        binding.btnEditButtons.text = getString(R.string.btn_edit_buttons)
        renderButtons()
    }

    private fun closeEditOptionsMenu() {
        editOptionsVisible = false
        binding.layoutEditOptions.visibility = View.GONE

        if (!editMode) {
            binding.btnEditButtons.text = getString(R.string.btn_edit_buttons)
        }
    }

    private fun loadButtons() {
        val dao = MyApplication.getDatabase(this).botonSonidoDao()

        lifecycleScope.launch {
            botones = withContext(Dispatchers.IO) {
                dao.obtenerBotonesPorUsuario(usuarioId)
            }
            renderButtons()
        }
    }

    private fun renderButtons() {
        binding.layoutSoundButtons.removeAllViews()
        binding.tvwEmptyButtons.visibility = if (botones.isEmpty()) View.VISIBLE else View.GONE

        botones.forEach { boton ->
            if (editMode) {
                addEditableButtonRow(boton)
            } else {
                addNormalSoundButton(boton)
            }
        }
    }

    private fun addNormalSoundButton(boton: BotonSonido) {
        val button = Button(this).apply {
            text = boton.nombreBoton
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 0)
            }
            setOnClickListener {
                playSound(boton)
            }
        }
        binding.layoutSoundButtons.addView(button)
    }

    private fun addEditableButtonRow(boton: BotonSonido) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 0)
            }
        }

        val soundButton = Button(this).apply {
            text = boton.nombreBoton
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                playSound(boton)
            }
        }

        val deleteButton = Button(this).apply {
            text = getString(R.string.delete_cross)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(12, 0, 0, 0)
            }
            setOnClickListener {
                confirmDeleteButton(boton)
            }
        }

        row.addView(soundButton)
        row.addView(deleteButton)
        binding.layoutSoundButtons.addView(row)
    }

    private fun playSound(boton: BotonSonido) {
        val soundFile = File(boton.rutaArchivo)
        if (!soundFile.exists()) {
            Toast.makeText(this, getString(R.string.msg_sound_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(soundFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    releasePlayer()
                }
            }
        } catch (exception: Exception) {
            releasePlayer()
            Toast.makeText(this, getString(R.string.msg_sound_play_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun confirmDeleteButton(boton: BotonSonido) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message, boton.nombreBoton))
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                deleteButton(boton)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun deleteButton(boton: BotonSonido) {
        val dao = MyApplication.getDatabase(this).botonSonidoDao()
        val audioFileManager = AudioFileManager(this)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                audioFileManager.deleteLocalSound(boton.rutaArchivo)
                dao.eliminarBoton(boton)
            }
            Toast.makeText(
                this@SoundBoardActivity,
                getString(R.string.msg_button_deleted),
                Toast.LENGTH_SHORT
            ).show()
            loadButtons()
        }
    }
}
