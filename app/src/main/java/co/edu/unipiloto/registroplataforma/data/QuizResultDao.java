package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuizResultDao {

    @Insert
    long insertar(QuizResult resultado);

    @Query("SELECT * FROM resultados_evaluacion WHERE id = :id LIMIT 1")
    QuizResult obtenerPorId(int id);

    @Query("SELECT * FROM resultados_evaluacion WHERE actividadId = :actividadId AND estudianteId = :estudianteId " +
            "ORDER BY fechaEnvio DESC LIMIT 1")
    QuizResult obtenerUltimoResultado(int actividadId, int estudianteId);

    @Query("SELECT COUNT(*) FROM resultados_evaluacion WHERE actividadId = :actividadId AND estudianteId = :estudianteId")
    int contarIntentos(int actividadId, int estudianteId);

    @Query("SELECT * FROM resultados_evaluacion WHERE actividadId = :actividadId ORDER BY fechaEnvio DESC")
    List<QuizResult> listarPorActividad(int actividadId);

    @Query("UPDATE resultados_evaluacion SET calificacion = :calificacion, retroalimentacion = :retro WHERE id = :resultadoId")
    void calificar(int resultadoId, Double calificacion, String retro);
}