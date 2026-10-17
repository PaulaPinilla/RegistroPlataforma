package co.edu.unipiloto.registroplataforma;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumFormBinding;

public class ForumFormActivity extends AppCompatActivity {

    private ActivityForumFormBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int cursoId;

    private final Calendar fechaApertura = Calendar.getInstance();
    private final Calendar fechaCierre = Calendar.getInstance();
    private boolean aperturaFechaSeleccionada = false;
    private boolean aperturaHoraSeleccionada = false;
    private boolean cierreFechaSeleccionada = false;
    private boolean cierreHoraSeleccionada = false;

    private static final String[] CORTES = {"Corte 1", "Corte 2", "Corte 3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);

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

        binding.btnVolver.setOnClickListener(v -> finish());

        binding.etFechaApertura.setOnClickListener(v -> mostrarSelectorFecha(true));
        binding.etHoraApertura.setOnClickListener(v -> mostrarSelectorHora(true));
        binding.etFechaCierre.setOnClickListener(v -> mostrarSelectorFecha(false));
        binding.etHoraCierre.setOnClickListener(v -> mostrarSelectorHora(false));

        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    /** Porcentaje ya usado por actividades y foros en un corte del curso. Se llama en segundo plano. */
    private double porcentajeUsadoEnCorte(int corte) {
        if (cursoId == -1) return 0;
        return db.assignmentDao().sumarPorcentajeActividadesCorte(cursoId, corte, -1)
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

    private void mostrarSelectorFecha(boolean esApertura) {
        Calendar base = esApertura ? fechaApertura : fechaCierre;
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            base.set(Calendar.YEAR, year);
            base.set(Calendar.MONTH, month);
            base.set(Calendar.DAY_OF_MONTH, day);
            if (esApertura) {
                aperturaFechaSeleccionada = true;
                binding.etFechaApertura.setText(formatoFecha.format(base.getTime()));
            } else {
                cierreFechaSeleccionada = true;
                binding.etFechaCierre.setText(formatoFecha.format(base.getTime()));
            }
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void mostrarSelectorHora(boolean esApertura) {
        Calendar base = esApertura ? fechaApertura : fechaCierre;
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            base.set(Calendar.HOUR_OF_DAY, hourOfDay);
            base.set(Calendar.MINUTE, minute);
            String horaTexto = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (esApertura) {
                aperturaHoraSeleccionada = true;
                binding.etHoraApertura.setText(horaTexto);
            } else {
                cierreHoraSeleccionada = true;
                binding.etHoraCierre.setText(horaTexto);
            }
        }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void guardar() {
        binding.tilTema.setError(null);
        binding.tilFechaCierre.setError(null);
        binding.tilPorcentaje.setError(null);

        String tema = binding.etTema.getText().toString().trim();
        String porcentajeTexto = binding.etPorcentaje.getText().toString().trim().replace(',', '.');

        boolean valido = true;

        if (tema.isEmpty()) {
            binding.tilTema.setError("Escribe el tema del foro");
            valido = false;
        }

        double porcentaje = 0;
        if (porcentajeTexto.isEmpty()) {
            binding.tilPorcentaje.setError("Indica cuánto vale este foro dentro del corte");
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

        int corte = binding.spCorte.getSelectedItemPosition() + 1;

        long apertura = 0;
        if (aperturaFechaSeleccionada) {
            if (!aperturaHoraSeleccionada) {
                fechaApertura.set(Calendar.HOUR_OF_DAY, 0);
                fechaApertura.set(Calendar.MINUTE, 0);
            }
            fechaApertura.set(Calendar.SECOND, 0);
            fechaApertura.set(Calendar.MILLISECOND, 0);
            apertura = fechaApertura.getTimeInMillis();
        }

        long cierre = 0;
        if (cierreFechaSeleccionada) {
            if (!cierreHoraSeleccionada) {
                fechaCierre.set(Calendar.HOUR_OF_DAY, 23);
                fechaCierre.set(Calendar.MINUTE, 59);
            }
            fechaCierre.set(Calendar.SECOND, 59);
            fechaCierre.set(Calendar.MILLISECOND, 999);
            cierre = fechaCierre.getTimeInMillis();
        }

        if (apertura > 0 && cierre > 0 && apertura > cierre) {
            binding.tilFechaCierre.setError("Debe ser después de la apertura");
            valido = false;
        }

        if (!valido) return;

        long aperturaFinal = apertura;
        long cierreFinal = cierre;
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

            Forum nuevo = new Forum(cursoId, tema, "", System.currentTimeMillis(), aperturaFinal,
                    cierreFinal, corteFinal);
            nuevo.setPorcentaje(porcentajeFinal);
            db.forumDao().insertar(nuevo);

            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Foro creado")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .show());
        });
    }
}