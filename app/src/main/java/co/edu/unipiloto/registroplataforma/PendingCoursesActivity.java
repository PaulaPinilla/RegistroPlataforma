package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityPendingCoursesBinding;

public class PendingCoursesActivity extends AppCompatActivity {

    private ActivityPendingCoursesBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Course> cursosActuales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> mostrarOpciones(item.id));
        binding.rvCursos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCursos.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void cargarCursos() {
        executor.execute(() -> {
            List<Course> cursos = db.courseDao().listarPendientes();
            cursosActuales.clear();
            cursosActuales.addAll(cursos);

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Course c : cursos) {
                items.add(new SimpleListAdapter.Item(c.getId(), c.getTitulo(),
                        "Profesor: " + c.getProfesorNombre(), false));
            }

            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private Course buscarCurso(int cursoId) {
        for (Course c : cursosActuales) {
            if (c.getId() == cursoId) return c;
        }
        return null;
    }

    private void mostrarOpciones(int cursoId) {
        Course curso = buscarCurso(cursoId);
        if (curso == null) return;

        new AlertDialog.Builder(this)
                .setTitle(curso.getTitulo())
                .setMessage(curso.getDescripcion() + "\n\nProfesor: " + curso.getProfesorNombre())
                .setNegativeButton("Rechazar", (dialog, which) -> cambiarEstado(curso, Course.ESTADO_RECHAZADO))
                .setPositiveButton("Aprobar", (dialog, which) -> cambiarEstado(curso, Course.ESTADO_APROBADO))
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void cambiarEstado(Course curso, String nuevoEstado) {
        executor.execute(() -> {
            db.courseDao().actualizarEstado(curso.getId(), nuevoEstado);
            runOnUiThread(this::cargarCursos);
        });
    }
}