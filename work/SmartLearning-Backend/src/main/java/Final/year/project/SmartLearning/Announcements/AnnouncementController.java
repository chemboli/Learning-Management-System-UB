package Final.year.project.SmartLearning.Announcements;

import Final.year.project.SmartLearning.dto.AnnouncementResponse;
import Final.year.project.SmartLearning.dto.CreateAnnouncementRequest;
import Final.year.project.SmartLearning.dto.UpdateAnnouncementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public AnnouncementResponse create(@RequestBody CreateAnnouncementRequest request) {
        return announcementService.create(request);
    }

    @GetMapping("/sitewide")
    public List<AnnouncementResponse> getSitewide() {
        return announcementService.getSitewide();
    }

    @GetMapping("/course/{courseId}")
    public List<AnnouncementResponse> getForCourse(@PathVariable UUID courseId) {
        return announcementService.getForCourse(courseId);
    }

    /** Personalized feed: sitewide + the user's own course announcements, for dashboards/notifications. */
    @GetMapping("/feed")
    public List<AnnouncementResponse> getFeed() {
        return announcementService.getFeedForCurrentUser();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public AnnouncementResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateAnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
