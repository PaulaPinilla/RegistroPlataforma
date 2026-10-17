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
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumListBinding;

public class ForumListActivity extends AppCompatActivity {

    private ActivityForumListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int cursoId;
    private int usuarioId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        int profesorId = getIntent().getIntExtra("profesorId", -1);
        int estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;
        usuarioId = esProfesor ? profesorId : estudianteId;

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnCrearForo.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnCrearForo.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForumFormActivity.class);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        binding.btnReporte.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnReporte.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForumReportActivity.class);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(item -> {
            Intent intent = new Intent(this, ForumDetailActivity.class);
            intent.putExtra("foroId", item.id);
            intent.putExtra("usuarioId", usuarioId);
            startActivity(intent);
        });
        binding.rvForos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvForos.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarForos();
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    private void cargarForos() {
        executor.execute(() -> {
            List<Forum> foros = db.forumDao().listarPorCurso(cursoId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Forum f : foros) {
                int cantidad = db.forumPostDao().contarPorForo(f.getId());
                String estado;
                if (!f.yaAbrio()) {
                    estado = "Aún no abre";
                } else if (f.yaCerro()) {
                    estado = "Cerrado";
                } else {
                    estado = cantidad + (cantidad == 1 ? " participación" : " participaciones");
                }
                String valor = f.getPorcentaje() > 0
                        ? "Corte " + f.getCorte() + " - vale " + formatearPorcentaje(f.getPorcentaje()) + "%"
                        : "Corte " + f.getCorte() + " - sin porcentaje definido";
                items.add(new SimpleListAdapter.Item(f.getId(), f.getTema(), valor + " - " + estado, false));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}