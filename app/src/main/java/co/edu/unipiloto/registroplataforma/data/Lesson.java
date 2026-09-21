package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lecciones")
public class Lesson {

    public static final String TIPO_VIDEO = "VIDEO";
    public static final String TIPO_DOCUMENTO = "DOCUMENTO";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int unidadId;
    private String titulo;
    private String tipo;
    private String urlMaterial;
    private String contenidoTexto;

    public Lesson(int unidadId, String titulo, String tipo, String urlMaterial, String contenidoTexto) {
        this.unidadId = unidadId;
        this.titulo = titulo;
        this.tipo = tipo;
        this.urlMaterial = urlMaterial;
        this.contenidoTexto = contenidoTexto;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnidadId() { return unidadId; }

    public String getTitulo() { return titulo; }

    public String getTipo() { return tipo; }

    public String getUrlMaterial() { return urlMaterial; }

    public String getContenidoTexto() { return contenidoTexto; }

    public boolean esTexto() { return contenidoTexto != null; }
}