package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    private int cursoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void guardar() {
        binding.tilTituloActividad.setError(null);
        binding.tilInstrucciones.setError(null);
        binding.tilFechaLimite.setError(null);

        String titulo = binding.etTituloActividad.getText().toString().trim();
        String instrucciones = binding.etInstrucciones.getText().toString().trim();
        String fechaTexto = binding.etFechaLimite.getText().toString().trim();

        boolean valido = true;

        if (titulo.length() < 3) {
            binding.tilTituloActividad.setError("Escribe un título para la actividad");
            valido = false;
        }

        if (instrucciones.isEmpty()) {
            binding.tilInstrucciones.setError("Describe las instrucciones");
            valido = false;
        }

        long fechaLimite = 0;
        try {
            formato.setLenient(false);
            Date fecha = formato.parse(fechaTexto);
            fechaLimite = fecha.getTime();
        } catch (ParseException | NullPointerException e) {
            binding.tilFechaLimite.setError("Usa el formato DD/MM/AAAA (ej: 25/12/2026)");
            valido = false;
        }

        if (!valido) return;

        long fechaLimiteFinal = fechaLimite;

        executor.execute(() -> {
            db.assignmentDao().insertar(new Assignment(cursoId, titulo, instrucciones, fechaLimiteFinal));
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Actividad creada 🎉")
                    .setMessage("Ya está disponible para los estudiantes inscritos.")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .show());
        });
    }
}