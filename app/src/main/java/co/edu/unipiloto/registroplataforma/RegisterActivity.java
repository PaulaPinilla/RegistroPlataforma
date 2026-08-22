package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityRegisterBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        binding.btnRegistrar.setOnClickListener(v -> intentarRegistrar());
        binding.tvIrLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void intentarRegistrar() {
        binding.tilNombre.setError(null);
        binding.tilCorreo.setError(null);
        binding.tilPassword.setError(null);
        binding.tilPasswordConfirm.setError(null);

        String nombre = binding.etNombre.getText().toString().trim();
        String correo = binding.etCorreo.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String passwordConfirm = binding.etPasswordConfirm.getText().toString();

        boolean valido = true;

        if (nombre.isEmpty()) {
            binding.tilNombre.setError("Este campo es obligatorio");
            valido = false;
        }

        if (correo.isEmpty()) {
            binding.tilCorreo.setError("Este campo es obligatorio");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.setError("Ingresa un correo válido");
            valido = false;
        }

        if (password.length() < 6) {
            binding.tilPassword.setError("Mínimo 6 caracteres");
            valido = false;
        }

        if (!password.equals(passwordConfirm)) {
            binding.tilPasswordConfirm.setError("Las contraseñas no coinciden");
            valido = false;
        }

        if (!valido) return;

        executor.execute(() -> {
            User existente = db.userDao().buscarPorCorreo(correo);

            if (existente != null) {
                runOnUiThread(() ->
                        binding.tilCorreo.setError("Este correo ya está registrado"));
                return;
            }

            db.userDao().insertar(new User(nombre, correo, password));

            runOnUiThread(() -> mostrarConfirmacion(correo));
        });
    }

    private void mostrarConfirmacion(String correo) {
        new AlertDialog.Builder(this)
                .setTitle("¡Registro exitoso! 🎉")
                .setMessage("Tu cuenta se creó correctamente. Ahora puedes iniciar sesión.")
                .setCancelable(false)
                .setPositiveButton("Ir a iniciar sesión", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("correo_prellenado", correo);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}