package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityStatsBinding;

public class StatsActivity extends AppCompatActivity {

    private ActivityStatsBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        binding.btnVolver.setOnClickListener(v -> finish());

        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        executor.execute(() -> {
            int estudiantes = db.userDao().contarPorRol(User.ROL_ESTUDIANTE);
            int profesores = db.userDao().contarPorRol(User.ROL_PROFESOR);
            int coordinadores = db.userDao().contarPorRol(User.ROL_COORDINADOR);
            int totalCursos = db.courseDao().contarCursos();
            int cursosPendientes = db.courseDao().contarCursosPorEstado(Course.ESTADO_PENDIENTE);
            int cursosAprobados = db.courseDao().contarCursosPorEstado(Course.ESTADO_APROBADO);
            int cursosRechazados = db.courseDao().contarCursosPorEstado(Course.ESTADO_RECHAZADO);
            int inscripciones = db.enrollmentDao().contarInscripciones();

            runOnUiThread(() -> {
                binding.tvEstudiantes.setText("Estudiantes: " + estudiantes);
                binding.tvProfesores.setText("Profesores: " + profesores);
                binding.tvCoordinadores.setText("Coordinadores: " + coordinadores);
                binding.tvTotalCursos.setText("Total de cursos: " + totalCursos);
                binding.tvCursosPendientes.setText("Pendientes de aprobación: " + cursosPendientes);
                binding.tvCursosAprobados.setText("Aprobados: " + cursosAprobados);
                binding.tvCursosRechazados.setText("Rechazados: " + cursosRechazados);
                binding.tvInscripciones.setText("Inscripciones totales: " + inscripciones);
            });
        });
    }
}