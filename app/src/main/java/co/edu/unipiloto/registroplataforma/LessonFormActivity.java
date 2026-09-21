package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private Uri archivoSeleccionado;
    private String nombreArchivoSeleccionado;

    private ActivityResultLauncher<String[]> selectorArchivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);

        selectorArchivo = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) { }
                    archivoSeleccionado = uri;
                    nombreArchivoSeleccionado = obtenerNombreArchivo(uri);
                    binding.tvArchivoSeleccionado.setText("📎 " + nombreArchivoSeleccionado);
                    binding.tvArchivoSeleccionado.setVisibility(View.VISIBLE);
                });

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAdjuntarDocumento.setOnClickListener(v ->
                selectorArchivo.launch(new String[]{"application/pdf", "image/jpeg", "image/png"}));
        binding.btnGuardar.setOnClickListener(v -> guardar());

        binding.rgTipo.setOnCheckedChangeListener((group, checkedId) -> actualizarVisibilidad());

        actualizarVisibilidad();
    }

    private void actualizarVisibilidad() {
        boolean esVideo = binding.rbVideo.isChecked();

        binding.tilUrlMaterial.setVisibility(esVideo ? View.VISIBLE : View.GONE);
        binding.tvFormatoDocumento.setVisibility(esVideo ? View.GONE : View.VISIBLE);
        binding.tilContenidoTexto.setVisibility(esVideo ? View.GONE : View.VISIBLE);
        binding.btnAdjuntarDocumento.setVisibility(esVideo ? View.GONE : View.VISIBLE);
        binding.tvArchivoSeleccionado.setVisibility(
                (!esVideo && archivoSeleccionado != null) ? View.VISIBLE : View.GONE);
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = uri.getLastPathSegment();
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) nombre = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return nombre != null ? nombre : "documento";
    }

    private void guardar() {
        binding.tilTituloLeccion.setError(null);
        binding.tilUrlMaterial.setError(null);
        binding.tilContenidoTexto.setError(null);

        String titulo = binding.etTituloLeccion.getText().toString().trim();
        boolean esVideo = binding.rbVideo.isChecked();
        String tipo = esVideo ? Lesson.TIPO_VIDEO : Lesson.TIPO_DOCUMENTO;

        boolean valido = true;

        if (titulo.length() < 3) {
            binding.tilTituloLeccion.setError("Escribe un título para la lección");
            valido = false;
        }

        String urlMaterial = null;
        String contenidoTexto = null;

        if (esVideo) {
            urlMaterial = binding.etUrlMaterial.getText().toString().trim();
            if (urlMaterial.isEmpty()) {
                binding.tilUrlMaterial.setError("Pega el enlace del video");
                valido = false;
            }
        } else {
            String textoIngresado = binding.etContenidoTexto.getText().toString().trim();
            contenidoTexto = textoIngresado.isEmpty() ? null : textoIngresado;

            if (archivoSeleccionado != null) {
                urlMaterial = archivoSeleccionado.toString();
            }

            if (contenidoTexto == null && urlMaterial == null) {
                Toast.makeText(this, "Escribe el contenido, adjunta un documento, o ambos", Toast.LENGTH_LONG).show();
                valido = false;
            }
        }

        if (!valido) return;

        String urlFinal = urlMaterial;
        String textoFinal = contenidoTexto;

        executor.execute(() -> {
            db.lessonDao().insertar(new Lesson(unidadId, titulo, tipo, urlFinal, textoFinal));
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Lección creada")
                    .setMessage("Ya está disponible para los estudiantes inscritos.")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .show());
        });
    }
}