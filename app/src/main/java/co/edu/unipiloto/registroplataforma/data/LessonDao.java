package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LessonDao {

    @Insert
    long insertar(Lesson leccion);

    @Query("SELECT * FROM lecciones WHERE unidadId = :unidadId ORDER BY id ASC")
    List<Lesson> listarPorUnidad(int unidadId);

    @Query("SELECT * FROM lecciones WHERE id = :id LIMIT 1")
    Lesson obtenerPorId(int id);
}