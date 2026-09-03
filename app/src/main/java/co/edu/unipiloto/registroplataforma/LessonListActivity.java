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
import co.edu.unipiloto.registroplataforma.data.Lesson;
import co.edu.unipiloto.registroplataforma.databinding.ActivityLessonListBinding;

public class LessonListActivity extends AppCompatActivity {

    private ActivityLessonListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        adapter = new SimpleListAdapter(item -> {
            if (esProfesor) return;
            Intent intent = new Intent(this, LessonDetailActivity.class);
            intent.putExtra("leccionId", item.id);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });
        binding.rvLecciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLecciones.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLecciones();
    }

    private void cargarLecciones() {
        executor.execute(() -> {
            List<Lesson> lecciones = db.lessonDao().listarPorUnidad(unidadId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Lesson l : lecciones) {
                boolean completada = !esProfesor && db.lessonProgressDao().existeProgreso(l.getId(), estudianteId) > 0;
                items.add(new SimpleListAdapter.Item(l.getId(), l.getTitulo(), tipoLegible(l.getTipo()), completada));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private String tipoLegible(String tipo) {
        if (Lesson.TIPO_VIDEO.equals(tipo)) return "🎥 Video";
        if (Lesson.TIPO_DOCUMENTO.equals(tipo)) return "📄 Documento";
        return "🔗 Material";
    }
}