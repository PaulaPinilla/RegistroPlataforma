package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityCourseFormBinding;

public class CourseFormActivity extends AppCompatActivity {

    private ActivityCourseFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int profesorId;
    private String profesorNombre;
    private int cursoId = -1;
    private Course cursoExistente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        profesorId = getIntent().getIntExtra("profesorId", -1);
        profesorNombre = getIntent().getStringExtra("profesorNombre");
        cursoId = getIntent().getIntExtra("cursoId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());

        boolean esEdicion = cursoId != -1;
        binding.tvTitulo.setText(esEdicion ? "Editar curso" : "Crear curso");
        binding.btnGuardar.setText(esEdicion ? "Guardar cambios" : "Crear curso");

        if (esEdicion) {
            cargarCurso();
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void cargarCurso() {
        executor.execute(() -> {
            cursoExistente = db.courseDao().obtenerPorId(cursoId);
            if (cursoExistente == null) return;
            runOnUiThread(() -> {
                binding.etTituloCurso.setText(cursoExistente.getTitulo());
                binding.etDescripcionCurso.setText(cursoExistente.getDescripcion());
                binding.etContrasenaCurso.setText(cursoExistente.getContrasenaCurso());
            });
        });
    }

    private void guardar() {
        binding.tilTituloCurso.setError(null);
        binding.tilDescripcionCurso.setError(null);
        binding.tilContrasenaCurso.setError(null);

        String titulo = binding.etTituloCurso.getText().toString().trim();
        String descripcion = binding.etDescripcionCurso.getText().toString().trim();
        String contrasena = binding.etContrasenaCurso.getText().toString().trim();

        boolean valido = true;

        if (titulo.length() < 3) {
            binding.tilTituloCurso.setError("El título debe tener al menos 3 caracteres");
            valido = false;
        }

        if (descripcion.isEmpty()) {
            binding.tilDescripcionCurso.setError("Describe brevemente el curso");
            valido = false;
        }

        if (contrasena.length() < 4) {
            binding.tilContrasenaCurso.setError("Mínimo 4 caracteres");
            valido = false;
        }

        if (!valido) return;

        boolean esEdicion = cursoId != -1;

        executor.execute(() -> {
            if (esEdicion && cursoExistente != null) {
                cursoExistente.setTitulo(titulo);
                cursoExistente.setDescripcion(descripcion);
                cursoExistente.setContrasenaCurso(contrasena);
                db.courseDao().actualizar(cursoExistente);
            } else {
                db.courseDao().insertar(new Course(titulo, descripcion, profesorId, profesorNombre, contrasena));
            }
            runOnUiThread(() -> mostrarConfirmacion(esEdicion));
        });
    }

    private void mostrarConfirmacion(boolean esEdicion) {
        new AlertDialog.Builder(this)
                .setTitle(esEdicion ? "Curso actualizado ✅" : "Curso creado 🎉")
                .setMessage(esEdicion
                        ? "Los cambios se guardaron correctamente."
                        : "El curso ya está disponible. Comparte la contraseña con tus estudiantes.")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }
}