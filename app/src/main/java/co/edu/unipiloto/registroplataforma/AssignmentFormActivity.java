package co.edu.unipiloto.registroplataforma;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

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
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAssignmentFormBinding;

public class AssignmentFormActivity extends AppCompatActivity {

    private ActivityAssignmentFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int unidadId;
    private int cursoId;
    private int actividadId;
    private boolean modoEdicion;

    // Curso al que pertenece la actividad (se carga al abrir la pantalla)
    private volatile Course curso;
    private volatile boolean cursoCargado = false;

    private Integer horaInicioHora;
    private Integer horaInicioMinuto;
    private Integer horaLimiteHora;
    private Integer horaLimiteMinuto;

    private static final String[] CATEGORIAS = {"Taller", "Parcial", "Quiz", "Foro", "Video", "Preguntas"};
    private static final String[] CATEGORIA_VALORES = {
            Assignment.CATEGORIA_TALLER, Assignment.CATEGORIA_PARCIAL, Assignment.CATEGORIA_QUIZ,
            Assignment.CATEGORIA_FORO, Assignment.CATEGORIA_VIDEO, Assignment.CATEGORIA_PREGUNTAS
    };

    private static final String[] CORTES = {"Corte 1", "Corte 2", "Corte 3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        modoEdicion = actividadId != -1;

        setTitle(modoEdicion ? "Editar actividad" : "Nueva actividad");

        ArrayAdapter<String> adaptadorCategorias = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CATEGORIAS);
        adaptadorCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCategoria.setAdapter(adaptadorCategorias);

