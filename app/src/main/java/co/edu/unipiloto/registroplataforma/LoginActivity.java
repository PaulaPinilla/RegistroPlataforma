package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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

        String usuarioPrellenado = getIntent().getStringExtra("usuario_prellenado");
        if (usuarioPrellenado != null) {
            binding.etUsuario.setText(usuarioPrellenado);
        }

        binding.btnLogin.setOnClickListener(v -> intentarLogin());
        binding.btnIrRegistro.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
        binding.tvOlvideContrasena.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void intentarLogin() {
        binding.tilPassword.setError(null);

        String usuario = binding.etUsuario.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (usuario.isEmpty() || password.isEmpty()) {
            binding.tilPassword.setError("Completa usuario y contraseña");
            return;
        }

        executor.execute(() -> {
            User usuarioLogueado = db.userDao().login(usuario, password);

            if (usuarioLogueado != null) {
                Log.d("LoginActivity", "Usuario logueado con rol: " + usuarioLogueado.getRol());
                final User usuarioFinal = usuarioLogueado;
                new Thread(() -> EmailSender.enviar(
                        usuarioFinal.getCorreo(),
                        "Inicio de sesión detectado",
                        "Hola " + usuarioFinal.getNombre() + ",\n\n" +
                                "Se acaba de iniciar sesión en tu cuenta de la plataforma académica.\n\n" +
                                "Si no fuiste tú, cambia tu contraseña de inmediato desde la app."
                )).start();
            }

            runOnUiThread(() -> {
                if (usuarioLogueado == null) {
                    binding.tilPassword.setError("Usuario o contraseña incorrectos");
                } else {
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("usuarioId", usuarioLogueado.getId());
                    intent.putExtra("nombre", usuarioLogueado.getNombre());
                    intent.putExtra("correo", usuarioLogueado.getCorreo());
                    intent.putExtra("rol", usuarioLogueado.getRol());
                    startActivity(intent);
                    finish();
                }
            });
        });
    }
}