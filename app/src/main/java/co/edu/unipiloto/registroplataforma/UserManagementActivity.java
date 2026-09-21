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
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityUserManagementBinding;

public class UserManagementActivity extends AppCompatActivity {

    private ActivityUserManagementBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<User> usuariosActuales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        binding.btnVolver.setOnClickListener(v -> finish());

        adapter = new SimpleListAdapter(item -> confirmarEliminar(item.id));
        binding.rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsuarios.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        executor.execute(() -> {
            List<User> usuarios = db.userDao().listarTodos();
            usuariosActuales.clear();
            usuariosActuales.addAll(usuarios);

            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (User u : usuarios) {
                items.add(new SimpleListAdapter.Item(u.getId(), u.getNombre(),
                        rolLegible(u.getRol()) + " — " + u.getCorreo(), false));
            }

            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private String rolLegible(String rol) {
        if (User.ROL_PROFESOR.equals(rol)) return "Profesor";
        if (User.ROL_COORDINADOR.equals(rol)) return "Coordinador";
        return "Estudiante";
    }

    private User buscarUsuario(int id) {
        for (User u : usuariosActuales) {
            if (u.getId() == id) return u;
        }
        return null;
    }

    private void confirmarEliminar(int usuarioId) {
        User usuario = buscarUsuario(usuarioId);
        if (usuario == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Seguro que quieres eliminar a \"" + usuario.getNombre()
                        + "\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> executor.execute(() -> {
                    db.userDao().eliminar(usuario);
                    runOnUiThread(this::cargarUsuarios);
                }))
                .show();
    }
}