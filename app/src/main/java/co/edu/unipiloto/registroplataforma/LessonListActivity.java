package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.Lesson;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLessonListBinding;

public class LessonListActivity extends AppCompatActivity {

    private ActivityLessonListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapterLecciones;
    private SimpleListAdapter adapterActividades;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private int unidadId;
    private int cursoId;
    private String unidadTitulo;
    private int profesorId;
    private int estudianteId;
    private boolean esProfesor;

    // Estado del curso (se consulta cada vez que la pantalla vuelve a mostrarse)
    private volatile boolean cursoTerminado = false;
    private volatile long fechaFinCurso = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        unidadTitulo = getIntent().getStringExtra("unidadTitulo");
        profesorId = getIntent().getIntExtra("profesorId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;

        binding.tvTitulo.setText(unidadTitulo != null ? unidadTitulo : "Lecciones");
        binding.btnVolver.setOnClickListener(v -> finish());

        binding.btnAgregarLeccion.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAgregarLeccion.setOnClickListener(v -> {
            Intent intent = new Intent(this, LessonFormActivity.class);
            intent.putExtra("unidadId", unidadId);
            startActivity(intent);
        });

        binding.btnAgregarActividad.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAgregarActividad.setOnClickListener(v -> {
            if (cursoTerminado) {
                mostrarAvisoCursoTerminado();
                return;
            }
            Intent intent = new Intent(this, AssignmentFormActivity.class);
            intent.putExtra("unidadId", unidadId);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        adapterLecciones = new SimpleListAdapter(item -> {
            if (esProfesor) return;
            Intent intent = new Intent(this, LessonDetailActivity.class);
            intent.putExtra("leccionId", item.id);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });
        binding.rvLecciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLecciones.setAdapter(adapterLecciones);
        binding.rvLecciones.setNestedScrollingEnabled(false);

        adapterActividades = new SimpleListAdapter(new SimpleListAdapter.OnItemClickListener() {
            @Override
            public void onClick(SimpleListAdapter.Item item) {
                if (esProfesor) {
                    Intent intent = new Intent(LessonListActivity.this, SubmissionListActivity.class);
                    intent.putExtra("actividadId", item.id);
                    intent.putExtra("actividadTitulo", item.titulo);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(LessonListActivity.this, AssignmentDetailActivity.class);
                    intent.putExtra("actividadId", item.id);
                    intent.putExtra("estudianteId", estudianteId);
                    startActivity(intent);
                }
            }

            @Override
            public void onEditar(SimpleListAdapter.Item item) {
                if (cursoTerminado) {
                    mostrarAvisoCursoTerminado();
                    return;
                }
                Intent intent = new Intent(LessonListActivity.this, AssignmentFormActivity.class);
                intent.putExtra("unidadId", unidadId);
                intent.putExtra("cursoId", cursoId);
                intent.putExtra("actividadId", item.id);
                startActivity(intent);
            }

            @Override
            public void onEliminar(SimpleListAdapter.Item item) {
                if (cursoTerminado) {
                    mostrarAvisoCursoTerminado();
                    return;
                }
                confirmarEliminarActividad(item);
            }
        });
        binding.rvActividades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActividades.setAdapter(adapterActividades);
        binding.rvActividades.setNestedScrollingEnabled(false);
    }

    private void mostrarAvisoCursoTerminado() {
        String fin = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaFinCurso);
        new AlertDialog.Builder(this)
                .setTitle("Curso finalizado")
                .setMessage("Este curso finalizó el " + fin
                        + ". Ya no se pueden crear, editar ni eliminar actividades.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void confirmarEliminarActividad(SimpleListAdapter.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Seguro que quieres eliminar \"" + item.titulo + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarActividad(item.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarActividad(int idActividad) {
        executor.execute(() -> {
            Assignment a = db.assignmentDao().obtenerPorId(idActividad);
            if (a != null) {
                db.assignmentDao().eliminar(a);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Actividad eliminada", Toast.LENGTH_SHORT).show();
                cargarActividades();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLecciones();
        cargarCursoYActividades();
    }

    /** Primero consulta si el curso ya terminó y luego carga las actividades (mismo hilo, en orden). */
    private void cargarCursoYActividades() {
        executor.execute(() -> {
            if (cursoId != -1) {
                Course curso = db.courseDao().obtenerPorId(cursoId);
                cursoTerminado = esProfesor && curso != null && curso.yaTermino();
                fechaFinCurso = curso != null ? curso.getFechaFinalizacion() : 0;
            }
            runOnUiThread(() -> binding.btnAgregarActividad.setAlpha(cursoTerminado ? 0.5f : 1f));
        });
        cargarActividades();
    }

    private void cargarLecciones() {
        executor.execute(() -> {
            List<Lesson> lecciones = db.lessonDao().listarPorUnidad(unidadId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Lesson l : lecciones) {
                boolean completada = !esProfesor && db.lessonProgressDao().existeProgreso(l.getId(), estudianteId) > 0;
                items.add(new SimpleListAdapter.Item(l.getId(), l.getTitulo(), tipoLegibleLeccion(l), completada));
            }
            runOnUiThread(() -> {
                adapterLecciones.actualizarDatos(items);
                binding.tvVacioLecciones.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private void cargarActividades() {
        executor.execute(() -> {
            List<Assignment> actividades = db.assignmentDao().listarPorUnidad(unidadId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            boolean mostrarAcciones = esProfesor && !cursoTerminado;
            for (Assignment a : actividades) {
                String subtitulo = AssignmentListActivity.categoriaLegible(a.getCategoria())
                        + " — Entrega antes del " + formatoFecha.format(a.getFechaLimite());
                boolean entregada;
                if (Assignment.TIPO_ENTREGA_PREGUNTAS.equals(a.getTipoEntrega())) {
                    entregada = !esProfesor && db.quizResultDao().obtenerUltimoResultado(a.getId(), estudianteId) != null;
                } else {
                    entregada = !esProfesor && db.submissionDao().obtenerEntrega(a.getId(), estudianteId) != null;
                }
                items.add(new SimpleListAdapter.Item(a.getId(), a.getTitulo(), subtitulo, entregada, mostrarAcciones));
            }
            runOnUiThread(() -> {
                adapterActividades.actualizarDatos(items);
                binding.tvVacioActividades.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private String tipoLegibleLeccion(Lesson leccion) {
        return Lesson.TIPO_VIDEO.equals(leccion.getTipo()) ? "Video" : "Documento";
    }
}