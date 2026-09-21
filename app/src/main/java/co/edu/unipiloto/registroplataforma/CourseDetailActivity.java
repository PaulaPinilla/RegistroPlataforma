package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

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
    private Course cursoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnInscribirme.setOnClickListener(v -> pedirContrasenaYInscribir());
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
                cursoActual = curso;
                binding.tvTituloCurso.setText(curso.getTitulo());
                binding.tvDescripcionCurso.setText(curso.getDescripcion());
                binding.tvProfesorCurso.setText("Profesor: " + curso.getProfesorNombre());

                if (yaInscrito) {
                    binding.btnInscribirme.setEnabled(false);
                    binding.btnInscribirme.setText("Ya estás inscrito");
                }
            });
        });
    }

    private void pedirContrasenaYInscribir() {
        if (cursoActual == null) return;

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Contraseña del curso");

        new AlertDialog.Builder(this)
                .setTitle("Ingresa la contraseña del curso")
                .setMessage("Pídesela a tu profesor si no la tienes.")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Inscribirme", (dialog, which) -> {
                    String ingresada = input.getText().toString();
                    if (ingresada.equals(cursoActual.getContrasenaCurso())) {
                        inscribirme();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Contraseña incorrecta")
                                .setMessage("Intenta de nuevo.")
                                .setPositiveButton("Aceptar", null)
                                .show();
                    }
                })
                .show();
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
                .setTitle("¡Inscripción exitosa! ")
                .setMessage("El curso ya aparece en tu sección \"Mis cursos\".")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }
}