package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.Enrollment;
import co.edu.unipiloto.registroplataforma.databinding.ActivityCourseDetailBinding;

public class CourseDetailActivity extends AppCompatActivity {

    private ActivityCourseDetailBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int cursoId;
    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnInscribirme.setOnClickListener(v -> inscribirme());
        cargarCurso();
    }

    private void cargarCurso() {
        executor.execute(() -> {
            Course curso = db.courseDao().obtenerPorId(cursoId);
            boolean yaInscrito = db.enrollmentDao().existeInscripcion(cursoId, estudianteId) > 0;

            runOnUiThread(() -> {
                if (curso == null) {
                    finish();
                    return;
                }
                binding.tvTituloCurso.setText(curso.getTitulo());
                binding.tvDescripcionCurso.setText(curso.getDescripcion());
                binding.tvProfesorCurso.setText("Profesor: " + curso.getProfesorNombre());

                if (yaInscrito) {
                    binding.btnInscribirme.setEnabled(false);
                    binding.btnInscribirme.setText("Ya estás inscrito ");
                }
            });
        });
    }

    private void inscribirme() {
        binding.btnInscribirme.setEnabled(false);

        executor.execute(() -> {
            boolean yaInscrito = db.enrollmentDao().existeInscripcion(cursoId, estudianteId) > 0;

            if (yaInscrito) {
                runOnUiThread(() -> binding.btnInscribirme.setText("Ya estás inscrito"));
                return;
            }

            db.enrollmentDao().insertar(new Enrollment(cursoId, estudianteId, System.currentTimeMillis()));

            runOnUiThread(this::mostrarConfirmacion);
        });
    }

    private void mostrarConfirmacion() {
        new AlertDialog.Builder(this)
                .setTitle("¡Inscripción exitosa!")
                .setMessage("El curso ya aparece en tu sección \"Mis cursos\".")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }
}