package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "unidades")
public class Unit {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cursoId;
    private String titulo;

    public Unit(int cursoId, String titulo) {
        this.cursoId = cursoId;
        this.titulo = titulo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }

    public String getTitulo() { return titulo; }
}