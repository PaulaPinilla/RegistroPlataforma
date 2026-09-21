package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Question;
import co.edu.unipiloto.registroplataforma.data.QuizResult;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityQuizResultListBinding;

public class QuizResultListActivity extends AppCompatActivity {

    private ActivityQuizResultListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private int actividadId;
    private final List<QuizResult> resultadosActuales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizResultListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        String actividadTitulo = getIntent().getStringExtra("actividadTitulo");

        binding.tvTitulo.setText(actividadTitulo != null ? actividadTitulo : "Resultados");
        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> mostrarRespuestas(item.id));
        binding.rvResultados.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResultados.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarResultados();
    }

    private void cargarResultados() {
        executor.execute(() -> {
            List<QuizResult> resultados = db.quizResultDao().listarPorActividad(actividadId);
            resultadosActuales.clear();
            resultadosActuales.addAll(resultados);

            Map<Integer, Integer> totalPorEstudiante = new HashMap<>();
            for (QuizResult r : resultados) {
                totalPorEstudiante.merge(r.getEstudianteId(), 1, Integer::sum);
            }

            List<QuizResult> ascendente = new ArrayList<>(resultados);
            Collections.reverse(ascendente);
            Map<Integer, Integer> intentoActual = new HashMap<>();
            Map<Integer, Integer> intentoDe = new HashMap<>();
            for (QuizResult r : ascendente) {
                int n = intentoActual.merge(r.getEstudianteId(), 1, Integer::sum);
                intentoDe.put(r.getId(), n);
            }

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (QuizResult r : resultados) {
                User estudiante = db.userDao().obtenerPorId(r.getEstudianteId());
                String nombre = estudiante != null ? estudiante.getNombre() : "Estudiante";
                int total = totalPorEstudiante.get(r.getEstudianteId());
                String subtitulo = "Intento " + intentoDe.get(r.getId()) + " de " + total + " — "
                        + formato.format(r.getFechaEnvio())
                        + (r.estaCalificada() ? " — Nota: " + r.getCalificacion() : " — Sin calificar");
                items.add(new SimpleListAdapter.Item(r.getId(), nombre, subtitulo, r.estaCalificada()));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private QuizResult buscarResultado(int id) {
        for (QuizResult r : resultadosActuales) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    private void mostrarRespuestas(int resultadoId) {
        QuizResult resultado = buscarResultado(resultadoId);
        if (resultado == null) return;

        executor.execute(() -> {
            List<Question> preguntas = db.questionDao().listarPorActividad(resultado.getActividadId());
            Map<Integer, String> respuestas = QuizAnswerActivity.parsearRespuestas(resultado.getRespuestas());

            StringBuilder texto = new StringBuilder();
            int numero = 1;
            for (Question p : preguntas) {
                texto.append(numero).append(". ").append(p.getEnunciado()).append("\n");
                String respuesta = respuestas.get(p.getId());
                texto.append("→ ").append(respuesta != null ? respuesta : "(sin responder)").append("\n\n");
                numero++;
            }

            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("Respuestas del estudiante")
                    .setMessage(texto.toString())
                    .setNegativeButton("Cerrar", null)
                    .setPositiveButton("Calificar", (d, w) -> mostrarDialogoCalificar(resultado))
                    .show());
        });
    }

    private void mostrarDialogoCalificar(QuizResult resultado) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        final EditText inputNota = new EditText(this);
        inputNota.setHint("Nota (0.0 a 5.0)");
        inputNota.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (resultado.getCalificacion() != null) inputNota.setText(String.valueOf(resultado.getCalificacion()));
        layout.addView(inputNota);

        final EditText inputComentario = new EditText(this);
        inputComentario.setHint("Comentario (opcional)");
        if (resultado.getRetroalimentacion() != null) inputComentario.setText(resultado.getRetroalimentacion());
        layout.addView(inputComentario);

        new AlertDialog.Builder(this)
                .setTitle("Calificar evaluación")
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
                        db.quizResultDao().calificar(resultado.getId(), nota, comentario.isEmpty() ? null : comentario);
                        runOnUiThread(this::cargarResultados);
                    });
                })
                .show();
    }
}