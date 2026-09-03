package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface LessonProgressDao {

    @Insert
    long insertar(LessonProgress progreso);

    @Query("SELECT COUNT(*) FROM progreso_lecciones WHERE leccionId = :leccionId AND estudianteId = :estudianteId")
    int existeProgreso(int leccionId, int estudianteId);
}