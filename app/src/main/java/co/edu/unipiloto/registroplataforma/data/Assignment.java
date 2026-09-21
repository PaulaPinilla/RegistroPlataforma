package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "actividades")
public class Assignment {

    public static final String TIPO_ENTREGA_ARCHIVO = "ARCHIVO";
    public static final String TIPO_ENTREGA_TEXTO = "TEXTO";
    public static final String TIPO_ENTREGA_PREGUNTAS = "PREGUNTAS";

    public static final String CATEGORIA_TALLER = "TALLER";
    public static final String CATEGORIA_PARCIAL = "PARCIAL";
    public static final String CATEGORIA_QUIZ = "QUIZ";
    public static final String CATEGORIA_FORO = "FORO";
    public static final String CATEGORIA_VIDEO = "VIDEO";
    public static final String CATEGORIA_PREGUNTAS = "PREGUNTAS";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int unidadId;
    private String titulo;
    private String instrucciones;
    private long fechaInicio;
    private long fechaLimite;
    private String tipoEntrega;
    private String categoria;
    private boolean mostrarCalificacion;
    private int intentosPermitidos;

    public Assignment(int unidadId, String titulo, String instrucciones, long fechaInicio, long fechaLimite,
                      String tipoEntrega, String categoria, boolean mostrarCalificacion, int intentosPermitidos) {
        this.unidadId = unidadId;
        this.titulo = titulo;
        this.instrucciones = instrucciones;
        this.fechaInicio = fechaInicio;
        this.fechaLimite = fechaLimite;
        this.tipoEntrega = tipoEntrega;
        this.categoria = categoria;
        this.mostrarCalificacion = mostrarCalificacion;
        this.intentosPermitidos = intentosPermitidos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnidadId() { return unidadId; }

    public String getTitulo() { return titulo; }

    public String getInstrucciones() { return instrucciones; }

    public long getFechaInicio() { return fechaInicio; }

    public long getFechaLimite() { return fechaLimite; }

    public String getTipoEntrega() { return tipoEntrega; }

    public String getCategoria() { return categoria; }

    public boolean isMostrarCalificacion() { return mostrarCalificacion; }

    public int getIntentosPermitidos() { return intentosPermitidos; }

    public boolean yaComenzo() {
        return fechaInicio <= 0 || System.currentTimeMillis() >= fechaInicio;
    }
}