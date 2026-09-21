package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "preguntas")
public class Question {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int actividadId;
    private String enunciado;
    private String opciones;

    public Question(int actividadId, String enunciado, String opciones) {
        this.actividadId = actividadId;
        this.enunciado = enunciado;
        this.opciones = opciones;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActividadId() { return actividadId; }

    public String getEnunciado() { return enunciado; }

    public String getOpciones() { return opciones; }

    public boolean tieneOpciones() { return opciones != null && !opciones.isEmpty(); }

    public String[] getListaOpciones() {
        return tieneOpciones() ? opciones.split("\\|") : new String[0];
    }
}
