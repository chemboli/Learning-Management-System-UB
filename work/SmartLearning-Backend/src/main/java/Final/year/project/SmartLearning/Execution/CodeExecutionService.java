package Final.year.project.SmartLearning.Execution;

import Final.year.project.SmartLearning.Assignments.ProgrammingLanguage;
import Final.year.project.SmartLearning.Assignments.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes untrusted student-submitted code against lecturer-defined stdin/stdout
 * test cases.
 *
 * IMPORTANT — SECURITY MODEL:
 * This is process-level sandboxing, not container-level. Each run gets:
 *  - a fresh, dedicated scratch directory (deleted afterwards)
 *  - a hard wall-clock timeout per test case
 *  - stdout/stderr size caps (to stop runaway output from exhausting memory/disk)
 *  - best-effort resource limits via `ulimit` on Linux (CPU time, memory, processes, file size)
 *  - no network access is granted explicitly (though full network namespace isolation
 *    would require containers/seccomp, which are out of scope for this in-process runner)
 *
 * If your deployment can run Docker-in-Docker or gVisor/firecracker-backed sandboxes,
 * swapping the process-launch logic below for "docker run --rm --network=none ..."
 * per language image would be a meaningful additional hardening step.
 */
@Slf4j
@Service
public class CodeExecutionService {

    @Value("${execution.workdir:/tmp/sl-exec}")
    private String baseWorkDir;

    @Value("${execution.timeout-seconds:8}")
    private int timeoutSeconds;

    @Value("${execution.compile-timeout-seconds:20}")
    private int compileTimeoutSeconds;

    @Value("${execution.max-output-chars:20000}")
    private int maxOutputChars;

    @Value("${execution.memory-limit-kb:262144}") // 256 MB
    private long memoryLimitKb;

    public ExecutionRunResult run(
            ProgrammingLanguage language,
            String sourceCode,
            List<TestCase> testCases) {

        Path runDir;
        try {
            Path base = Paths.get(baseWorkDir);
            Files.createDirectories(base);
            runDir = Files.createTempDirectory(base, "run-");
        } catch (IOException e) {
            throw new RuntimeException("Could not allocate execution sandbox directory", e);
        }

        try {
            CompileOutcome compileOutcome = compile(language, sourceCode, runDir);

            if (!compileOutcome.success()) {
                return ExecutionRunResult.builder()
                        .compiled(false)
                        .compileError(truncate(compileOutcome.errorOutput()))
                        .testCaseResults(List.of())
                        .totalAwarded(0)
                        .totalPossible(testCases.stream().mapToDouble(TestCase::getWeight).sum())
                        .passedCount(0)
                        .totalCount(testCases.size())
                        .build();
            }

            List<TestCaseResult> results = new ArrayList<>();
            double totalAwarded = 0;
            double totalPossible = 0;
            int passedCount = 0;

            for (TestCase tc : testCases) {
                TestCaseResult result = runOne(language, runDir, tc);
                results.add(result);
                totalPossible += tc.getWeight();
                totalAwarded += result.getAwarded();
                if (result.isPassed()) passedCount++;
            }

            return ExecutionRunResult.builder()
                    .compiled(true)
                    .testCaseResults(results)
                    .totalAwarded(totalAwarded)
                    .totalPossible(totalPossible)
                    .passedCount(passedCount)
                    .totalCount(testCases.size())
                    .build();

        } finally {
            deleteRecursively(runDir);
        }
    }

    // ---- Compilation ---------------------------------------------------

    private record CompileOutcome(boolean success, String errorOutput) {}

