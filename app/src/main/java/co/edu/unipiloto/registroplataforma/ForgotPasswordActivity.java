package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForgotPasswordBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        binding.btnRestablecer.setOnClickListener(v -> intentarRestablecer());
        binding.tvVolverLogin.setOnClickListener(v -> finish());
    }

    private void intentarRestablecer() {
        binding.tilCorreo.setError(null);
        binding.tilPassword.setError(null);
        binding.tilPasswordConfirm.setError(null);

        String correo = binding.etCorreo.getText().toString().trim();
        String nuevaPassword = binding.etPassword.getText().toString();
        String nuevaPasswordConfirm = binding.etPasswordConfirm.getText().toString();

        boolean valido = true;

        if (correo.isEmpty()) {
            binding.tilCorreo.setError("Este campo es obligatorio");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.setError("Ingresa un correo válido");
            valido = false;
        }

        if (nuevaPassword.length() < 6) {
            binding.tilPassword.setError("Mínimo 6 caracteres");
            valido = false;
        }

        if (!nuevaPassword.equals(nuevaPasswordConfirm)) {
            binding.tilPasswordConfirm.setError("Las contraseñas no coinciden");
            valido = false;
        }

        if (!valido) return;

        executor.execute(() -> {
            User existente = db.userDao().buscarPorCorreo(correo);

            if (existente == null) {
                runOnUiThread(() ->
                        binding.tilCorreo.setError("No existe una cuenta con este correo"));
                return;
            }

            db.userDao().actualizarPassword(correo, nuevaPassword);

            new Thread(() -> EmailSender.enviar(
                    correo,
                    "Tu contraseña fue restablecida",
                    "Hola " + existente.getNombre() + ",\n\n" +
                            "Tu contraseña de la plataforma académica se actualizó correctamente."
            )).start();

            runOnUiThread(() -> mostrarConfirmacion(correo));
        });
    }

    private void mostrarConfirmacion(String correo) {
        new AlertDialog.Builder(this)
                .setTitle("Contraseña actualizada ✅")
                .setMessage("Tu contraseña se restableció correctamente. Ya puedes iniciar sesión con la nueva.")
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