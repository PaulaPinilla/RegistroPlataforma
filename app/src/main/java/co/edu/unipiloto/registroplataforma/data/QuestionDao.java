package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuestionDao {

    @Insert
    long insertar(Question pregunta);

    @Query("SELECT * FROM preguntas WHERE actividadId = :actividadId ORDER BY id ASC")
    List<Question> listarPorActividad(int actividadId);
}