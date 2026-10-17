package co.edu.unipiloto.registroplataforma.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {User.class, Course.class, Enrollment.class, Unit.class, Lesson.class,
                LessonProgress.class, Assignment.class, Submission.class, Question.class,
                QuizResult.class, Forum.class, ForumPost.class, ForumGrade.class},
        version = 23,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract CourseDao courseDao();
    public abstract EnrollmentDao enrollmentDao();
    public abstract UnitDao unitDao();
    public abstract LessonDao lessonDao();
    public abstract LessonProgressDao lessonProgressDao();
    public abstract AssignmentDao assignmentDao();
    public abstract SubmissionDao submissionDao();
    public abstract QuestionDao questionDao();
    public abstract QuizResultDao quizResultDao();
    public abstract ForumDao forumDao();
    public abstract ForumPostDao forumPostDao();
    public abstract ForumGradeDao forumGradeDao();

    private static volatile AppDatabase INSTANCE;

    /** 21 -> 22: porcentajes por actividad, por foro y peso de cada corte en el curso */
    static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE actividades ADD COLUMN porcentaje REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE foros ADD COLUMN porcentaje REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE cursos ADD COLUMN pesoCorte1 REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE cursos ADD COLUMN pesoCorte2 REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE cursos ADD COLUMN pesoCorte3 REAL NOT NULL DEFAULT 0");
        }
    };

    /** 22 -> 23: fecha de creación de cada actividad (para los avisos de actividades nuevas) */
    static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE actividades ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "campus_virtual_db"
                            )
                            .addMigrations(MIGRATION_21_22, MIGRATION_22_23)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}