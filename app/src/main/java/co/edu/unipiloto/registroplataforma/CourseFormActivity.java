package co.edu.unipiloto.registroplataforma;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityCourseFormBinding;

public class CourseFormActivity extends AppCompatActivity {

    private ActivityCourseFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int profesorId;
    private String profesorNombre;
    private int cursoId = -1;
    private Course cursoExistente;

    private final Calendar fechaFinalizacion = Calendar.getInstance();
    private boolean fechaFinalizacionSeleccionada = false;
    private boolean horaFinalizacionSeleccionada = false;

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

        binding.etFechaFinalizacion.setOnClickListener(v -> mostrarSelectorFecha());
        binding.etHoraFinalizacion.setOnClickListener(v -> mostrarSelectorHora());

        TextWatcher vigilante = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { actualizarSumaPesos(); }
        };
        binding.etPesoCorte1.addTextChangedListener(vigilante);
        binding.etPesoCorte2.addTextChangedListener(vigilante);
        binding.etPesoCorte3.addTextChangedListener(vigilante);

        if (esEdicion) {
            cargarCurso();
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void mostrarSelectorFecha() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            // Solo cambia el día; la hora que ya se eligió se conserva
            fechaFinalizacion.set(year, month, day);
            fechaFinalizacionSeleccionada = true;
            binding.etFechaFinalizacion.setText(formatoFecha.format(fechaFinalizacion.getTime()));
            binding.tilFechaFinalizacion.setError(null);
        }, fechaFinalizacion.get(Calendar.YEAR), fechaFinalizacion.get(Calendar.MONTH), fechaFinalizacion.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void mostrarSelectorHora() {
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            fechaFinalizacion.set(Calendar.HOUR_OF_DAY, hourOfDay);
            fechaFinalizacion.set(Calendar.MINUTE, minute);
            fechaFinalizacion.set(Calendar.SECOND, 0);
            fechaFinalizacion.set(Calendar.MILLISECOND, 0);
            horaFinalizacionSeleccionada = true;
            binding.etHoraFinalizacion.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            binding.tilHoraFinalizacion.setError(null);
        }, fechaFinalizacion.get(Calendar.HOUR_OF_DAY), fechaFinalizacion.get(Calendar.MINUTE), true);
        dialog.show();
    }

    /** Lee un campo numérico. Devuelve -1 si está vacío o no es un número válido. */
    private double leerPorcentaje(EditText campo) {
        String texto = campo.getText().toString().trim().replace(',', '.');
        if (texto.isEmpty()) return -1;
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatearPorcentaje(double valor) {
        if (valor == Math.floor(valor)) {
            return String.valueOf((int) valor);
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    private void actualizarSumaPesos() {
        double p1 = Math.max(0, leerPorcentaje(binding.etPesoCorte1));
        double p2 = Math.max(0, leerPorcentaje(binding.etPesoCorte2));
        double p3 = Math.max(0, leerPorcentaje(binding.etPesoCorte3));
        double suma = p1 + p2 + p3;
        binding.tvSumaPesos.setText("Suma actual: " + formatearPorcentaje(suma) + "% de 100%");
    }

    private void cargarCurso() {
        executor.execute(() -> {
            cursoExistente = db.courseDao().obtenerPorId(cursoId);
            if (cursoExistente == null) return;
            runOnUiThread(() -> {
                binding.etTituloCurso.setText(cursoExistente.getTitulo());
                binding.etDescripcionCurso.setText(cursoExistente.getDescripcion());
                binding.etContrasenaCurso.setText(cursoExistente.getContrasenaCurso());
                binding.etSemestre.setText(cursoExistente.getSemestre());
                if (cursoExistente.getFechaFinalizacion() > 0) {
                    fechaFinalizacion.setTimeInMillis(cursoExistente.getFechaFinalizacion());
                    fechaFinalizacionSeleccionada = true;
                    horaFinalizacionSeleccionada = true;
                    binding.etFechaFinalizacion.setText(formatoFecha.format(fechaFinalizacion.getTime()));
                    binding.etHoraFinalizacion.setText(String.format(Locale.getDefault(), "%02d:%02d",
                            fechaFinalizacion.get(Calendar.HOUR_OF_DAY), fechaFinalizacion.get(Calendar.MINUTE)));
                }
                // Si el curso ya tenía los pesos definidos se muestran; si es un curso anterior quedan vacíos
                if (cursoExistente.tienePesosCortesValidos()) {
                    binding.etPesoCorte1.setText(formatearPorcentaje(cursoExistente.getPesoCorte1()));
                    binding.etPesoCorte2.setText(formatearPorcentaje(cursoExistente.getPesoCorte2()));
                    binding.etPesoCorte3.setText(formatearPorcentaje(cursoExistente.getPesoCorte3()));
                }
            });
        });
    }

    private void guardar() {
        binding.tilTituloCurso.setError(null);
        binding.tilDescripcionCurso.setError(null);
        binding.tilContrasenaCurso.setError(null);
        binding.tilFechaFinalizacion.setError(null);
        binding.tilHoraFinalizacion.setError(null);
        binding.tilPesoCorte1.setError(null);
        binding.tilPesoCorte2.setError(null);
        binding.tilPesoCorte3.setError(null);

        String titulo = binding.etTituloCurso.getText().toString().trim();
        String descripcion = binding.etDescripcionCurso.getText().toString().trim();
        String contrasena = binding.etContrasenaCurso.getText().toString().trim();
        String semestre = binding.etSemestre.getText().toString().trim();

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

        if (!fechaFinalizacionSeleccionada) {
            binding.tilFechaFinalizacion.setError("Selecciona la fecha en que termina el curso");
            valido = false;
        }

        if (!horaFinalizacionSeleccionada) {
            binding.tilHoraFinalizacion.setError("Selecciona la hora exacta en que termina el curso");
            valido = false;
        }

        double peso1 = leerPorcentaje(binding.etPesoCorte1);
        double peso2 = leerPorcentaje(binding.etPesoCorte2);
        double peso3 = leerPorcentaje(binding.etPesoCorte3);

        if (peso1 < 0 || peso1 > 100) {
            binding.tilPesoCorte1.setError("Escribe un valor entre 0 y 100");
            valido = false;
        }
        if (peso2 < 0 || peso2 > 100) {
            binding.tilPesoCorte2.setError("Escribe un valor entre 0 y 100");
            valido = false;
        }
        if (peso3 < 0 || peso3 > 100) {
            binding.tilPesoCorte3.setError("Escribe un valor entre 0 y 100");
            valido = false;
        }

        if (peso1 >= 0 && peso2 >= 0 && peso3 >= 0) {
            double suma = peso1 + peso2 + peso3;
            if (Math.abs(suma - 100.0) > 0.001) {
                binding.tilPesoCorte3.setError("Los cortes suman " + formatearPorcentaje(suma)
                        + "%. Deben sumar exactamente 100%");
                valido = false;
            }
        }

        if (!valido) return;

        boolean esEdicion = cursoId != -1;
        long fechaFinalizacionFinal = fechaFinalizacion.getTimeInMillis();

        executor.execute(() -> {
            if (esEdicion && cursoExistente != null) {
                cursoExistente.setTitulo(titulo);
                cursoExistente.setDescripcion(descripcion);
                cursoExistente.setContrasenaCurso(contrasena);
                cursoExistente.setSemestre(semestre);
                cursoExistente.setFechaFinalizacion(fechaFinalizacionFinal);
                cursoExistente.setPesoCorte1(peso1);
                cursoExistente.setPesoCorte2(peso2);
                cursoExistente.setPesoCorte3(peso3);
                db.courseDao().actualizar(cursoExistente);
            } else {
                Course nuevo = new Course(titulo, descripcion, profesorId, profesorNombre, contrasena,
                        semestre, fechaFinalizacionFinal);
                nuevo.setPesoCorte1(peso1);
                nuevo.setPesoCorte2(peso2);
                nuevo.setPesoCorte3(peso3);
                db.courseDao().insertar(nuevo);
            }
            runOnUiThread(() -> mostrarConfirmacion(esEdicion));
        });
    }

    private void mostrarConfirmacion(boolean esEdicion) {
        new AlertDialog.Builder(this)
                .setTitle(esEdicion ? "Curso actualizado" : "Curso creado")
                .setMessage(esEdicion
                        ? "Los cambios se guardaron correctamente."
                        : "El curso ya está disponible para que los estudiantes se inscriban.")
                .setCancelable(false)
                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                .show();
    }
}