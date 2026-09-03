package co.edu.unipiloto.registroplataforma;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Lesson;
import co.edu.unipiloto.registroplataforma.data.LessonProgress;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLessonDetailBinding;

public class LessonDetailActivity extends AppCompatActivity {

    private ActivityLessonDetailBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int leccionId;
    private int estudianteId;
    private Lesson leccionActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        leccionId = getIntent().getIntExtra("leccionId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAbrirMaterial.setOnClickListener(v -> abrirMaterial());
        binding.btnMarcarCompletada.setOnClickListener(v -> marcarCompletada());

        cargarLeccion();
    }

    private void cargarLeccion() {
        executor.execute(() -> {
            Lesson leccion = db.lessonDao().obtenerPorId(leccionId);
            boolean completada = db.lessonProgressDao().existeProgreso(leccionId, estudianteId) > 0;

            runOnUiThread(() -> {
                if (leccion == null) {
                    finish();
                    return;
                }
                leccionActual = leccion;
                binding.tvTituloLeccion.setText(leccion.getTitulo());
                binding.tvTipoLeccion.setText(tipoLegible(leccion.getTipo()));

                if (completada) {
                    binding.btnMarcarCompletada.setEnabled(false);
                    binding.btnMarcarCompletada.setText("Lección completada ✅");
                }
            });
        });
    }

    private String tipoLegible(String tipo) {
        if (Lesson.TIPO_VIDEO.equals(tipo)) return "🎥 Video";
        if (Lesson.TIPO_DOCUMENTO.equals(tipo)) return "📄 Documento";
        return "🔗 Material";
    }

    private void abrirMaterial() {
        if (leccionActual == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(leccionActual.getUrlMaterial()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("No se pudo abrir")
                    .setMessage("El enlace no es válido: " + leccionActual.getUrlMaterial())
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void marcarCompletada() {
        binding.btnMarcarCompletada.setEnabled(false);
        executor.execute(() -> {
            boolean yaCompletada = db.lessonProgressDao().existeProgreso(leccionId, estudianteId) > 0;
            if (!yaCompletada) {
                db.lessonProgressDao().insertar(new LessonProgress(leccionId, estudianteId, System.currentTimeMillis()));
            }
            runOnUiThread(() -> binding.btnMarcarCompletada.setText("Lección completada ✅"));
        });
    }
}