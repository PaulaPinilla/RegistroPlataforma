package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ForumGradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(ForumGrade calificacion);

    @Query("SELECT * FROM calificaciones_foro WHERE foroId = :foroId")
    List<ForumGrade> listarPorForo(int foroId);

    @Query("SELECT * FROM calificaciones_foro WHERE foroId = :foroId AND estudianteId = :estudianteId LIMIT 1")
    ForumGrade obtenerPorForoYEstudiante(int foroId, int estudianteId);

    @Query("SELECT fg.* FROM calificaciones_foro fg INNER JOIN foros f ON fg.foroId = f.id WHERE f.cursoId = :cursoId AND fg.estudianteId = :estudianteId")
    List<ForumGrade> listarPorCursoYEstudiante(int cursoId, int estudianteId);

    /** Borra todas las calificaciones de un foro (se usa al eliminar el foro) */
    @Query("DELETE FROM calificaciones_foro WHERE foroId = :foroId")
    void eliminarPorForo(int foroId);
}