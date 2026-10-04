package co.edu.unipiloto.registroplataforma;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.ParticipacionEstudiante;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumReportBinding;

public class ForumReportActivity extends AppCompatActivity {

    private ActivityForumReportBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int cursoId;

    private final Calendar fechaDesde = Calendar.getInstance();
    private final Calendar fechaHasta = Calendar.getInstance();
    private boolean desdeSeleccionada = false;
    private boolean hastaSeleccionada = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.etFechaDesde.setOnClickListener(v -> mostrarSelectorFecha(true));
        binding.etFechaHasta.setOnClickListener(v -> mostrarSelectorFecha(false));
        binding.btnGenerarReporte.setOnClickListener(v -> generarReporte());
    }

    private void mostrarSelectorFecha(boolean esDesde) {
        Calendar base = esDesde ? fechaDesde : fechaHasta;
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            base.set(year, month, day, 0, 0, 0);
            if (esDesde) {
                desdeSeleccionada = true;
                binding.etFechaDesde.setText(formatoFecha.format(base.getTime()));
            } else {
                hastaSeleccionada = true;
                binding.etFechaHasta.setText(formatoFecha.format(base.getTime()));
            }
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void generarReporte() {
        if (!desdeSeleccionada || !hastaSeleccionada) {
            binding.tvError.setText("Selecciona la fecha de inicio y de fin");
            binding.tvError.setVisibility(android.view.View.VISIBLE);
            return;
        }

        long desde = truncarInicioDia(fechaDesde).getTimeInMillis();
        long hasta = truncarFinDia(fechaHasta).getTimeInMillis();

        if (desde > hasta) {
            binding.tvError.setText("La fecha de inicio debe ser antes que la fecha de fin");
            binding.tvError.setVisibility(android.view.View.VISIBLE);
            return;
        }

        binding.tvError.setVisibility(android.view.View.GONE);

        executor.execute(() -> {
            List<User> estudiantes = db.enrollmentDao().listarEstudiantesInscritos(cursoId);
            List<ParticipacionEstudiante> comentarios = db.forumPostDao().contarComentariosPorEstudiante(cursoId, desde, hasta);
            List<ParticipacionEstudiante> forosDistintos = db.forumPostDao().contarForosParticipadosPorEstudiante(cursoId, desde, hasta);

            Map<Integer, Integer> mapaComentarios = new HashMap<>();
            for (ParticipacionEstudiante p : comentarios) mapaComentarios.put(p.autorId, p.cantidad);

            Map<Integer, Integer> mapaForos = new HashMap<>();
            for (ParticipacionEstudiante p : forosDistintos) mapaForos.put(p.autorId, p.cantidad);

            int totalComentarios = 0;
            for (int c : mapaComentarios.values()) totalComentarios += c;

            StringBuilder resultado = new StringBuilder();
            resultado.append("TOTAL del curso: ").append(totalComentarios).append(" participaciones\n\n");

            for (User e : estudiantes) {
                int cant = mapaComentarios.getOrDefault(e.getId(), 0);
                int foros = mapaForos.getOrDefault(e.getId(), 0);
                resultado.append(e.getNombre()).append(" — ").append(cant)
                        .append(cant == 1 ? " participación" : " participaciones")
                        .append(" en ").append(foros).append(foros == 1 ? " foro" : " foros")
                        .append("\n");
            }

            if (estudiantes.isEmpty()) {
                resultado.append("Este curso todavía no tiene estudiantes inscritos.");
            }

            String textoFinal = resultado.toString();
            runOnUiThread(() -> {
                binding.tvResultado.setText(textoFinal);
                binding.tvResultado.setVisibility(android.view.View.VISIBLE);
            });
        });
    }

    private Calendar truncarInicioDia(Calendar original) {
        Calendar copia = (Calendar) original.clone();
        copia.set(Calendar.HOUR_OF_DAY, 0);
        copia.set(Calendar.MINUTE, 0);
        copia.set(Calendar.SECOND, 0);
        copia.set(Calendar.MILLISECOND, 0);
        return copia;
    }

    private Calendar truncarFinDia(Calendar original) {
        Calendar copia = (Calendar) original.clone();
        copia.set(Calendar.HOUR_OF_DAY, 23);
        copia.set(Calendar.MINUTE, 59);
        copia.set(Calendar.SECOND, 59);
        copia.set(Calendar.MILLISECOND, 999);
        return copia;
    }
}