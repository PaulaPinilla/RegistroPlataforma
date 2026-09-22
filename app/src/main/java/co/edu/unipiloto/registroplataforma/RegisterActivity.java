package co.edu.unipiloto.registroplataforma;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityRegisterBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private final Calendar fechaNacimientoSeleccionada = Calendar.getInstance();
    private boolean fechaSeleccionada = false;

    private Double latitudSeleccionada;
    private Double longitudSeleccionada;

    private FusedLocationProviderClient clienteUbicacion;
    private ActivityResultLauncher<String> solicitarPermisoUbicacion;

    private static final String[] ROLES = {"Estudiante", "Profesor"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this);

        solicitarPermisoUbicacion = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                concedido -> {
                    if (concedido) {
                        obtenerUbicacionActual();
                    } else {
                        Toast.makeText(this, "Se necesita el permiso de ubicación para esta función", Toast.LENGTH_LONG).show();
                    }
                });

        ArrayAdapter<String> adaptadorRoles = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ROLES);
        adaptadorRoles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spRol.setAdapter(adaptadorRoles);

        binding.etFechaNacimiento.setOnClickListener(v -> mostrarSelectorFecha());

        binding.btnUsarUbicacion.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionActual();
            } else {
                solicitarPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        binding.btnRegistrar.setOnClickListener(v -> intentarRegistrar());
        binding.tvIrLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void mostrarSelectorFecha() {
        Calendar hoy = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            fechaNacimientoSeleccionada.set(year, month, day, 0, 0, 0);
            fechaSeleccionada = true;
            binding.etFechaNacimiento.setText(formatoFecha.format(fechaNacimientoSeleccionada.getTime()));
            binding.tilFechaNacimiento.setError(null);
        }, hoy.get(Calendar.YEAR) - 18, hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private void obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        clienteUbicacion.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitudSeleccionada = location.getLatitude();
                longitudSeleccionada = location.getLongitude();
                binding.etLatitud.setText(String.valueOf(latitudSeleccionada));
                binding.etLongitud.setText(String.valueOf(longitudSeleccionada));
                Toast.makeText(this, "Ubicación obtenida ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación. Activa el GPS del emulador (Extended Controls > Location).", Toast.LENGTH_LONG).show();
            }
        });
    }

    private int calcularEdad(Calendar nacimiento) {
        Calendar hoy = Calendar.getInstance();
        int edad = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR);
        if (hoy.get(Calendar.DAY_OF_YEAR) < nacimiento.get(Calendar.DAY_OF_YEAR)) {
            edad--;
        }
        return edad;
    }

    private void intentarRegistrar() {
        binding.tilNombre.setError(null);
        binding.tilUsuario.setError(null);
        binding.tilCorreo.setError(null);
        binding.tilDireccion.setError(null);
        binding.tilPassword.setError(null);
        binding.tilPasswordConfirm.setError(null);
        binding.tilFechaNacimiento.setError(null);

        String nombre = binding.etNombre.getText().toString().trim();
        String usuario = binding.etUsuario.getText().toString().trim();
        String correo = binding.etCorreo.getText().toString().trim();
        String direccion = binding.etDireccion.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String passwordConfirm = binding.etPasswordConfirm.getText().toString();

        int posicionRol = binding.spRol.getSelectedItemPosition();
        String rol = (posicionRol == 1) ? User.ROL_PROFESOR : User.ROL_ESTUDIANTE;

        String genero;
        int idGenero = binding.rgGenero.getCheckedRadioButtonId();
        if (idGenero == binding.rbFemenino.getId()) genero = User.GENERO_FEMENINO;
        else if (idGenero == binding.rbNoBinario.getId()) genero = User.GENERO_NO_BINARIO;
        else genero = User.GENERO_MASCULINO;

        boolean valido = true;

        if (nombre.isEmpty()) {
            binding.tilNombre.setError("Este campo es obligatorio");
            valido = false;
        }

        if (usuario.isEmpty()) {
            binding.tilUsuario.setError("Este campo es obligatorio");
            valido = false;
        }

        if (correo.isEmpty()) {
            binding.tilCorreo.setError("Este campo es obligatorio");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.setError("Ingresa un correo válido");
            valido = false;
        }

        if (direccion.isEmpty()) {
            binding.tilDireccion.setError("Ingresa tu dirección");
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

        if (!fechaSeleccionada) {
            binding.tilFechaNacimiento.setError("Selecciona tu fecha de nacimiento");
            valido = false;
        } else if (calcularEdad(fechaNacimientoSeleccionada) < 18) {
            binding.tilFechaNacimiento.setError("Debes ser mayor de 18 años para registrarte");
            valido = false;
        }

        if (!valido) return;

        long fechaNacimientoMillis = fechaNacimientoSeleccionada.getTimeInMillis();

        executor.execute(() -> {
            User correoExistente = db.userDao().buscarPorCorreo(correo);
            if (correoExistente != null) {
                runOnUiThread(() -> binding.tilCorreo.setError("Este correo ya está registrado"));
                return;
            }

            User usuarioExistente = db.userDao().buscarPorUsuario(usuario);
            if (usuarioExistente != null) {
                runOnUiThread(() -> binding.tilUsuario.setError("Este usuario ya existe, elige otro"));
                return;
            }

            db.userDao().insertar(new User(
                    nombre, usuario, correo, direccion, latitudSeleccionada, longitudSeleccionada,
                    password, rol, fechaNacimientoMillis, genero));

            runOnUiThread(() -> mostrarConfirmacion(usuario));
        });
    }

    private void mostrarConfirmacion(String usuario) {
        new AlertDialog.Builder(this)
                .setTitle("¡Registro exitoso! 🎉")
                .setMessage("Tu cuenta se creó correctamente. Ahora puedes iniciar sesión.")
                .setCancelable(false)
                .setPositiveButton("Ir a iniciar sesión", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("usuario_prellenado", usuario);
                    startActivity(intent);
                    finish();
                })
                .show();
    }
}