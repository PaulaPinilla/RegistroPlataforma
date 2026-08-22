package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLoginBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        String correoPrellenado = getIntent().getStringExtra("correo_prellenado");
        if (correoPrellenado != null) {
            binding.etCorreo.setText(correoPrellenado);
        }

        binding.btnLogin.setOnClickListener(v -> intentarLogin());
        binding.tvIrRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void intentarLogin() {
        binding.tilPassword.setError(null);

        String correo = binding.etCorreo.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (correo.isEmpty() || password.isEmpty()) {
            binding.tilPassword.setError("Completa correo y contraseña");
            return;
        }

        executor.execute(() -> {
            User usuario = db.userDao().login(correo, password);

            runOnUiThread(() -> {
                if (usuario == null) {
                    binding.tilPassword.setError("Correo o contraseña incorrectos");
                } else {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("nombre", usuario.getNombre());
                    startActivity(intent);
                    finish();
                }
            });
        });
    }
}