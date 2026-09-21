package co.edu.unipiloto.registroplataforma;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.data.ForumPost;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumDetailBinding;

public class ForumDetailActivity extends AppCompatActivity {

    private ActivityForumDetailBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private int foroId;
    private int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        foroId = getIntent().getIntExtra("foroId", -1);
        usuarioId = getIntent().getIntExtra("usuarioId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnPublicar.setOnClickListener(v -> publicarComentario());

        cargarForo();
    }

    private void cargarForo() {
        executor.execute(() -> {
            Forum foro = db.forumDao().obtenerPorId(foroId);
            List<ForumPost> comentarios = db.forumPostDao().listarPorForo(foroId);

            runOnUiThread(() -> {
                if (foro == null) {
                    finish();
                    return;
                }
                binding.tvTema.setText(foro.getTema());
                pintarComentarios(comentarios);
            });
        });
    }

    private void pintarComentarios(List<ForumPost> comentarios) {
        binding.contenedorComentarios.removeAllViews();

        if (comentarios.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText("Aún no hay participaciones. ¡Sé el primero en comentar!");
            vacio.setPadding(0, 24, 0, 24);
            binding.contenedorComentarios.addView(vacio);
            return;
        }

        for (ForumPost c : comentarios) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, 24, 0, 0);

            TextView autorFecha = new TextView(this);
            autorFecha.setText(c.getAutorNombre() + " — " + formato.format(c.getFecha()));
            autorFecha.setTextSize(13);
            autorFecha.setTypeface(null, Typeface.BOLD_ITALIC);
            item.addView(autorFecha);

            TextView texto = new TextView(this);
            texto.setText(c.getTexto());
            texto.setPadding(0, 4, 0, 0);
            item.addView(texto);

            binding.contenedorComentarios.addView(item);
        }
    }

    private void publicarComentario() {
        String texto = binding.etNuevoComentario.getText().toString().trim();
        if (texto.isEmpty()) return;

        binding.btnPublicar.setEnabled(false);

        executor.execute(() -> {
            User usuario = db.userDao().obtenerPorId(usuarioId);
            String nombre = usuario != null ? usuario.getNombre() : "Usuario";
            db.forumPostDao().insertar(new ForumPost(foroId, usuarioId, nombre, texto, System.currentTimeMillis()));
            runOnUiThread(() -> {
                binding.etNuevoComentario.setText("");
                binding.btnPublicar.setEnabled(true);
                cargarForo();
            });
        });
    }
}