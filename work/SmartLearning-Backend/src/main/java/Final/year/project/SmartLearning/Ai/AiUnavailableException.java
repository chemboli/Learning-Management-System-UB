package Final.year.project.SmartLearning.Ai;

/** Thrown when the AI provider is unreachable, misconfigured, or returns an error. */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message) {
        super(message);
    }
}
