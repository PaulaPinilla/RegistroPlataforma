package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    /** Máximo de avisos que se muestran en el inicio */
    private static final int MAX_AVISOS = 10;

    private ActivityHomeBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SimpleListAdapter adapterAvisos;
    private Map<Integer, AvisosHelper.Aviso> avisosPorActividad = new HashMap<>();

    private int usuarioId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        usuarioId = getIntent().getIntExtra("usuarioId", -1);
        String nombre = getIntent().getStringExtra("nombre");
        String rol = getIntent().getStringExtra("rol");
        if (nombre == null) nombre = "";
        if (rol == null) rol = User.ROL_ESTUDIANTE;

        binding.tvBienvenida.setText("¡Hola, " + nombre + "!");

        esProfesor = User.ROL_PROFESOR.equals(rol);

        binding.tvRol.setText(esProfesor ? "Rol: Profesor" : "Rol: Estudiante");

        binding.btnMisCursosProfesor.setVisibility(esProfesor ? View.VISIBLE : View.GONE);
        binding.btnCursosDisponibles.setVisibility(esProfesor ? View.GONE : View.VISIBLE);
        binding.btnMisCursosEstudiante.setVisibility(esProfesor ? View.GONE : View.VISIBLE);
        binding.btnMisCalificaciones.setVisibility(esProfesor ? View.GONE : View.VISIBLE);

        // El panel de avisos es solo para el estudiante
        binding.panelAvisos.setVisibility(esProfesor ? View.GONE : View.VISIBLE);
        if (!esProfesor) {
            adapterAvisos = new SimpleListAdapter(item -> abrirCursoDelAviso(item.id));
            binding.rvAvisos.setLayoutManager(new LinearLayoutManager(this));
            binding.rvAvisos.setAdapter(adapterAvisos);
            binding.rvAvisos.setNestedScrollingEnabled(false);
        }

        final int idFinal = usuarioId;
        final String nombreFinal = nombre;

        binding.btnMisCursosProfesor.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseListActivity.class);
            intent.putExtra("profesorId", idFinal);
            intent.putExtra("profesorNombre", nombreFinal);
            startActivity(intent);
        });

        binding.btnCursosDisponibles.setOnClickListener(v -> {
            Intent intent = new Intent(this, AvailableCoursesActivity.class);
            intent.putExtra("estudianteId", idFinal);
            startActivity(intent);
        });

        binding.btnMisCursosEstudiante.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyCoursesActivity.class);
            intent.putExtra("estudianteId", idFinal);
            startActivity(intent);
        });

        binding.btnMisCalificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(this, GradesCourseListActivity.class);
            intent.putExtra("estudianteId", idFinal);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cada vez que el estudiante vuelve al inicio se actualizan los avisos
        if (!esProfesor) {
            cargarAvisos();
        }
    }

    private void cargarAvisos() {
        executor.execute(() -> {
            List<AvisosHelper.Aviso> todos = AvisosHelper.generar(db, usuarioId);

            List<AvisosHelper.Aviso> pronto = new ArrayList<>();
            List<AvisosHelper.Aviso> nuevas = new ArrayList<>();
            List<AvisosHelper.Aviso> vencidas = new ArrayList<>();
            for (AvisosHelper.Aviso a : todos) {
                if (a.tipo == AvisosHelper.TIPO_PRONTO) pronto.add(a);
                else if (a.tipo == AvisosHelper.TIPO_NUEVA) nuevas.add(a);
                else vencidas.add(a);
            }

            Comparator<AvisosHelper.Aviso> porOrden = (x, y) -> Long.compare(x.orden, y.orden);
            Collections.sort(pronto, porOrden);
            Collections.sort(nuevas, porOrden);
            Collections.sort(vencidas, porOrden);

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            Map<Integer, AvisosHelper.Aviso> mapa = new HashMap<>();
            int[] contador = {0};

            agregarGrupo(items, mapa, contador, "Vencen pronto", pronto);
            agregarGrupo(items, mapa, contador, "Actividades nuevas", nuevas);
            agregarGrupo(items, mapa, contador, "Vencidas sin entregar", vencidas);

            runOnUiThread(() -> {
                avisosPorActividad = mapa;
                adapterAvisos.actualizarDatos(items);
                binding.tvSinAvisos.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                binding.rvAvisos.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void agregarGrupo(List<SimpleListAdapter.Item> items, Map<Integer, AvisosHelper.Aviso> mapa,
                              int[] contador, String encabezado, List<AvisosHelper.Aviso> grupo) {
        boolean encabezadoAgregado = false;
        for (AvisosHelper.Aviso a : grupo) {
            if (contador[0] >= MAX_AVISOS) return;
            if (!encabezadoAgregado) {
                items.add(new SimpleListAdapter.Item(encabezado));
                encabezadoAgregado = true;
            }
            items.add(new SimpleListAdapter.Item(a.actividadId, a.titulo,
                    a.cursoTitulo + " — " + a.detalle, false));
            mapa.put(a.actividadId, a);
            contador[0]++;
        }
    }

    /** Al tocar un aviso se abre el curso al que pertenece la actividad */
    private void abrirCursoDelAviso(int actividadId) {
        AvisosHelper.Aviso aviso = avisosPorActividad.get(actividadId);
        if (aviso == null) return;

        Intent intent = new Intent(this, UnitListActivity.class);
        intent.putExtra("cursoId", aviso.cursoId);
        intent.putExtra("cursoTitulo", aviso.cursoTitulo);
        intent.putExtra("estudianteId", usuarioId);
        startActivity(intent);
    }
}