package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Lesson;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLessonFormBinding;

public class LessonFormActivity extends AppCompatActivity {

    private ActivityLessonFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int unidadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        binding.tilTituloLeccion.setError(null);
        binding.tilUrlMaterial.setError(null);

        String titulo = binding.etTituloLeccion.getText().toString().trim();
        String url = binding.etUrlMaterial.getText().toString().trim();

        String tipo;
        if (binding.rbVideo.isChecked()) {
            tipo = Lesson.TIPO_VIDEO;
        } else if (binding.rbDocumento.isChecked()) {
            tipo = Lesson.TIPO_DOCUMENTO;
        } else {
            tipo = Lesson.TIPO_OTRO;
        }

        boolean valido = true;

        if (titulo.length() < 3) {
            binding.tilTituloLeccion.setError("Escribe un título para la lección");
            valido = false;
        }

        if (url.isEmpty()) {
            binding.tilUrlMaterial.setError("Pega el enlace del material");
            valido = false;
        }

        if (!valido) return;

        executor.execute(() -> {
            db.lessonDao().insertar(new Lesson(unidadId, titulo, tipo, url));
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Lección creada 🎉")
                    .setMessage("Ya está disponible para los estudiantes inscritos.")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .show());
        });
    }
}