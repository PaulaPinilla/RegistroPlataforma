package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Submission;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAssignmentDetailBinding;

public class AssignmentDetailActivity extends AppCompatActivity {

    private ActivityAssignmentDetailBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int actividadId;
    private int estudianteId;
    private Assignment actividadActual;
    private Submission entregaActual;
    private boolean plazoVencido;

    private Uri archivoSeleccionado;
    private String nombreArchivoSeleccionado;

    private ActivityResultLauncher<String[]> selectorArchivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

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
                    binding.btnEnviarActividad.setEnabled(true);
                });

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAdjuntarArchivo.setOnClickListener(v ->
                selectorArchivo.launch(new String[]{"application/pdf", "image/jpeg", "image/png"}));
        binding.btnEnviarActividad.setOnClickListener(v -> enviarActividad());
        binding.btnEliminarEntrega.setOnClickListener(v -> confirmarEliminarEntrega());

        binding.etRespuestaTexto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnEnviarActividad.setEnabled(s != null && s.toString().trim().length() > 0 && !plazoVencido);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        cargarActividad();
    }

    private void cargarActividad() {
        executor.execute(() -> {
            Assignment actividad = db.assignmentDao().obtenerPorId(actividadId);

            if (actividad != null && Assignment.TIPO_ENTREGA_PREGUNTAS.equals(actividad.getTipoEntrega())) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, QuizAnswerActivity.class);
                    intent.putExtra("actividadId", actividadId);
                    intent.putExtra("estudianteId", estudianteId);
                    startActivity(intent);
                    finish();
                });
                return;
            }

            Submission ultimaEntrega = db.submissionDao().obtenerUltimaEntrega(actividadId, estudianteId);

            runOnUiThread(() -> {
                if (actividad == null) {
                    finish();
                    return;
                }
                actividadActual = actividad;
                entregaActual = ultimaEntrega;
                plazoVencido = System.currentTimeMillis() > actividad.getFechaLimite();

                binding.tvTituloActividad.setText(AssignmentListActivity.categoriaLegible(actividad.getCategoria())
                        + " — " + actividad.getTitulo());
                binding.tvInstrucciones.setText(actividad.getInstrucciones());
                binding.tvFechaLimite.setText("Fecha límite: " + formatoFecha.format(actividad.getFechaLimite()));

                if (!actividad.yaComenzo()) {
                    binding.tvNoIniciado.setText("Disponible a partir del " + formatoFecha.format(actividad.getFechaInicio()));
                    binding.tvNoIniciado.setVisibility(View.VISIBLE);
                    binding.btnAdjuntarArchivo.setVisibility(View.GONE);
                    binding.tilRespuestaTexto.setVisibility(View.GONE);
                    binding.btnEnviarActividad.setVisibility(View.GONE);
                    return;
                }

                boolean esTexto = Assignment.TIPO_ENTREGA_TEXTO.equals(actividad.getTipoEntrega());
                binding.btnAdjuntarArchivo.setVisibility(esTexto ? View.GONE : View.VISIBLE);
                binding.tilRespuestaTexto.setVisibility(esTexto ? View.VISIBLE : View.GONE);

                actualizarEstado();

                if (plazoVencido) {
                    binding.btnAdjuntarArchivo.setEnabled(false);
                    binding.etRespuestaTexto.setEnabled(false);
                    binding.btnEnviarActividad.setEnabled(false);
                }
            });
        });
    }

    private void actualizarEstado() {
        StringBuilder texto = new StringBuilder();

        if (entregaActual != null) {
            if (entregaActual.esTexto()) {
                texto.append("Respuesta entregada (").append(formatoFecha.format(entregaActual.getFechaEntrega())).append(")");
            } else {
                texto.append(" Entregado: ").append(entregaActual.getNombreArchivo())
                        .append(" (").append(formatoFecha.format(entregaActual.getFechaEntrega())).append(")");
            }

            if (entregaActual.estaCalificada()) {
                texto.append("\n Calificación: ").append(entregaActual.getCalificacion());
                if (entregaActual.getRetroalimentacion() != null && !entregaActual.getRetroalimentacion().isEmpty()) {
                    texto.append("\n").append(entregaActual.getRetroalimentacion());
                }
            } else if (!plazoVencido) {
                texto.append("\nPuedes eliminarla y subir otra mientras no venza el plazo.");
            }
        }

        if (plazoVencido) {
            if (texto.length() > 0) texto.append("\n");
            texto.append("El plazo para entregar venció.");
        }

        if (texto.length() > 0) {
            binding.tvEstadoEntrega.setText(texto.toString());
            binding.tvEstadoEntrega.setVisibility(View.VISIBLE);
        } else {
            binding.tvEstadoEntrega.setVisibility(View.GONE);
        }

        binding.btnEliminarEntrega.setVisibility(
                (entregaActual != null && !plazoVencido && !entregaActual.estaCalificada()) ? View.VISIBLE : View.GONE);
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

    private void confirmarEliminarEntrega() {
        if (entregaActual == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar entrega")
                .setMessage("¿Seguro que quieres eliminar tu entrega? Podrás subir otra después.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarEntrega())
                .show();
    }

    private void eliminarEntrega() {
        Submission aEliminar = entregaActual;
        executor.execute(() -> {
            db.submissionDao().eliminar(aEliminar);
            runOnUiThread(() -> {
                entregaActual = null;
                actualizarEstado();
            });
        });
    }

    private void enviarActividad() {
        if (actividadActual == null) return;

        boolean esTexto = Assignment.TIPO_ENTREGA_TEXTO.equals(actividadActual.getTipoEntrega());
        String respuestaTexto = binding.etRespuestaTexto.getText().toString().trim();

        if (esTexto && respuestaTexto.isEmpty()) return;
        if (!esTexto && archivoSeleccionado == null) return;

        if (System.currentTimeMillis() > actividadActual.getFechaLimite()) {
            new AlertDialog.Builder(this)
                    .setTitle("Plazo vencido")
                    .setMessage("Ya no puedes entregar esta actividad, el plazo venció.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        binding.btnEnviarActividad.setEnabled(false);

        executor.execute(() -> {
            long ahora = System.currentTimeMillis();
            Submission entrega;
            if (esTexto) {
                entrega = new Submission(actividadId, estudianteId, null, null, respuestaTexto, ahora);
            } else {
                entrega = new Submission(actividadId, estudianteId,
                        archivoSeleccionado.toString(), nombreArchivoSeleccionado, null, ahora);
            }
            long idGenerado = db.submissionDao().insertar(entrega);
            entrega.setId((int) idGenerado);

            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("¡Entrega exitosa! ")
                    .setMessage("Tu entrega se envió correctamente.")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> {
                        entregaActual = entrega;
                        actualizarEstado();
                        archivoSeleccionado = null;
                        nombreArchivoSeleccionado = null;
                        binding.tvArchivoSeleccionado.setVisibility(View.GONE);
                        binding.etRespuestaTexto.setText("");
                        binding.btnEnviarActividad.setEnabled(false);
                    })
                    .show());
        });
    }
}