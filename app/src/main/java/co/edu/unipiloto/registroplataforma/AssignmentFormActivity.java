package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAssignmentFormBinding;

public class AssignmentFormActivity extends AppCompatActivity {

    private ActivityAssignmentFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int unidadId;

    private static final String[] CATEGORIAS = {"Taller", "Parcial", "Quiz", "Foro", "Video", "Preguntas"};
    private static final String[] CATEGORIA_VALORES = {
            Assignment.CATEGORIA_TALLER, Assignment.CATEGORIA_PARCIAL, Assignment.CATEGORIA_QUIZ,
            Assignment.CATEGORIA_FORO, Assignment.CATEGORIA_VIDEO, Assignment.CATEGORIA_PREGUNTAS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);

        ArrayAdapter<String> adaptadorCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIAS);
        adaptadorCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCategoria.setAdapter(adaptadorCategorias);

        binding.etIntentosPermitidos.setText("1");

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        binding.tilTituloActividad.setError(null);
        binding.tilInstrucciones.setError(null);
        binding.tilFechaInicio.setError(null);
        binding.tilFechaLimite.setError(null);
        binding.tilIntentosPermitidos.setError(null);

        String titulo = binding.etTituloActividad.getText().toString().trim();
        String instrucciones = binding.etInstrucciones.getText().toString().trim();
        String fechaInicioTexto = binding.etFechaInicio.getText().toString().trim();
        String fechaLimiteTexto = binding.etFechaLimite.getText().toString().trim();
        String intentosTexto = binding.etIntentosPermitidos.getText().toString().trim();

        String categoria = CATEGORIA_VALORES[binding.spCategoria.getSelectedItemPosition()];

        String tipoEntrega;
        if (binding.rbTipoTexto.isChecked()) {
            tipoEntrega = Assignment.TIPO_ENTREGA_TEXTO;
        } else if (binding.rbTipoPreguntas.isChecked()) {
            tipoEntrega = Assignment.TIPO_ENTREGA_PREGUNTAS;
        } else {
            tipoEntrega = Assignment.TIPO_ENTREGA_ARCHIVO;
        }

        boolean mostrarCalificacion = binding.cbMostrarCalificacion.isChecked();

        boolean valido = true;

        if (titulo.length() < 3) {
            binding.tilTituloActividad.setError("Escribe un título para la actividad");
            valido = false;
        }

        if (instrucciones.isEmpty()) {
            binding.tilInstrucciones.setError("Describe las instrucciones");
            valido = false;
        }

        int intentosPermitidos = 1;
        if (!intentosTexto.isEmpty()) {
            try {
                intentosPermitidos = Integer.parseInt(intentosTexto);
                if (intentosPermitidos < 1) {
                    binding.tilIntentosPermitidos.setError("Debe ser al menos 1");
                    valido = false;
                }
            } catch (NumberFormatException e) {
                binding.tilIntentosPermitidos.setError("Escribe un número válido");
                valido = false;
            }
        }

        long fechaInicio = 0;
        if (!fechaInicioTexto.isEmpty()) {
            try {
                formato.setLenient(false);
                Date fecha = formato.parse(fechaInicioTexto);
                Calendar cal = Calendar.getInstance();
                cal.setTime(fecha);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                fechaInicio = cal.getTimeInMillis();
            } catch (ParseException | NullPointerException e) {
                binding.tilFechaInicio.setError("Usa el formato DD/MM/AAAA (ej: 25/12/2026)");
                valido = false;
            }
        }

        long fechaLimite = 0;
        try {
            formato.setLenient(false);
            Date fecha = formato.parse(fechaLimiteTexto);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fecha);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            fechaLimite = cal.getTimeInMillis();
        } catch (ParseException | NullPointerException e) {
            binding.tilFechaLimite.setError("Usa el formato DD/MM/AAAA (ej: 25/12/2026)");
            valido = false;
        }

        if (valido && fechaInicio > 0 && fechaInicio > fechaLimite) {
            binding.tilFechaInicio.setError("Debe ser antes de la fecha límite");
            valido = false;
        }

        if (!valido) return;

        long fechaInicioFinal = fechaInicio;
        long fechaLimiteFinal = fechaLimite;
        String tipoEntregaFinal = tipoEntrega;
        String tituloFinal = titulo;
        int intentosFinal = intentosPermitidos;

        executor.execute(() -> {
            long idGenerado = db.assignmentDao().insertar(new Assignment(unidadId, tituloFinal, instrucciones,
                    fechaInicioFinal, fechaLimiteFinal, tipoEntregaFinal, categoria, mostrarCalificacion, intentosFinal));

            runOnUiThread(() -> {
                if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(tipoEntregaFinal)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Actividad creada")
                            .setMessage("Ahora agrega las preguntas de esta evaluación.")
                            .setCancelable(false)
                            .setPositiveButton("Agregar preguntas", (d, w) -> {
                                Intent intent = new Intent(this, QuestionListActivity.class);
                                intent.putExtra("actividadId", (int) idGenerado);
                                intent.putExtra("actividadTitulo", tituloFinal);
                                startActivity(intent);
                                finish();
                            })
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Actividad creada")
                            .setMessage("Ya está disponible para los estudiantes inscritos en esta unidad.")
                            .setCancelable(false)
                            .setPositiveButton("Aceptar", (d, w) -> finish())
                            .show();
                }
            });
        });
    }
}