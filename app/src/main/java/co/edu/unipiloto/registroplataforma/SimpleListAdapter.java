package co.edu.unipiloto.registroplataforma;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import co.edu.unipiloto.registroplataforma.databinding.ItemSimpleBinding;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.ViewHolder> {

    public static class Item {
        public final int id;
        public final String titulo;
        public final String subtitulo;
        public final boolean marcada;
        public final boolean esEncabezado;
        public final boolean mostrarAcciones;

        public Item(int id, String titulo, String subtitulo, boolean marcada) {
            this(id, titulo, subtitulo, marcada, false);
        }

        public Item(int id, String titulo, String subtitulo, boolean marcada, boolean mostrarAcciones) {
            this.id = id;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.marcada = marcada;
            this.esEncabezado = false;
            this.mostrarAcciones = mostrarAcciones;
        }

        public Item(String tituloEncabezado) {
            this.id = -1;
            this.titulo = tituloEncabezado;
            this.subtitulo = null;
            this.marcada = false;
            this.esEncabezado = true;
            this.mostrarAcciones = false;
        }
    }

    public interface OnItemClickListener {
        void onClick(Item item);
        default void onLongClick(Item item) { }
        default void onEditar(Item item) { }
        default void onEliminar(Item item) { }
    }

    private final List<Item> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public SimpleListAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void actualizarDatos(List<Item> nuevos) {
        items.clear();
        items.addAll(nuevos);
        notifyDataSetChanged();
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSimpleBinding binding = ItemSimpleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSimpleBinding binding;

        ViewHolder(ItemSimpleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Item item) {
            if (item.esEncabezado) {
                binding.tvTitulo.setText(item.titulo);
                binding.tvTitulo.setTypeface(null, Typeface.BOLD);
                binding.tvTitulo.setTextColor(0xFF1565C0);
                binding.tvSubtitulo.setVisibility(View.GONE);
                binding.tvEstado.setText("");
                binding.layoutAcciones.setVisibility(View.GONE);
                binding.getRoot().setOnClickListener(null);
                binding.getRoot().setClickable(false);
            } else {
                binding.tvTitulo.setTypeface(null, Typeface.NORMAL);
                binding.tvTitulo.setTextColor(0xFF000000);
                binding.tvTitulo.setText(item.titulo);
                if (item.subtitulo != null && !item.subtitulo.isEmpty()) {
                    binding.tvSubtitulo.setVisibility(View.VISIBLE);
                    binding.tvSubtitulo.setText(item.subtitulo);
                } else {
                    binding.tvSubtitulo.setVisibility(View.GONE);
                }
                binding.tvEstado.setText("");
                binding.getRoot().setClickable(true);
                binding.getRoot().setOnClickListener(v -> listener.onClick(item));

                if (item.mostrarAcciones) {
                    binding.layoutAcciones.setVisibility(View.VISIBLE);
                    binding.btnEditar.setOnClickListener(v -> listener.onEditar(item));
                    binding.btnEliminar.setOnClickListener(v -> listener.onEliminar(item));
                } else {
                    binding.layoutAcciones.setVisibility(View.GONE);
                    binding.btnEditar.setOnClickListener(null);
                    binding.btnEliminar.setOnClickListener(null);
                }
            }
        }
    }
}