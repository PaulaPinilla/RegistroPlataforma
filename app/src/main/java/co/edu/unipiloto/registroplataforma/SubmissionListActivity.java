package co.edu.unipiloto.registroplataforma;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Submission;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivitySubmissionListBinding;

public class SubmissionListActivity extends AppCompatActivity {

    private ActivitySubmissionListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private int actividadId;
    private String actividadTitulo;
    private final List<Submission> entregasActuales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubmissionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        actividadTitulo = getIntent().getStringExtra("actividadTitulo");

        binding.tvTitulo.setText(actividadTitulo != null ? actividadTitulo : "Entregas");
        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> mostrarOpciones(item.id));
        binding.rvEntregas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEntregas.setAdapter(adapter);

        verificarSiEsPreguntas();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarEntregas();
    }

    private void verificarSiEsPreguntas() {
        executor.execute(() -> {
            Assignment actividad = db.assignmentDao().obtenerPorId(actividadId);
            if (actividad != null && Assignment.TIPO_ENTREGA_PREGUNTAS.equals(actividad.getTipoEntrega())) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, QuestionListActivity.class);
                    intent.putExtra("actividadId", actividadId);
                    intent.putExtra("actividadTitulo", actividadTitulo);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }

    private void cargarEntregas() {
        executor.execute(() -> {
            List<Submission> entregas = db.submissionDao().listarPorActividad(actividadId);
            entregasActuales.clear();
            entregasActuales.addAll(entregas);

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Submission e : entregas) {
                User estudiante = db.userDao().obtenerPorId(e.getEstudianteId());
                String nombreEstudiante = estudiante != null ? estudiante.getNombre() : "Estudiante";
                String subtitulo = (e.esTexto() ? "Respuesta de texto" : "📎 " + e.getNombreArchivo())
                        + " — " + formato.format(e.getFechaEntrega())
                        + (e.estaCalificada() ? " — Nota: " + e.getCalificacion() : " — Sin calificar");
                items.add(new SimpleListAdapter.Item(e.getId(), nombreEstudiante, subtitulo, e.estaCalificada()));
            }

            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private Submission buscarEntrega(int submissionId) {
        for (Submission e : entregasActuales) {
            if (e.getId() == submissionId) return e;
        }
        return null;
    }

    private void mostrarOpciones(int submissionId) {
        Submission entrega = buscarEntrega(submissionId);
        if (entrega == null) return;

        if (entrega.esTexto()) {
            new AlertDialog.Builder(this)
                    .setTitle("Respuesta del estudiante")
                    .setMessage(entrega.getTextoRespuesta())
                    .setNegativeButton("Cerrar", null)
                    .setPositiveButton("Calificar", (d, w) -> mostrarDialogoCalificar(entrega))
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(entrega.getNombreArchivo())
                .setItems(new String[]{"Ver documento", "Descargar", "Calificar"}, (dialog, which) -> {
                    if (which == 0) {
                        abrirArchivo(entrega);
                    } else if (which == 1) {
                        descargarArchivo(entrega);
                    } else {
                        mostrarDialogoCalificar(entrega);
                    }
                })
                .show();
    }
    private void mostrarDialogoCalificar(Submission entrega) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        final EditText inputNota = new EditText(this);
        inputNota.setHint("Nota (0.0 a 5.0)");
        inputNota.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (entrega.getCalificacion() != null) inputNota.setText(String.valueOf(entrega.getCalificacion()));
        layout.addView(inputNota);

        final EditText inputComentario = new EditText(this);
        inputComentario.setHint("Comentario (opcional)");
        if (entrega.getRetroalimentacion() != null) inputComentario.setText(entrega.getRetroalimentacion());
        layout.addView(inputComentario);

        new AlertDialog.Builder(this)
                .setTitle("Calificar entrega")
                .setView(layout)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String notaTexto = inputNota.getText().toString().trim();
                    if (notaTexto.isEmpty()) return;
                    Double nota;
                    try {
                        nota = Double.parseDouble(notaTexto);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Ingresa una nota válida", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String comentario = inputComentario.getText().toString().trim();
                    executor.execute(() -> {
                        db.submissionDao().calificar(entrega.getId(), nota, comentario.isEmpty() ? null : comentario);
                        runOnUiThread(this::cargarEntregas);
                    });
                })
                .show();
    }

    private void abrirArchivo(Submission entrega) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(entrega.getUriArchivo()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(this)
                    .setTitle("No se pudo abrir")
                    .setMessage("No hay ninguna app en este dispositivo que pueda abrir este archivo.")
                    .setPositiveButton("Aceptar", null)
                    .show();
        }
    }

    private void descargarArchivo(Submission entrega) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Descargar solo está disponible en Android 10 o superior", Toast.LENGTH_LONG).show();
            return;
        }

        executor.execute(() -> {
            boolean exito = true;
            try {
                Uri origen = Uri.parse(entrega.getUriArchivo());
                InputStream in = getContentResolver().openInputStream(origen);
                if (in == null) throw new Exception("No se pudo abrir el archivo original");

                String mimeType = getContentResolver().getType(origen);
                if (mimeType == null) mimeType = "application/octet-stream";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, entrega.getNombreArchivo());
                values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri destino = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (destino == null) throw new Exception("No se pudo crear el archivo de destino");

                OutputStream out = getContentResolver().openOutputStream(destino);
                byte[] buffer = new byte[4096];
                int leidos;
                while ((leidos = in.read(buffer)) != -1) {
                    out.write(buffer, 0, leidos);
                }
                in.close();
                out.close();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(destino, values, null, null);

            } catch (Exception e) {
                exito = false;
            }

            boolean finalExito = exito;
            runOnUiThread(() -> Toast.makeText(this,
                    finalExito ? "Archivo descargado a tu carpeta de Descargas" : "No se pudo descargar el archivo",
                    Toast.LENGTH_LONG).show());
        });
    }
}