    private CompileOutcome compile(ProgrammingLanguage language, String sourceCode, Path runDir) {
        try {
            switch (language) {
                case PYTHON -> {
                    Files.writeString(runDir.resolve("main.py"), sourceCode, StandardCharsets.UTF_8);
                    // Python is interpreted; do a syntax-only compile check.
                    ProcessResult pr = runProcess(
                            runDir,
                            List.of("python3", "-m", "py_compile", "main.py"),
                            null,
                            compileTimeoutSeconds,
                            null
                    );
                    return new CompileOutcome(pr.exitCode() == 0, pr.stderr());
                }
                case JAVA -> {
                    // Student code must define a public class Main for this to work.
                    Files.writeString(runDir.resolve("Main.java"), sourceCode, StandardCharsets.UTF_8);
                    ProcessResult pr = runProcess(
                            runDir,
                            List.of("javac", "Main.java"),
                            null,
                            compileTimeoutSeconds,
                            null
                    );
                    return new CompileOutcome(pr.exitCode() == 0, pr.stderr());
                }
                case C -> {
                    Files.writeString(runDir.resolve("main.c"), sourceCode, StandardCharsets.UTF_8);
                    ProcessResult pr = runProcess(
                            runDir,
                            List.of("gcc", "main.c", "-O2", "-o", "main_exec", "-lm"),
                            null,
                            compileTimeoutSeconds,
                            null
                    );
                    return new CompileOutcome(pr.exitCode() == 0, pr.stderr());
                }
                case CPP -> {
                    Files.writeString(runDir.resolve("main.cpp"), sourceCode, StandardCharsets.UTF_8);
                    ProcessResult pr = runProcess(
                            runDir,
                            List.of("g++", "main.cpp", "-O2", "-std=c++17", "-o", "main_exec"),
                            null,
                            compileTimeoutSeconds,
                            null
                    );
                    return new CompileOutcome(pr.exitCode() == 0, pr.stderr());
                }
                default -> throw new IllegalArgumentException("Unsupported language: " + language);
            }
        } catch (IOException e) {
            return new CompileOutcome(false, "Internal error preparing sandbox: " + e.getMessage());
        }
    }

    // ---- Execution -------------------------------------------------------

    private TestCaseResult runOne(ProgrammingLanguage language, Path runDir, TestCase tc) {
        List<String> command = runCommand(language);

        ProcessResult pr;
        try {
            pr = runProcess(runDir, command, tc.getInput(), timeoutSeconds, language);
        } catch (IOException e) {
            return TestCaseResult.builder()
                    .testCaseId(tc.getId())
                    .label(tc.getLabel())
                    .hidden(tc.isHidden())
                    .input(tc.isHidden() ? null : tc.getInput())
                    .expectedOutput(tc.isHidden() ? null : tc.getExpectedOutput())
                    .actualOutput("")
                    .passed(false)
                    .weight(tc.getWeight())
                    .awarded(0)
                    .status("RUNTIME_ERROR")
                    .errorOutput("Failed to start process: " + e.getMessage())
                    .executionTimeMs(0)
                    .build();
        }

        String actual = normalize(pr.stdout());
        String expected = normalize(tc.getExpectedOutput());

        boolean passed = !pr.timedOut() && pr.exitCode() == 0 && actual.equals(expected);

        String status = pr.timedOut() ? "TIMEOUT" : (pr.exitCode() != 0 ? "RUNTIME_ERROR" : "OK");

        return TestCaseResult.builder()
                .testCaseId(tc.getId())
                .label(tc.getLabel())
                .hidden(tc.isHidden())
                .input(tc.isHidden() ? null : tc.getInput())
                .expectedOutput(tc.isHidden() ? null : tc.getExpectedOutput())
                .actualOutput(truncate(pr.stdout()))
                .passed(passed)
                .weight(tc.getWeight())
                .awarded(passed ? tc.getWeight() : 0)
                .status(status)
                .errorOutput(truncate(pr.stderr()))
                .executionTimeMs(pr.elapsedMs())
                .build();
    }

    private List<String> runCommand(ProgrammingLanguage language) {
        return switch (language) {
            case PYTHON -> List.of("python3", "main.py");
            case JAVA -> List.of(
                    "java",
                    "-XX:+UseSerialGC",
                    "-Xmx256m",
                    "-XX:ReservedCodeCacheSize=64m", // default reservation (~240MB) alone exceeds our whole vmem ceiling
                    "-Xss512k",                       // smaller thread stacks — student programs don't need 1MB/thread
                    "Main"
            );
            case C, CPP -> List.of("./main_exec");
        };
    }

    /** Normalizes line endings and trims trailing whitespace per line for forgiving comparison. */
    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n")
                .stripTrailing()
                .lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    private String truncate(String s) {
        if (s == null) return "";
        if (s.length() <= maxOutputChars) return s;
        return s.substring(0, maxOutputChars) + "\n...[output truncated]";
    }

    // ---- Process launching with timeout + best-effort resource limits ----

