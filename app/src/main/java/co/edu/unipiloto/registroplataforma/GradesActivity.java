package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.data.ForumGrade;
import co.edu.unipiloto.registroplataforma.data.QuizResult;
import co.edu.unipiloto.registroplataforma.data.Submission;
import co.edu.unipiloto.registroplataforma.databinding.ActivityGradesBinding;

public class GradesActivity extends AppCompatActivity {

    private ActivityGradesBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int cursoId;
    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGradesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnObtenerCertificado.setOnClickListener(v -> {
            Intent intent = new Intent(this, CertificateActivity.class);
            intent.putExtra("cursoId", cursoId);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(item -> { /* solo lectura */ });
        binding.rvCalificaciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCalificaciones.setAdapter(adapter);

        cargarCalificaciones();
    }

    private void cargarCalificaciones() {
        executor.execute(() -> {
            Course curso = db.courseDao().obtenerPorId(cursoId);
            List<Assignment> actividades = db.assignmentDao().listarPorCurso(cursoId);
            List<Forum> foros = db.forumDao().listarPorCurso(cursoId);

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            double sumaNotas = 0;
            int cantidadCalificadas = 0;
            int totalEvaluable = actividades.size() + foros.size();

            for (Assignment a : actividades) {
                Double nota = null;

                if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(a.getTipoEntrega())) {
                    QuizResult resultado = db.quizResultDao().obtenerUltimoResultado(a.getId(), estudianteId);
                    if (resultado != null) nota = resultado.getCalificacion();
                } else {
                    Submission entrega = db.submissionDao().obtenerUltimaEntrega(a.getId(), estudianteId);
                    if (entrega != null) nota = entrega.getCalificacion();
                }

                String subtitulo;
                if (nota != null) {
                    subtitulo = "Calificación: " + nota;
                    sumaNotas += nota;
                    cantidadCalificadas++;
                } else {
                    subtitulo = "Sin calificar";
                }

                items.add(new SimpleListAdapter.Item(a.getId(),
                        AssignmentListActivity.categoriaLegible(a.getCategoria()) + " — " + a.getTitulo(),
                        subtitulo, nota != null));
            }

            for (Forum f : foros) {
                ForumGrade calificacion = db.forumGradeDao().obtenerPorForoYEstudiante(f.getId(), estudianteId);
                Double nota = calificacion != null ? calificacion.getCalificacion() : null;

                String subtitulo;
                if (nota != null) {
                    subtitulo = "Calificación: " + nota;
                    sumaNotas += nota;
                    cantidadCalificadas++;
                } else {
                    subtitulo = "Sin calificar";
                }

                items.add(new SimpleListAdapter.Item(f.getId(),
                        "Foro — " + f.getTema(), subtitulo, nota != null));
            }

            double promedio = cantidadCalificadas > 0 ? sumaNotas / cantidadCalificadas : 0;
            boolean todosCalificados = totalEvaluable > 0 && cantidadCalificadas == totalEvaluable;
            boolean aprobado = todosCalificados && promedio >= 3.0;

            String tituloCurso = curso != null ? curso.getTitulo() : "Calificaciones";
            String promedioTexto = cantidadCalificadas > 0
                    ? String.format(Locale.getDefault(), "Promedio del curso: %.1f", promedio)
                    : "Todavía no tienes actividades calificadas en este curso.";

            runOnUiThread(() -> {
                binding.tvTituloCurso.setText(tituloCurso);
                binding.tvPromedio.setText(promedioTexto);
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                binding.btnObtenerCertificado.setVisibility(aprobado ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}