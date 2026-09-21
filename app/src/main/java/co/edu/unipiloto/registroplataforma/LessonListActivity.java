package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

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
import co.edu.unipiloto.registroplataforma.data.Lesson;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLessonListBinding;

public class LessonListActivity extends AppCompatActivity {

    private ActivityLessonListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapterLecciones;
    private SimpleListAdapter adapterActividades;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int unidadId;
    private String unidadTitulo;
    private int profesorId;
    private int estudianteId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);
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
            Intent intent = new Intent(this, AssignmentFormActivity.class);
            intent.putExtra("unidadId", unidadId);
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

        adapterActividades = new SimpleListAdapter(item -> {
            if (esProfesor) {
                Intent intent = new Intent(this, SubmissionListActivity.class);
                intent.putExtra("actividadId", item.id);
                intent.putExtra("actividadTitulo", item.titulo);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, AssignmentDetailActivity.class);
                intent.putExtra("actividadId", item.id);
                intent.putExtra("estudianteId", estudianteId);
                startActivity(intent);
            }
        });
        binding.rvActividades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActividades.setAdapter(adapterActividades);
        binding.rvActividades.setNestedScrollingEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLecciones();
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
            for (Assignment a : actividades) {
                String subtitulo = AssignmentListActivity.categoriaLegible(a.getCategoria())
                        + " — Entrega antes del " + formatoFecha.format(a.getFechaLimite());
                boolean entregada = !esProfesor && db.submissionDao().obtenerEntrega(a.getId(), estudianteId) != null;
                items.add(new SimpleListAdapter.Item(a.getId(), a.getTitulo(), subtitulo, entregada));
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