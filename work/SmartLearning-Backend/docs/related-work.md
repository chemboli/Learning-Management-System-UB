# Related Work: Automated Assessment of Programming Assignments

## Auto-Grade (Lobe Nyoh Serge, University of Buea, 2020/2021)

The closest existing work to SmartLearning in the local academic context is **Auto-Grade**, a
Bachelor of Engineering dissertation submitted to the Department of Computer Engineering,
Faculty of Engineering and Technology, University of Buea, in the 2020/2021 academic year,
by Lobe Nyoh Serge (Matriculation Number: FE17A035), under the supervision of
Dr. SOP Deffo Lionel Landry and Mr. Nkemeni Valery.

Auto-Grade proposes and implements an automated assessment system for programming
assignments using **dynamic code analysis** — the same fundamental approach adopted in
SmartLearning. In dynamic analysis, an instructor specifies a set of inputs and their expected
outputs; the student's submitted source code is executed against these inputs, and the actual
outputs are compared to the expected outputs to produce a score. This approach is contrasted with
static analysis (which examines code structure without execution) and was chosen for the same
reasons in both systems: it measures whether the student's solution actually works, not merely
whether it is structurally similar to a reference, and it supports diverse implementation approaches
for the same problem.

### Similarities

| Feature | Auto-Grade | SmartLearning |
|---|---|---|
| Backend technology | Spring Boot (Java) | Spring Boot 3.5 (Java 17) |
| Test-case-based grading | ✅ Input/expected-output pairs | ✅ Input/expected-output pairs |
| Supported languages | Java, C, C++, Python | Java, C, C++, Python |
| Role-based access | Instructor / Student | STUDENT, LECTURER, ADMIN, MASTER |
| File upload submission | ✅ | ✅ |
| Course roster export (CSV) | ✅ | ✅ |
| Late submission handling | Penalty percentage | Configurable penalty percentage per assignment |
| In-browser code editor | ✅ (practice mode) | ✅ (submission mode, CodeMirror 6) |
| Assignment statistics/charts | ✅ Average score chart | ✅ Per-assignment score distribution and per-course average |

### Key Differences

**Execution sandbox.** Auto-Grade uses Docker containers as the sandboxing mechanism for
code execution — each submission runs inside an isolated container with its own filesystem and
network namespace. SmartLearning uses OS-process-level sandboxing via `ulimit` (CPU time
limit, virtual memory ceiling, file-size limit, maximum process count), applied directly to the
compiler and runtime processes within the Spring Boot container. The `ulimit` approach provides
meaningful isolation without requiring Docker-in-Docker (DinD) orchestration, which introduces
significant operational complexity and security concerns when Docker is itself the deployment
mechanism. The Auto-Grade dissertation explicitly identifies Docker-based execution as its
architectural centrepiece; SmartLearning deliberately chose not to use DinD, trading theoretical
isolation depth for operational simplicity and deploy-anywhere portability.

**Frontend technology.** Auto-Grade uses VueJS and the Quasar framework. SmartLearning
uses Angular 19, which provides strict TypeScript integration, signal-based reactivity, and a
component-scoping model that was better suited to the declarative, role-scoped UI requirements
of a multi-role LMS.

**File storage.** Auto-Grade uses Firebase Storage. SmartLearning uses MinIO, a
self-hosted S3-compatible object store, which keeps all student data on-premises and avoids
dependency on Google's cloud infrastructure — an important consideration for a Cameroonian
university deployment where data sovereignty and internet bandwidth costs are real constraints.

**AI-assisted features.** Auto-Grade does not include any AI-assisted features.
SmartLearning adds three novel AI features built on Google Gemini's free API tier: (1) AI-assisted
assignment draft generation (title, description, and test cases from a lecturer's short prompt);
(2) an AI-likelihood analysis tool that estimates how likely a submission's text is AI-generated,
surfaced as a triage signal for the lecturer rather than an automated verdict; and (3) a
personalized AI chat assistant that grounds its recommendations in each user's real, role-scoped
academic data (enrolled courses and submissions for students; taught courses and class-wide
grading patterns for lecturers).

**Plagiarism detection.** The Auto-Grade dissertation explicitly identifies plagiarism detection
as a limitation and future work item. SmartLearning's AI-likelihood analysis partially addresses
this gap, though it targets AI-generated content specifically rather than inter-student copying.
Full plagiarism detection between student submissions remains a gap in both systems.

**Database.** Auto-Grade uses MySQL. SmartLearning uses PostgreSQL, providing stronger
support for JSON column types (used for run-result storage) and more mature UUID-primary-key
handling with Hibernate.

### Conclusion

SmartLearning builds on the same foundational approach as Auto-Grade — dynamic analysis,
test-case-based scoring, Spring Boot backend, multi-language support — while modernising the
technology stack, adding role-scoped AI features not present in prior work, and making different
architectural trade-offs around sandboxing and storage that better fit the deployment context at a
Cameroonian university institution. The Auto-Grade dissertation serves as direct local validation
that this problem space is both well-defined and technically achievable within the UB Computer
Engineering programme.

---

### Reference

Lobe Nyoh Serge. *Analysis, Design and Implementation of an Application for Automated
Assessment of Programming Assignments*. Bachelor of Engineering Dissertation, Department of
Computer Engineering, Faculty of Engineering and Technology, University of Buea, Cameroon.
Academic Year 2020/2021. Supervisors: Dr. SOP Deffo Lionel Landry, Mr. Nkemeni Valery.
