package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.data.ForumPost;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumDetailBinding;

public class ForumDetailActivity extends AppCompatActivity {

    private ActivityForumDetailBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private int foroId;
    private int usuarioId;
    private boolean esProfesor;

    private ForumPost respondiendoA;
    private Uri archivoSeleccionado;
    private String nombreArchivoSeleccionado;

    private ActivityResultLauncher<String[]> selectorArchivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        db = AppDatabase.getInstance(this);
        foroId = getIntent().getIntExtra("foroId", -1);
        usuarioId = getIntent().getIntExtra("usuarioId", -1);

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
        binding.btnAdjuntarArchivo.setOnClickListener(v ->
                selectorArchivo.launch(new String[]{"*/*"}));
        binding.tvArchivoSeleccionado.setOnClickListener(v -> {
            archivoSeleccionado = null;
            nombreArchivoSeleccionado = null;
            binding.tvArchivoSeleccionado.setVisibility(View.GONE);
        });
        binding.tvCancelarRespuesta.setOnClickListener(v -> cancelarRespuesta());
        binding.btnPublicar.setOnClickListener(v -> publicarComentario());

        cargarForo();
    }

    private void cargarForo() {
        executor.execute(() -> {
            Forum foro = db.forumDao().obtenerPorId(foroId);
            List<ForumPost> comentarios = db.forumPostDao().listarPorForo(foroId);
            User usuario = db.userDao().obtenerPorId(usuarioId);
            boolean profesor = usuario != null && usuario.esProfesor();

            Map<Integer, ForumPost> porId = new HashMap<>();
            for (ForumPost c : comentarios) porId.put(c.getId(), c);

            runOnUiThread(() -> {
                if (foro == null) {
                    finish();
                    return;
                }
                esProfesor = profesor;
                binding.tvTema.setText(foro.getTema());
                pintarComentarios(comentarios, porId);
            });
        });
    }

    private void pintarComentarios(List<ForumPost> comentarios, Map<Integer, ForumPost> porId) {
        binding.contenedorComentarios.removeAllViews();

        if (esProfesor) {
            TextView btnCalificar = new TextView(this);
            btnCalificar.setText("Calificar participación de estudiantes");
            btnCalificar.setTextColor(0xFF9B7BD6);
            btnCalificar.setTypeface(null, Typeface.BOLD);
            btnCalificar.setPadding(0, 0, 0, 24);
            btnCalificar.setOnClickListener(v -> {
                Intent intent = new Intent(this, ForumGradeListActivity.class);
                intent.putExtra("foroId", foroId);
                startActivity(intent);
            });
            binding.contenedorComentarios.addView(btnCalificar);
        }

        if (comentarios.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Aún no hay participaciones. ¡Sé el primero en comentar!");
            vacio.setPadding(0, 24, 0, 24);
            binding.contenedorComentarios.addView(vacio);
            return;
        }

        for (ForumPost c : comentarios) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, 24, 0, 0);

            if (c.esRespuesta()) {
                ForumPost original = porId.get(c.getRespondiendoAId());
                LinearLayout cita = new LinearLayout(this);
                cita.setOrientation(LinearLayout.VERTICAL);
                cita.setPadding(20, 8, 8, 8);
                cita.setBackgroundColor(0x22FFFFFF);

                TextView citaAutor = new TextView(this);
                citaAutor.setText(original != null ? original.getAutorNombre() : "Comentario");
                citaAutor.setTextSize(12);
                citaAutor.setTypeface(null, Typeface.BOLD);
                cita.addView(citaAutor);

                TextView citaTexto = new TextView(this);
                String snippet = original != null
                        ? (original.getTexto() != null && !original.getTexto().isEmpty()
                           ? original.getTexto() : "📎 Archivo adjunto")
                        : "(comentario eliminado)";
                if (snippet.length() > 80) snippet = snippet.substring(0, 80) + "...";
                citaTexto.setText(snippet);
                citaTexto.setTextSize(12);
                cita.addView(citaTexto);

                item.addView(cita);
            }

            TextView autorFecha = new TextView(this);
            autorFecha.setText(c.getAutorNombre() + " — " + formato.format(c.getFecha()));
            autorFecha.setTextSize(13);
            autorFecha.setTypeface(null, Typeface.BOLD_ITALIC);
            autorFecha.setPadding(0, 6, 0, 0);
            item.addView(autorFecha);

            if (c.getTexto() != null && !c.getTexto().isEmpty()) {
                TextView texto = new TextView(this);
                texto.setText(c.getTexto());
                texto.setPadding(0, 4, 0, 0);
                item.addView(texto);
            }

            if (c.tieneArchivo()) {
                TextView archivo = new TextView(this);
                archivo.setText("📎 " + c.getNombreArchivo());
                archivo.setTextColor(0xFF9B7BD6);
                archivo.setPadding(0, 4, 0, 0);
                archivo.setOnClickListener(v -> abrirArchivo(c.getUriArchivo()));
                item.addView(archivo);
            }

            TextView responder = new TextView(this);
            responder.setText("Responder");
            responder.setTextSize(12);
            responder.setTextColor(0xFF9B7BD6);
            responder.setPadding(0, 6, 0, 0);
            responder.setOnClickListener(v -> iniciarRespuesta(c));
            item.addView(responder);

            binding.contenedorComentarios.addView(item);
        }
    }

    private void iniciarRespuesta(ForumPost comentario) {
        respondiendoA = comentario;
        String snippet = comentario.getTexto() != null && !comentario.getTexto().isEmpty()
                ? comentario.getTexto() : "📎 Archivo adjunto";
        if (snippet.length() > 60) snippet = snippet.substring(0, 60) + "...";
        binding.tvCancelarRespuesta.setText("Respondiendo a " + comentario.getAutorNombre() + ": " + snippet + "  ✕");
        binding.tvCancelarRespuesta.setVisibility(View.VISIBLE);
    }

    private void cancelarRespuesta() {
        respondiendoA = null;
        binding.tvCancelarRespuesta.setVisibility(View.GONE);
    }

    private void abrirArchivo(String uriTexto) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriTexto));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception ignored) { }
    }

    private String obtenerNombreArchivo(Uri uri) {
        String nombre = uri.getLastPathSegment();
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) nombre = cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return nombre != null ? nombre : "archivo";
    }

    private void publicarComentario() {
        String texto = binding.etNuevoComentario.getText().toString().trim();
        if (texto.isEmpty() && archivoSeleccionado == null) return;

        binding.btnPublicar.setEnabled(false);

        Integer respuestaAId = respondiendoA != null ? respondiendoA.getId() : null;
        String uriArchivoFinal = archivoSeleccionado != null ? archivoSeleccionado.toString() : null;
        String nombreArchivoFinal = nombreArchivoSeleccionado;
        String textoFinal = texto.isEmpty() ? null : texto;

        executor.execute(() -> {
            User usuario = db.userDao().obtenerPorId(usuarioId);
            String nombre = usuario != null ? usuario.getNombre() : "Usuario";
            db.forumPostDao().insertar(new ForumPost(foroId, usuarioId, nombre, textoFinal,
                    System.currentTimeMillis(), uriArchivoFinal, nombreArchivoFinal, respuestaAId));
            runOnUiThread(() -> {
                binding.etNuevoComentario.setText("");
                binding.btnPublicar.setEnabled(true);
                archivoSeleccionado = null;
                nombreArchivoSeleccionado = null;
                binding.tvArchivoSeleccionado.setVisibility(View.GONE);
                cancelarRespuesta();
                cargarForo();
            });
        });
    }
}