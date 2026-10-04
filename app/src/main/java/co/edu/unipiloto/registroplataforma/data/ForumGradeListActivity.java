package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.data.ForumGrade;
import co.edu.unipiloto.registroplataforma.data.ForumPost;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumGradeListBinding;

public class ForumGradeListActivity extends AppCompatActivity {

    private ActivityForumGradeListBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int foroId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumGradeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        foroId = getIntent().getIntExtra("foroId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());

        cargarParticipantes();
    }

    private void cargarParticipantes() {
        executor.execute(() -> {
            Forum foro = db.forumDao().obtenerPorId(foroId);
            List<ForumPost> comentarios = db.forumPostDao().listarPorForo(foroId);
            List<ForumGrade> calificacionesExistentes = db.forumGradeDao().listarPorForo(foroId);

            Map<Integer, String> participantes = new LinkedHashMap<>();
            for (ForumPost c : comentarios) {
                participantes.put(c.getAutorId(), c.getAutorNombre());
            }

            Map<Integer, ForumGrade> calificacionesPorEstudiante = new LinkedHashMap<>();
            for (ForumGrade cal : calificacionesExistentes) {
                calificacionesPorEstudiante.put(cal.getEstudianteId(), cal);
            }

            runOnUiThread(() -> {
                if (foro != null) {
                    binding.tvTema.setText("Calificar: " + foro.getTema());
                }
                pintarParticipantes(participantes, calificacionesPorEstudiante);
            });
        });
    }

    private void pintarParticipantes(Map<Integer, String> participantes, Map<Integer, ForumGrade> calificaciones) {
        binding.contenedorEstudiantes.removeAllViews();

        if (participantes.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Aún ningún estudiante ha participado en este foro.");
            vacio.setPadding(0, 16, 0, 16);
            binding.contenedorEstudiantes.addView(vacio);
            return;
        }

        for (Map.Entry<Integer, String> entry : participantes.entrySet()) {
            int estudianteId = entry.getKey();
            String nombre = entry.getValue();
            ForumGrade existente = calificaciones.get(estudianteId);

            LinearLayout tarjeta = new LinearLayout(this);
            tarjeta.setOrientation(LinearLayout.VERTICAL);
            tarjeta.setPadding(24, 24, 24, 24);
            tarjeta.setBackgroundColor(0x11000000);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            tarjeta.setLayoutParams(params);

            TextView tvNombre = new TextView(this);
            tvNombre.setText(nombre);
            tvNombre.setTextSize(16);
            tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
            tarjeta.addView(tvNombre);

            EditText etNota = new EditText(this);
            etNota.setHint("Nota (0.0 - 5.0)");
            etNota.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            if (existente != null) etNota.setText(String.valueOf(existente.getCalificacion()));
            tarjeta.addView(etNota);

            EditText etComentario = new EditText(this);
            etComentario.setHint("Comentario (opcional)");
            if (existente != null && existente.getComentario() != null) etComentario.setText(existente.getComentario());
            tarjeta.addView(etComentario);

            TextView btnGuardar = new TextView(this);
            btnGuardar.setText("Guardar");
            btnGuardar.setTextColor(0xFF9B7BD6);
            btnGuardar.setPadding(0, 12, 0, 0);
            btnGuardar.setOnClickListener(v -> {
                String notaTexto = etNota.getText().toString().trim();
                if (notaTexto.isEmpty()) {
                    Toast.makeText(this, "Escribe una nota", Toast.LENGTH_SHORT).show();
                    return;
                }
                double nota;
                try {
                    nota = Double.parseDouble(notaTexto);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Nota inválida", Toast.LENGTH_SHORT).show();
                    return;
                }
                String comentario = etComentario.getText().toString().trim();

                executor.execute(() -> {
                    db.forumGradeDao().guardar(new ForumGrade(foroId, estudianteId, nombre, nota, comentario));
                    runOnUiThread(() -> Toast.makeText(this, "Calificación guardada", Toast.LENGTH_SHORT).show());
                });
            });
            tarjeta.addView(btnGuardar);

            binding.contenedorEstudiantes.addView(tarjeta);
        }
    }
}