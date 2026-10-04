package co.edu.unipiloto.registroplataforma;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());

        binding.etFechaApertura.setOnClickListener(v -> mostrarSelectorFecha(true));
        binding.etHoraApertura.setOnClickListener(v -> mostrarSelectorHora(true));
        binding.etFechaCierre.setOnClickListener(v -> mostrarSelectorFecha(false));
        binding.etHoraCierre.setOnClickListener(v -> mostrarSelectorHora(false));

        binding.btnGuardar.setOnClickListener(v -> guardar());
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

        String tema = binding.etTema.getText().toString().trim();
        if (tema.isEmpty()) {
            binding.tilTema.setError("Escribe el tema del foro");
            return;
        }

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
            return;
        }

        long aperturaFinal = apertura;
        long cierreFinal = cierre;

        executor.execute(() -> {
            db.forumDao().insertar(new Forum(cursoId, tema, "", System.currentTimeMillis(), aperturaFinal, cierreFinal));
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Foro creado 🎉")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .show());
        });
    }
}