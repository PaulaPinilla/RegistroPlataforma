package co.edu.unipiloto.registroplataforma;

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

        public Item(int id, String titulo, String subtitulo, boolean marcada) {
            this.id = id;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.marcada = marcada;
        }
    }

    public interface OnItemClickListener {
        void onClick(Item item);
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
            binding.tvTitulo.setText(item.titulo);
            if (item.subtitulo != null && !item.subtitulo.isEmpty()) {
                binding.tvSubtitulo.setVisibility(View.VISIBLE);
                binding.tvSubtitulo.setText(item.subtitulo);
            } else {
                binding.tvSubtitulo.setVisibility(View.GONE);
            }
            binding.tvEstado.setText(item.marcada ? "" : "");
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }
}