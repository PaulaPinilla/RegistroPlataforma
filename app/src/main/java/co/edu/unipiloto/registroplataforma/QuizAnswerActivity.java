package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Question;
import co.edu.unipiloto.registroplataforma.data.QuizResult;
import co.edu.unipiloto.registroplataforma.databinding.ActivityQuizAnswerBinding;

public class QuizAnswerActivity extends AppCompatActivity {

    private static final char SEP_PREGUNTA = '\u0001';
    private static final char SEP_RESPUESTA = '\u0002';

    private ActivityQuizAnswerBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int actividadId;
    private int estudianteId;
    private Assignment actividadActual;

    private final Map<Integer, RadioGroup> gruposOpciones = new HashMap<>();
    private final Map<Integer, EditText> camposTexto = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizAnswerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnEnviarEvaluacion.setOnClickListener(v -> enviarEvaluacion());

        cargarEvaluacion();
    }

    private void cargarEvaluacion() {
        executor.execute(() -> {
            Assignment actividad = db.assignmentDao().obtenerPorId(actividadId);
            List<Question> preguntas = db.questionDao().listarPorActividad(actividadId);
            QuizResult resultado = db.quizResultDao().obtenerUltimoResultado(actividadId, estudianteId);
            int intentosUsados = db.quizResultDao().contarIntentos(actividadId, estudianteId);

            Map<Integer, String> respuestasPrevias = new HashMap<>();
            if (resultado != null) {
                respuestasPrevias.putAll(parsearRespuestas(resultado.getRespuestas()));
            }

            runOnUiThread(() -> {
                if (actividad == null) {
                    finish();
                    return;
                }
                actividadActual = actividad;

                binding.tvTituloActividad.setText(AssignmentListActivity.categoriaLegible(actividad.getCategoria())
                        + " — " + actividad.getTitulo());
                binding.tvInstrucciones.setText(actividad.getInstrucciones());
                binding.tvFechaLimite.setText("Fecha límite: " + formatoFecha.format(actividad.getFechaLimite()));

                if (!actividad.yaComenzo()) {
                    binding.tvNoIniciado.setText("Disponible a partir del " + formatoFecha.format(actividad.getFechaInicio()));
                    binding.tvNoIniciado.setVisibility(View.VISIBLE);
                    binding.contenedorPreguntas.setVisibility(View.GONE);
                    binding.btnEnviarEvaluacion.setVisibility(View.GONE);
                    return;
                }

                boolean plazoVencido = System.currentTimeMillis() > actividad.getFechaLimite();
                boolean sinIntentos = intentosUsados >= actividad.getIntentosPermitidos();

                construirPreguntas(preguntas, respuestasPrevias);

                if (resultado != null) {
                    StringBuilder estado = new StringBuilder(" Última entrega: intento " + intentosUsados
                            + " de " + actividad.getIntentosPermitidos()
                            + " (" + formatoFecha.format(resultado.getFechaEnvio()) + ")");
                    if (actividad.isMostrarCalificacion() && resultado.estaCalificada()) {
                        estado.append("\n Calificación: ").append(resultado.getCalificacion());
                        if (resultado.getRetroalimentacion() != null && !resultado.getRetroalimentacion().isEmpty()) {
                            estado.append("\n ").append(resultado.getRetroalimentacion());
                        }
                    } else if (!resultado.estaCalificada()) {
                        estado.append("\nPendiente de calificación.");
                    }
                    binding.tvEstado.setText(estado.toString());
                    binding.tvEstado.setVisibility(View.VISIBLE);
                }

                if (plazoVencido) {
                    binding.tvPlazoVencido.setVisibility(View.VISIBLE);
                    binding.btnEnviarEvaluacion.setEnabled(false);
                } else if (sinIntentos) {
                    binding.tvIntentosAgotados.setVisibility(View.VISIBLE);
                    binding.contenedorPreguntas.setVisibility(View.GONE);
                    binding.btnEnviarEvaluacion.setVisibility(View.GONE);
                }
            });
        });
    }

    private void construirPreguntas(List<Question> preguntas, Map<Integer, String> respuestasPrevias) {
        binding.contenedorPreguntas.removeAllViews();
        gruposOpciones.clear();
        camposTexto.clear();
        int numero = 1;

        for (Question pregunta : preguntas) {
            TextView tvEnunciado = new TextView(this);
            tvEnunciado.setText(numero + ". " + pregunta.getEnunciado());
            tvEnunciado.setTextSize(16);
            tvEnunciado.setPadding(0, 32, 0, 8);
            binding.contenedorPreguntas.addView(tvEnunciado);

            String previa = respuestasPrevias.get(pregunta.getId());

            if (pregunta.tieneOpciones()) {
                RadioGroup grupo = new RadioGroup(this);
                grupo.setOrientation(RadioGroup.VERTICAL);
                grupo.setTag(pregunta.getId());

                for (String opcion : pregunta.getListaOpciones()) {
                    RadioButton rb = new RadioButton(this);
                    rb.setText(opcion);
                    grupo.addView(rb);
                    if (opcion.equals(previa)) rb.setChecked(true);
                }

                binding.contenedorPreguntas.addView(grupo);
                gruposOpciones.put(pregunta.getId(), grupo);
            } else {
                EditText campo = new EditText(this);
                campo.setHint("Escribe tu respuesta");
                campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                if (previa != null) campo.setText(previa);
                binding.contenedorPreguntas.addView(campo);
                camposTexto.put(pregunta.getId(), campo);
            }

            numero++;
        }
    }

    private void enviarEvaluacion() {
        if (actividadActual == null) return;

        if (System.currentTimeMillis() > actividadActual.getFechaLimite()) {
            new AlertDialog.Builder(this)
                    .setTitle("Plazo vencido")
                    .setMessage("Ya no puedes enviar esta evaluación, el plazo venció.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        StringBuilder codificado = new StringBuilder();
        boolean algunaRespuesta = false;

        for (Map.Entry<Integer, RadioGroup> entry : gruposOpciones.entrySet()) {
            RadioGroup grupo = entry.getValue();
            int idSeleccionado = grupo.getCheckedRadioButtonId();
            if (idSeleccionado != -1) {
                RadioButton seleccionado = grupo.findViewById(idSeleccionado);
                if (codificado.length() > 0) codificado.append(SEP_RESPUESTA);
                codificado.append(entry.getKey()).append(SEP_PREGUNTA).append(seleccionado.getText());
                algunaRespuesta = true;
            }
        }

        for (Map.Entry<Integer, EditText> entry : camposTexto.entrySet()) {
            String texto = entry.getValue().getText().toString().trim();
            if (!texto.isEmpty()) {
                if (codificado.length() > 0) codificado.append(SEP_RESPUESTA);
                codificado.append(entry.getKey()).append(SEP_PREGUNTA).append(texto);
                algunaRespuesta = true;
            }
        }

        if (!algunaRespuesta) {
            Toast.makeText(this, "Responde al menos una pregunta", Toast.LENGTH_SHORT).show();
            return;
        }

        String respuestasFinal = codificado.toString();
        binding.btnEnviarEvaluacion.setEnabled(false);

        executor.execute(() -> {
            int intentosUsados = db.quizResultDao().contarIntentos(actividadId, estudianteId);
            if (intentosUsados >= actividadActual.getIntentosPermitidos()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ya usaste todos tus intentos permitidos", Toast.LENGTH_LONG).show();
                    cargarEvaluacion();
                });
                return;
            }

            db.quizResultDao().insertar(new QuizResult(actividadId, estudianteId, respuestasFinal, System.currentTimeMillis()));

            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("¡Evaluación enviada!")
                    .setMessage("Tus respuestas se registraron correctamente.")
                    .setCancelable(false)
                    .setPositiveButton("Aceptar", (d, w) -> {
                        binding.btnEnviarEvaluacion.setEnabled(true);
                        cargarEvaluacion();
                    })
                    .show());
        });
    }

    static Map<Integer, String> parsearRespuestas(String codificado) {
        Map<Integer, String> mapa = new HashMap<>();
        if (codificado == null || codificado.isEmpty()) return mapa;
        String[] partes = codificado.split(String.valueOf(SEP_RESPUESTA));
        for (String parte : partes) {
            int idx = parte.indexOf(SEP_PREGUNTA);
            if (idx > 0) {
                try {
                    int preguntaId = Integer.parseInt(parte.substring(0, idx));
                    String respuesta = parte.substring(idx + 1);
                    mapa.put(preguntaId, respuesta);
                } catch (NumberFormatException ignored) { }
            }
        }
        return mapa;
    }
}