        ArrayAdapter<String> adaptadorCortes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, CORTES);
        adaptadorCortes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCorte.setAdapter(adaptadorCortes);

        // Cada vez que cambia el corte se recalcula cuánto porcentaje queda disponible
        binding.spCorte.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarPorcentajeDisponible();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        binding.etIntentosPermitidos.setText("1");

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.etHoraInicio.setOnClickListener(v -> mostrarSelectorHora(true));
        binding.etHoraLimite.setOnClickListener(v -> mostrarSelectorHora(false));
        binding.btnGuardar.setOnClickListener(v -> guardar());

        cargarCurso();

        if (modoEdicion) {
            binding.btnGuardar.setText("Guardar cambios");
            cargarActividadExistente();
        }
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    /** Porcentaje ya usado por otras actividades y foros en un corte del curso. Se llama en segundo plano. */
    private double porcentajeUsadoEnCorte(int corte) {
        if (cursoId == -1) return 0;
        int excluirId = modoEdicion ? actividadId : -1;
        return db.assignmentDao().sumarPorcentajeActividadesCorte(cursoId, corte, excluirId)
                + db.assignmentDao().sumarPorcentajeForosCorte(cursoId, corte);
    }

    private void actualizarPorcentajeDisponible() {
        if (cursoId == -1) {
            binding.tvPorcentajeDisponible.setText("");
            return;
        }
        final int corte = binding.spCorte.getSelectedItemPosition() + 1;
        executor.execute(() -> {
            double usado = porcentajeUsadoEnCorte(corte);
            double disponible = Math.max(0, 100 - usado);
            String texto = "Corte " + corte + ": ya asignado " + formatearPorcentaje(usado)
                    + "%, disponible " + formatearPorcentaje(disponible) + "%";
            runOnUiThread(() -> binding.tvPorcentajeDisponible.setText(texto));
        });
    }

    /** Carga el curso y, si ya terminó, avisa y cierra la pantalla. */
    private void cargarCurso() {
        executor.execute(() -> {
            if (cursoId != -1) {
                curso = db.courseDao().obtenerPorId(cursoId);
            }
            cursoCargado = true;
            if (curso != null && curso.yaTermino()) {
                runOnUiThread(this::avisarCursoTerminadoYSalir);
            }
        });
    }

    private void avisarCursoTerminadoYSalir() {
        if (isFinishing()) return;
        Course c = curso;
        String fin = c != null ? formato.format(new Date(c.getFechaFinalizacion())) : "";
        new AlertDialog.Builder(this)
                .setTitle("Curso finalizado")
                .setMessage("Este curso finalizó el " + fin
                        + ". Ya no se pueden crear, editar ni eliminar actividades.")
                .setCancelable(false)
                .setPositiveButton("Entendido", (d, w) -> finish())
                .show();
    }

    private void cargarActividadExistente() {
        executor.execute(() -> {
            Assignment a = db.assignmentDao().obtenerPorId(actividadId);
            if (a == null) return;

            runOnUiThread(() -> {
                binding.etTituloActividad.setText(a.getTitulo());
                binding.etInstrucciones.setText(a.getInstrucciones());
                binding.etIntentosPermitidos.setText(String.valueOf(a.getIntentosPermitidos()));
                binding.cbMostrarCalificacion.setChecked(a.isMostrarCalificacion());

                if (a.getPorcentaje() > 0) {
                    binding.etPorcentaje.setText(formatearPorcentaje(a.getPorcentaje()));
                }

                for (int i = 0; i < CATEGORIA_VALORES.length; i++) {
                    if (CATEGORIA_VALORES[i].equals(a.getCategoria())) {
                        binding.spCategoria.setSelection(i);
                        break;
                    }
                }
                binding.spCorte.setSelection(Math.max(0, a.getCorte() - 1));
                actualizarPorcentajeDisponible();

                if (Assignment.TIPO_ENTREGA_TEXTO.equals(a.getTipoEntrega())) {
                    binding.rbTipoTexto.setChecked(true);
                } else if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(a.getTipoEntrega())) {
                    binding.rbTipoPreguntas.setChecked(true);
                } else {
                    binding.rbTipoArchivo.setChecked(true);
                }

                if (a.getFechaInicio() > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(a.getFechaInicio());
                    binding.etFechaInicio.setText(formato.format(cal.getTime()));
                    horaInicioHora = cal.get(Calendar.HOUR_OF_DAY);
                    horaInicioMinuto = cal.get(Calendar.MINUTE);
                    binding.etHoraInicio.setText(String.format(Locale.getDefault(), "%02d:%02d", horaInicioHora, horaInicioMinuto));
                }

                Calendar calLimite = Calendar.getInstance();
                calLimite.setTimeInMillis(a.getFechaLimite());
                binding.etFechaLimite.setText(formato.format(calLimite.getTime()));
                horaLimiteHora = calLimite.get(Calendar.HOUR_OF_DAY);
                horaLimiteMinuto = calLimite.get(Calendar.MINUTE);
                binding.etHoraLimite.setText(String.format(Locale.getDefault(), "%02d:%02d", horaLimiteHora, horaLimiteMinuto));
            });
        });
    }

    private void mostrarSelectorHora(boolean esInicio) {
        Calendar ahora = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String horaTexto = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (esInicio) {
                horaInicioHora = hourOfDay;
                horaInicioMinuto = minute;
                binding.etHoraInicio.setText(horaTexto);
            } else {
                horaLimiteHora = hourOfDay;
                horaLimiteMinuto = minute;
                binding.etHoraLimite.setText(horaTexto);
            }
        }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void guardar() {
        // El curso todavía se está cargando
        if (cursoId != -1 && !cursoCargado) {
            Toast.makeText(this, "Cargando datos del curso, intenta de nuevo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si el curso terminó mientras la pantalla estaba abierta, no se guarda nada
        Course cursoActual = curso;
        if (cursoActual != null && cursoActual.yaTermino()) {
            avisarCursoTerminadoYSalir();
            return;
        }

        binding.tilTituloActividad.setError(null);
        binding.tilInstrucciones.setError(null);
        binding.tilFechaInicio.setError(null);
        binding.tilFechaLimite.setError(null);
        binding.tilIntentosPermitidos.setError(null);
        binding.tilPorcentaje.setError(null);

        String titulo = binding.etTituloActividad.getText().toString().trim();
        String instrucciones = binding.etInstrucciones.getText().toString().trim();
        String fechaInicioTexto = binding.etFechaInicio.getText().toString().trim();
        String fechaLimiteTexto = binding.etFechaLimite.getText().toString().trim();
        String intentosTexto = binding.etIntentosPermitidos.getText().toString().trim();
        String porcentajeTexto = binding.etPorcentaje.getText().toString().trim().replace(',', '.');

        String categoria = CATEGORIA_VALORES[binding.spCategoria.getSelectedItemPosition()];
        int corte = binding.spCorte.getSelectedItemPosition() + 1;

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

        double porcentaje = 0;
        if (porcentajeTexto.isEmpty()) {
            binding.tilPorcentaje.setError("Indica cuánto vale esta actividad dentro del corte");
            valido = false;
        } else {
            try {
                porcentaje = Double.parseDouble(porcentajeTexto);
                if (porcentaje <= 0 || porcentaje > 100) {
                    binding.tilPorcentaje.setError("Debe ser mayor que 0 y máximo 100");
                    valido = false;
                }
            } catch (NumberFormatException e) {
                binding.tilPorcentaje.setError("Escribe un número válido");
                valido = false;
            }
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
                if (horaInicioHora != null) {
                    cal.set(Calendar.HOUR_OF_DAY, horaInicioHora);
                    cal.set(Calendar.MINUTE, horaInicioMinuto);
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                }
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                fechaInicio = cal.getTimeInMillis();
            } catch (ParseException | NullPointerException e) {
                binding.tilFechaInicio.setError("Usa el formato DD/MM/AAAA (ej: 25/12/2026)");
                valido = false;
            }
        }

        long fechaLimite = 0;
        boolean fechaLimiteValida = true;
        try {
            formato.setLenient(false);
            Date fecha = formato.parse(fechaLimiteTexto);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fecha);
            if (horaLimiteHora != null) {
                cal.set(Calendar.HOUR_OF_DAY, horaLimiteHora);
                cal.set(Calendar.MINUTE, horaLimiteMinuto);
                cal.set(Calendar.SECOND, 59);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
            }
            cal.set(Calendar.MILLISECOND, 999);
            fechaLimite = cal.getTimeInMillis();
        } catch (ParseException | NullPointerException e) {
            binding.tilFechaLimite.setError("Usa el formato DD/MM/AAAA (ej: 25/12/2026)");
            valido = false;
            fechaLimiteValida = false;
        }

        if (valido && fechaInicio > 0 && fechaInicio > fechaLimite) {
            binding.tilFechaInicio.setError("Debe ser antes de la fecha límite");
            valido = false;
        }

        // La entrega no puede quedar después de que el curso termina
        if (fechaLimiteValida && cursoActual != null && cursoActual.getFechaFinalizacion() > 0
                && fechaLimite > cursoActual.getFechaFinalizacion()) {
            binding.tilFechaLimite.setError("No puede ser posterior al fin del curso ("
                    + formato.format(new Date(cursoActual.getFechaFinalizacion())) + ")");
            valido = false;
        }

        if (!valido) return;

        long fechaInicioFinal = fechaInicio;
        long fechaLimiteFinal = fechaLimite;
        String tipoEntregaFinal = tipoEntrega;
        String tituloFinal = titulo;
        int intentosFinal = intentosPermitidos;
        int corteFinal = corte;
        double porcentajeFinal = porcentaje;

        executor.execute(() -> {
            // Validación del porcentaje: el corte no puede pasar de 100%
            double usado = porcentajeUsadoEnCorte(corteFinal);
            double disponible = Math.max(0, 100 - usado);
            if (porcentajeFinal > disponible + 0.0001) {
                runOnUiThread(() -> binding.tilPorcentaje.setError("En el Corte " + corteFinal
                        + " solo quedan " + formatearPorcentaje(disponible) + "% disponibles"));
                return;
            }

            if (modoEdicion) {
                Assignment actualizada = new Assignment(unidadId, tituloFinal, instrucciones, fechaInicioFinal,
                        fechaLimiteFinal, tipoEntregaFinal, categoria, mostrarCalificacion, intentosFinal, corteFinal);
                actualizada.setId(actividadId);
                actualizada.setPorcentaje(porcentajeFinal);

                // Se conserva la fecha en que la actividad fue creada originalmente
                Assignment existente = db.assignmentDao().obtenerPorId(actividadId);
                if (existente != null) {
                    actualizada.setFechaCreacion(existente.getFechaCreacion());
                }

                db.assignmentDao().actualizar(actualizada);
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Actividad actualizada")
                        .setMessage("Los cambios se guardaron correctamente.")
                        .setCancelable(false)
                        .setPositiveButton("Aceptar", (d, w) -> finish())
                        .show());
            } else {
                Assignment nueva = new Assignment(unidadId, tituloFinal, instrucciones,
                        fechaInicioFinal, fechaLimiteFinal, tipoEntregaFinal, categoria, mostrarCalificacion,
                        intentosFinal, corteFinal);
                nueva.setPorcentaje(porcentajeFinal);
                nueva.setFechaCreacion(System.currentTimeMillis());
                long idGenerado = db.assignmentDao().insertar(nueva);

                runOnUiThread(() -> {
                    if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(tipoEntregaFinal)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Actividad creada ")
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
                                .setTitle("Actividad creada ")
                                .setMessage("Ya está disponible para los estudiantes inscritos en esta unidad.")
                                .setCancelable(false)
                                .setPositiveButton("Aceptar", (d, w) -> finish())
                                .show();
                    }
                });
            }
        });
    }
}