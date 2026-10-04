package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityGradesCourseListBinding;

public class GradesCourseListActivity extends AppCompatActivity {

    private ActivityGradesCourseListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGradesCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> {
            Intent intent = new Intent(this, GradesActivity.class);
            intent.putExtra("cursoId", item.id);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });
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
            List<Course> cursos = db.enrollmentDao().listarCursosInscritos(estudianteId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Course c : cursos) {
                items.add(new SimpleListAdapter.Item(c.getId(), c.getTitulo(), "Profesor: " + c.getProfesorNombre(), false));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}