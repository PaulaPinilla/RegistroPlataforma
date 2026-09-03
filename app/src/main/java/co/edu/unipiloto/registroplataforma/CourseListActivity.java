package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityCourseListBinding;

public class CourseListActivity extends AppCompatActivity {

    private ActivityCourseListBinding binding;
    private AppDatabase db;
    private CourseAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int profesorId;
    private String profesorNombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        profesorId = getIntent().getIntExtra("profesorId", -1);
        profesorNombre = getIntent().getStringExtra("profesorNombre");

        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new CourseAdapter(CourseAdapter.Modo.PROFESOR, new CourseAdapter.OnCourseActionListener() {
            @Override
            public void onVerDetalle(Course curso) {
                abrirFormulario(curso.getId());
            }

            @Override
            public void onEditar(Course curso) {
                abrirFormulario(curso.getId());
            }

            @Override
            public void onEliminar(Course curso) {
                confirmarEliminar(curso);
            }

            @Override
            public void onVerContenido(Course curso) {
                Intent intent = new Intent(CourseListActivity.this, UnitListActivity.class);
                intent.putExtra("cursoId", curso.getId());
                intent.putExtra("cursoTitulo", curso.getTitulo());
                intent.putExtra("profesorId", profesorId);
                startActivity(intent);
            }
        });

        binding.rvCursos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCursos.setAdapter(adapter);

        binding.btnCrearCurso.setOnClickListener(v -> abrirFormulario(-1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void abrirFormulario(int cursoId) {
        Intent intent = new Intent(this, CourseFormActivity.class);
        intent.putExtra("profesorId", profesorId);
        intent.putExtra("profesorNombre", profesorNombre);
        if (cursoId != -1) intent.putExtra("cursoId", cursoId);
        startActivity(intent);
    }

    private void confirmarEliminar(Course curso) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar curso")
                .setMessage("¿Seguro que quieres eliminar \"" + curso.getTitulo() + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> executor.execute(() -> {
                    db.courseDao().eliminar(curso);
                    runOnUiThread(this::cargarCursos);
                }))
                .show();
    }

    private void cargarCursos() {
        executor.execute(() -> {
            List<Course> cursos = db.courseDao().listarPorProfesor(profesorId);
            runOnUiThread(() -> {
                adapter.actualizarDatos(cursos);
                binding.tvVacio.setVisibility(adapter.estaVacio() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}