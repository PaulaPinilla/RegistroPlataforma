package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int usuarioId = getIntent().getIntExtra("usuarioId", -1);
        String nombre = getIntent().getStringExtra("nombre");
        String rol = getIntent().getStringExtra("rol");
        if (nombre == null) nombre = "";
        if (rol == null) rol = User.ROL_ESTUDIANTE;

        binding.tvBienvenida.setText("¡Hola, " + nombre + "! 👋");

        boolean esProfesor = User.ROL_PROFESOR.equals(rol);

        binding.tvRol.setText(esProfesor ? "Rol: Profesor" : "Rol: Estudiante");

        binding.btnMisCursosProfesor.setVisibility(esProfesor ? View.VISIBLE : View.GONE);
        binding.btnCursosDisponibles.setVisibility(esProfesor ? View.GONE : View.VISIBLE);
        binding.btnMisCursosEstudiante.setVisibility(esProfesor ? View.GONE : View.VISIBLE);

        final int idFinal = usuarioId;
        final String nombreFinal = nombre;

        binding.btnMisCursosProfesor.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseListActivity.class);
            intent.putExtra("profesorId", idFinal);
            intent.putExtra("profesorNombre", nombreFinal);
            startActivity(intent);
        });

        binding.btnCursosDisponibles.setOnClickListener(v -> {
            Intent intent = new Intent(this, AvailableCoursesActivity.class);
            intent.putExtra("estudianteId", idFinal);
            startActivity(intent);
        });

        binding.btnMisCursosEstudiante.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyCoursesActivity.class);
            intent.putExtra("estudianteId", idFinal);
            startActivity(intent);
        });
    }
}