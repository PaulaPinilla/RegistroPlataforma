package co.edu.unipiloto.registroplataforma;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ItemCourseBinding;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    public enum Modo { PROFESOR, DISPONIBLE, INSCRITO }

    public interface OnCourseActionListener {
        void onVerDetalle(Course curso);
        default void onEditar(Course curso) {}
        default void onEliminar(Course curso) {}
        default void onVerContenido(Course curso) {}
    }

    private final List<Course> cursos = new ArrayList<>();
    private final Modo modo;
    private final OnCourseActionListener listener;

    public CourseAdapter(Modo modo, OnCourseActionListener listener) {
        this.modo = modo;
        this.listener = listener;
    }

    public void actualizarDatos(List<Course> nuevosCursos) {
        cursos.clear();
        cursos.addAll(nuevosCursos);
        notifyDataSetChanged();
    }

    public boolean estaVacio() {
        return cursos.isEmpty();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseBinding binding = ItemCourseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        holder.bind(cursos.get(position));
    }

    @Override
    public int getItemCount() {
        return cursos.size();
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {

        private final ItemCourseBinding binding;

        CourseViewHolder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course curso) {
            binding.tvTitulo.setText(curso.getTitulo());
            binding.tvDescripcion.setText(curso.getDescripcion());
            binding.tvProfesor.setText("Profesor: " + curso.getProfesorNombre());

            binding.getRoot().setOnClickListener(v -> listener.onVerDetalle(curso));

            switch (modo) {
                case PROFESOR:
                    binding.btnAccionPrincipal.setVisibility(View.GONE);
                    binding.rowAccionesProfesor.setVisibility(View.VISIBLE);
                    binding.btnContenido.setOnClickListener(v -> listener.onVerContenido(curso));
                    binding.btnEditar.setOnClickListener(v -> listener.onEditar(curso));
                    binding.btnEliminar.setOnClickListener(v -> listener.onEliminar(curso));
                    break;
                case DISPONIBLE:
                    binding.rowAccionesProfesor.setVisibility(View.GONE);
                    binding.btnAccionPrincipal.setVisibility(View.VISIBLE);
                    binding.btnAccionPrincipal.setText("Ver curso");
                    binding.btnAccionPrincipal.setOnClickListener(v -> listener.onVerDetalle(curso));
                    break;
                case INSCRITO:
                    binding.rowAccionesProfesor.setVisibility(View.GONE);
                    binding.btnAccionPrincipal.setVisibility(View.GONE);
                    break;
            }
        }
    }
}