package com.example.soundy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundy.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun login() {
        val username = binding.edtLoginUser.text.toString().trim()
        val password = binding.edtLoginPassword.text.toString().trim()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_complete_login), Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioDao = MyApplication.getDatabase(this).usuarioDao()

        lifecycleScope.launch {
            val usuario = withContext(Dispatchers.IO) {
                usuarioDao.login(username, password)
            }

            if (usuario == null) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.msg_wrong_credentials),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val intent = Intent(this@MainActivity, SoundBoardActivity::class.java).apply {
                    putExtra(SoundBoardActivity.EXTRA_USUARIO_ID, usuario.id)
                    putExtra(SoundBoardActivity.EXTRA_USUARIO_NOMBRE, usuario.nombre)
                }
                startActivity(intent)
            }
        }
    }
}
