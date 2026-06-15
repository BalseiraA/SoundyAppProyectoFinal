package com.example.soundy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundy.databinding.ActivityAddButtonBinding
import com.example.soundy.model.BotonSonido
import com.example.soundy.storage.AudioFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddButtonActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USUARIO_ID = "EXTRA_USUARIO_ID"
    }

    private lateinit var binding: ActivityAddButtonBinding
    private var usuarioId: Int = -1
    private var selectedSoundUri: Uri? = null
    private var selectedSoundDurationMs: Long = 0L
    private var selectedOriginalName: String = ""

    private val soundPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            handleSelectedSound(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAddButtonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        usuarioId = intent.getIntExtra(EXTRA_USUARIO_ID, -1)
        if (usuarioId == -1) {
            Toast.makeText(this, getString(R.string.msg_user_not_received), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnChooseSound.setOnClickListener {
            soundPickerLauncher.launch(arrayOf("audio/*", "video/mp4"))
        }

        binding.btnSaveSoundButton.setOnClickListener {
            saveSoundButton()
        }

        binding.btnCancelSoundButton.setOnClickListener {
            finish()
        }
    }

    private fun handleSelectedSound(uri: Uri) {
        val audioFileManager = AudioFileManager(this)

        lifecycleScope.launch {
            val durationMs = withContext(Dispatchers.IO) {
                audioFileManager.getDurationMs(uri)
            }

            if (durationMs <= 0L) {
                selectedSoundUri = null
                selectedSoundDurationMs = 0L
                selectedOriginalName = ""
                binding.tvwSelectedSound.text = getString(R.string.no_sound_selected)
                Toast.makeText(
                    this@AddButtonActivity,
                    getString(R.string.msg_duration_not_readable),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            if (durationMs > AudioFileManager.MAX_DURATION_MS) {
                selectedSoundUri = null
                selectedSoundDurationMs = 0L
                selectedOriginalName = ""
                binding.tvwSelectedSound.text = getString(R.string.no_sound_selected)
                Toast.makeText(
                    this@AddButtonActivity,
                    getString(R.string.msg_sound_too_long),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // La app copia el archivo al guardar; si no se puede persistir el permiso, aún puede funcionar en esta sesión.
            }

            selectedSoundUri = uri
            selectedSoundDurationMs = durationMs
            selectedOriginalName = audioFileManager.getDisplayName(uri)
            binding.tvwSelectedSound.text = getString(
                R.string.selected_sound,
                selectedOriginalName,
                durationMs / 1000
            )
        }
    }

    private fun saveSoundButton() {
        val buttonName = binding.edtButtonName.text.toString().trim()
        val soundUri = selectedSoundUri

        if (buttonName.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_empty_button_name), Toast.LENGTH_SHORT).show()
            return
        }

        if (soundUri == null) {
            Toast.makeText(this, getString(R.string.msg_choose_sound), Toast.LENGTH_SHORT).show()
            return
        }

        val dao = MyApplication.getDatabase(this).botonSonidoDao()
        val audioFileManager = AudioFileManager(this)

        lifecycleScope.launch {
            try {
                val localFile = withContext(Dispatchers.IO) {
                    audioFileManager.copySoundToInternalStorage(soundUri, buttonName)
                }

                withContext(Dispatchers.IO) {
                    dao.insertarBoton(
                        BotonSonido(
                            usuarioId = usuarioId,
                            nombreBoton = buttonName,
                            rutaArchivo = localFile.absolutePath,
                            nombreArchivoOriginal = selectedOriginalName,
                            duracionMs = selectedSoundDurationMs
                        )
                    )
                }

                Toast.makeText(
                    this@AddButtonActivity,
                    getString(R.string.msg_button_saved),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (exception: Exception) {
                Toast.makeText(
                    this@AddButtonActivity,
                    getString(R.string.msg_button_save_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