    private record ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long elapsedMs) {}

    private ProcessResult runProcess(
            Path workDir,
            List<String> command,
            String stdin,
            int timeoutSeconds,
            ProgrammingLanguage limitForLanguage) throws IOException {

        List<String> fullCommand = limitForLanguage != null
                ? wrapWithUlimit(command, limitForLanguage)
                : command;

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        long start = System.currentTimeMillis();
        Process process = pb.start();

        if (stdin != null && !stdin.isEmpty()) {
            try {
                process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // process may have already exited / closed stdin — safe to ignore
            }
        }
        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {}

        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream(), maxOutputChars);
        StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream(), maxOutputChars);
        Thread outThread = new Thread(outGobbler);
        Thread errThread = new Thread(errGobbler);
        outThread.start();
        errThread.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }

        boolean timedOut = !finished;

        if (timedOut) {
            process.destroyForcibly();
        }

        try {
            outThread.join(2000);
            errThread.join(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        long elapsed = System.currentTimeMillis() - start;
        int exitCode = timedOut ? -1 : process.exitValue();

        return new ProcessResult(exitCode, outGobbler.getOutput(), errGobbler.getOutput(), timedOut, elapsed);
    }

    /**
     * Wraps a command with `bash -c 'ulimit ...; exec "$@"'` to apply best-effort
     * resource limits on Linux. CPU time, virtual memory, max file size, and max
     * process count are all capped to reduce the blast radius of misbehaving or
     * malicious submissions (fork bombs, huge allocations, runaway disk writes).
     *
     * The virtual memory ceiling is language-specific: a JVM needs significant
     * virtual address space beyond its heap just to start up (JIT code cache,
     * metaspace, thread stacks, loaded native libraries) — capping it at the
     * same tight limit used for a small C binary causes the JVM to fail before
     * the student's code even runs ("Could not reserve enough space for code
     * cache"). Python similarly needs more headroom than a bare C/C++ binary
     * for the interpreter itself. Each language gets a ceiling comfortably
     * above its own baseline footprint, while still bounding worst-case usage.
     *
     * If `bash` or `ulimit` are unavailable (non-Linux dev environments), this
     * falls back to running the command directly — the JVM-level timeout in
     * runProcess() above is still in effect either way.
     */
    private List<String> wrapWithUlimit(List<String> command, ProgrammingLanguage language) {
        if (!isLinux()) {
            return command;
        }

        String cpuSeconds = String.valueOf(timeoutSeconds + 2);
        long vmLimitKb = virtualMemoryLimitKb(language);

        String script = String.join(" ",
                "ulimit -t " + cpuSeconds + ";",        // CPU time (seconds)
                "ulimit -v " + vmLimitKb + ";",          // virtual memory (KB)
                "ulimit -f " + (20 * 1024) + ";",        // max file size (KB) — 20MB
                "ulimit -u 64;",                          // max user processes/threads
                "exec \"$@\""
        );

        List<String> wrapped = new ArrayList<>();
        wrapped.add("bash");
        wrapped.add("-c");
        wrapped.add(script);
        wrapped.add("--"); // "$0" placeholder consumed by bash -c
        wrapped.addAll(command);
        return wrapped;
    }

    /**
     * The configured memoryLimitKb (default 256MB) is a sensible ceiling for a
     * student's actual C/C++/Python program, but a JVM needs several hundred MB
     * of additional virtual address space on top of its heap just to boot. Java
     * gets a much larger ceiling here; the heap itself is still capped via
     * -Xmx in runCommand(), so a runaway Java program's actual memory use is
     * bounded the same way as the others — this only widens the address-space
     * limit the JVM needs to exist at all.
     */
    private long virtualMemoryLimitKb(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> Math.max(memoryLimitKb, 1_536_000); // ~1.5 GB floor for JVM startup overhead
            case PYTHON -> Math.max(memoryLimitKb, 524_288); // ~512 MB floor for the interpreter itself
            case C, CPP -> memoryLimitKb;
        };
    }

    private boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {}
                            });
                }
            }
        } catch (IOException e) {
            log.warn("Failed to clean up execution sandbox dir {}: {}", path, e.getMessage());
        }
    }

    /** Reads a process stream incrementally, capping how much is buffered in memory. */
    private static class StreamGobbler implements Runnable {
        private final java.io.InputStream inputStream;
        private final int maxChars;
        private final StringBuilder buffer = new StringBuilder();

        StreamGobbler(java.io.InputStream inputStream, int maxChars) {
            this.inputStream = inputStream;
            this.maxChars = maxChars;
        }

        @Override
        public void run() {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                char[] chunk = new char[4096];
                int n;
                while ((n = reader.read(chunk)) != -1) {
                    if (buffer.length() < maxChars) {
                        buffer.append(chunk, 0, n);
                    }
                    // Once over the cap we keep draining the stream (to avoid blocking
                    // the child process on a full pipe buffer) but stop retaining it.
                }
            } catch (IOException ignored) {
                // stream closed because the process was killed — expected on timeout
            }
        }

        synchronized String getOutput() {
            return buffer.toString();
        }
    }
}
