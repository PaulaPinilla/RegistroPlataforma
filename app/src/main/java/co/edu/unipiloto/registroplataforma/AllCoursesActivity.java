package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAllCoursesBinding;

public class AllCoursesActivity extends AppCompatActivity {

    private ActivityAllCoursesBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> { /* solo lectura */ });
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
            List<Course> cursos = db.courseDao().listarTodos();
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Course c : cursos) {
                String subtitulo = "Profesor: " + c.getProfesorNombre() + " — " + estadoLegible(c.getEstado());
                items.add(new SimpleListAdapter.Item(c.getId(), c.getTitulo(), subtitulo, false));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private String estadoLegible(String estado) {
        if (Course.ESTADO_APROBADO.equals(estado)) return "Aprobado";
        if (Course.ESTADO_RECHAZADO.equals(estado)) return "Rechazado";
        return "Pendiente";
    }
}