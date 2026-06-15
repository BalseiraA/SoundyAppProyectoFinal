package com.example.soundy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundy.databinding.ActivitySignUpBinding
import com.example.soundy.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.btnCancelRegister.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val username = binding.edtRegisterUser.text.toString().trim()
        val password = binding.edtRegisterPassword.text.toString().trim()

        if (username.isBlank() || password.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_complete_register), Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioDao = MyApplication.getDatabase(this).usuarioDao()

        lifecycleScope.launch {
            val existingUser = withContext(Dispatchers.IO) {
                usuarioDao.obtenerUsuarioPorNombre(username)
            }

            if (existingUser != null) {
                Toast.makeText(
                    this@SignUpActivity,
                    getString(R.string.msg_user_exists),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            withContext(Dispatchers.IO) {
                usuarioDao.insertarUsuario(
                    Usuario(
                        nombre = username,
                        password = password
                    )
                )
            }

            Toast.makeText(
                this@SignUpActivity,
                getString(R.string.msg_register_success),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}
