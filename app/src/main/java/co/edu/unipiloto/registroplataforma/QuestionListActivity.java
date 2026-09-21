package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Question;
import co.edu.unipiloto.registroplataforma.databinding.ActivityQuestionListBinding;

public class QuestionListActivity extends AppCompatActivity {

    private ActivityQuestionListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int actividadId;
    private String actividadTitulo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        actividadId = getIntent().getIntExtra("actividadId", -1);
        actividadTitulo = getIntent().getStringExtra("actividadTitulo");

        binding.tvTitulo.setText(actividadTitulo != null ? actividadTitulo : "Preguntas");
        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAgregarPregunta.setOnClickListener(v -> mostrarDialogoNuevaPregunta());
        binding.btnVerResultados.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizResultListActivity.class);
            intent.putExtra("actividadId", actividadId);
            intent.putExtra("actividadTitulo", actividadTitulo);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(item -> { /* solo lectura por ahora */ });
        binding.rvPreguntas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPreguntas.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPreguntas();
    }

    private void cargarPreguntas() {
        executor.execute(() -> {
            List<Question> preguntas = db.questionDao().listarPorActividad(actividadId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Question p : preguntas) {
                items.add(new SimpleListAdapter.Item(p.getId(), p.getEnunciado(),
                        p.tieneOpciones() ? "Opción múltiple" : "Respuesta libre", false));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private void mostrarDialogoNuevaPregunta() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        final EditText inputEnunciado = new EditText(this);
        inputEnunciado.setHint("Enunciado de la pregunta");
        layout.addView(inputEnunciado);

        RadioGroup grupoTipo = new RadioGroup(this);
        grupoTipo.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbConOpciones = new RadioButton(this);
        rbConOpciones.setText("Con opciones");
        rbConOpciones.setChecked(true);
        grupoTipo.addView(rbConOpciones);

        RadioButton rbSinOpciones = new RadioButton(this);
        rbSinOpciones.setText("Respuesta libre");
        grupoTipo.addView(rbSinOpciones);

        layout.addView(grupoTipo);

        final EditText inputOpciones = new EditText(this);
        inputOpciones.setHint("Opciones (una por línea)");
        inputOpciones.setMinLines(3);
        layout.addView(inputOpciones);

        grupoTipo.setOnCheckedChangeListener((group, checkedId) ->
                inputOpciones.setVisibility(rbConOpciones.isChecked() ? android.view.View.VISIBLE : android.view.View.GONE));

        new AlertDialog.Builder(this)
                .setTitle("Nueva pregunta")
                .setView(layout)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String enunciado = inputEnunciado.getText().toString().trim();
                    if (enunciado.isEmpty()) {
                        Toast.makeText(this, "Escribe el enunciado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String opcionesFinal = null;
                    if (rbConOpciones.isChecked()) {
                        String[] lineas = inputOpciones.getText().toString().trim().split("\n");
                        StringBuilder unidas = new StringBuilder();
                        int validas = 0;
                        for (String l : lineas) {
                            String limpio = l.trim();
                            if (!limpio.isEmpty()) {
                                if (unidas.length() > 0) unidas.append("|");
                                unidas.append(limpio);
                                validas++;
                            }
                        }
                        if (validas < 2) {
                            Toast.makeText(this, "Escribe al menos 2 opciones, una por línea", Toast.LENGTH_LONG).show();
                            return;
                        }
                        opcionesFinal = unidas.toString();
                    }

                    String opcionesParaGuardar = opcionesFinal;
                    executor.execute(() -> {
                        db.questionDao().insertar(new Question(actividadId, enunciado, opcionesParaGuardar));
                        runOnUiThread(this::cargarPreguntas);
                    });
                })
                .show();
    }
}