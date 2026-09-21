package co.edu.unipiloto.registroplataforma.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {User.class, Course.class, Enrollment.class, Unit.class, Lesson.class,
                LessonProgress.class, Assignment.class, Submission.class, Question.class,
                QuizResult.class, Forum.class, ForumPost.class},
        version = 13,
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

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "campus_virtual_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}