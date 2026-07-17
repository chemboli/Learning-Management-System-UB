package Final.year.project.SmartLearning.Notes;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Courses.CourseRepository;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.CreateNoteRequest;
import Final.year.project.SmartLearning.dto.NoteResponse;
import Final.year.project.SmartLearning.dto.UpdateNoteRequest;
import Final.year.project.SmartLearning.storage.MinioService;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;

    @Qualifier("publicMinioClient")
    private final MinioClient publicMinioClient;

    public NoteResponse upload(
            MultipartFile file,
            CreateNoteRequest request) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        boolean isLecturer = user.getRole().name().equals("LECTURER");
        boolean isAdmin = user.getRole().name().equals("ADMIN");
        boolean isMaster = user.getRole().name().equals("MASTER");

        // SECURITY RULE:
        // Lecturer can ONLY upload to their own course
        if (isLecturer) {

            if (!course.getLecturer().getId().equals(user.getId())) {
                throw new RuntimeException("You are not assigned to this course");
            }
        }

        // ADMIN + MASTER => allowed for ALL courses

        String objectName = minioService.uploadFile(
                file,
                "notes/" + course.getCourseCode()
        );

        Note note = Note.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .noteType(request.getNoteType())
                .weekNumber(request.getWeekNumber())
                .published(request.isPublished())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .objectName(objectName)
                .course(course)
                .lecturer(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        note = noteRepository.save(note);

        String url = generateUrl(objectName);

        return toResponse(note, url);
    }

    public List<NoteResponse> getCourseNotes(UUID courseId) {

        return noteRepository.findByCourseIdAndPublishedTrue(courseId)
                .stream()
                .map(n -> toResponse(n, generateUrl(n.getObjectName())))
                .toList();
    }

    private String generateUrl(String objectName) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("smartlearning")
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private NoteResponse toResponse(Note note, String url) {

        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .description(note.getDescription())
                .noteType(note.getNoteType())
                .weekNumber(note.getWeekNumber())
                .downloadUrl(url)
                .fileName(note.getFileName())
                .contentType(note.getContentType())
                .fileSize(note.getFileSize())
                .courseTitle(note.getCourse().getTitle())
                .lecturerName(
                        note.getLecturer().getFirstName() + " " +
                        note.getLecturer().getLastName()
                )
                .uploadedAt(note.getUploadedAt())
                .build();
    }
    public List<NoteResponse> getByCourseCode(String courseCode) {

        return noteRepository.findByCourseCourseCode(courseCode)
                .stream()
                .map(n -> toResponse(n, generateUrl(n.getObjectName())))
                .toList();
    }

    public NoteResponse getNote(UUID id) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        return toResponse(note, generateUrl(note.getObjectName()));
    }

    public NoteResponse updateNote(UUID id, UpdateNoteRequest request) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        checkOwnership(note);

        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            note.setDescription(request.getDescription());
        }

        if (request.getNoteType() != null) {
            note.setNoteType(request.getNoteType());
        }

        if (request.getWeekNumber() != null) {
            note.setWeekNumber(request.getWeekNumber());
        }

        if (request.getPublished() != null) {
            note.setPublished(request.getPublished());
        }

        note = noteRepository.save(note);

        return toResponse(note, generateUrl(note.getObjectName()));
    }

    public void deleteNote(UUID id) {

        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        checkOwnership(note);

        minioService.deleteFile(note.getObjectName());

        noteRepository.delete(note);
    }

    private void checkOwnership(Note note) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isLecturer = user.getRole().name().equals("LECTURER");

        if (isLecturer && !note.getLecturer().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to modify this note");
        }
    }
}