// Shared model interfaces — mirror backend DTOs in Final.year.project.SmartLearning.dto

export type Role = 'USER' | 'STUDENT' | 'LECTURER' | 'ADMIN' | 'MASTER';

export type NoteType = 'LECTURE_NOTE' | 'LAB' | 'ASSIGNMENT' | 'TUTORIAL' | 'PAST_EXAM';

export interface AuthResponse {
  token: string;
  email?: string;
  role?: Role;
  firstName?: string;
  lastName?: string;
  [key: string]: any;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  matricule?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  matricule?: string;
  role: Role;
  enabled: boolean;
  mustChangePassword?: boolean;
  /** True while the account awaits activation via its invitation code. */
  pendingActivation?: boolean;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  matricule?: string;
  enabled?: boolean;
}

export interface AdminCreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  matricule?: string;
  role: Role;
}

/** Identity fields loaded (read-only) after entering a valid invitation code. */
export interface InvitationDetails {
  firstName: string;
  lastName: string;
  email: string;
  matricule?: string;
  role: Role;
}

export interface ActivateAccountRequest {
  invitationCode: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CourseResponse {
  id: string;
  courseCode: string;
  title: string;
  description: string;
  creditHours: number;
  lecturerId: string;
  lecturerName: string;
  active: boolean;
}

export interface CreateCourseRequest {
  courseCode: string;
  title: string;
  description: string;
  creditHours: number;
  lecturerId: string;
}

export interface UpdateCourseRequest {
  title?: string;
  description?: string;
  creditHours?: number;
  lecturerId?: string;
  active?: boolean;
}

export interface EnrollmentResponse {
  id: string;
  studentId: string;
  studentName: string;
  courseId: string;
  courseCode: string;
  courseTitle: string;
  enrolledAt: string;
  active: boolean;
}

export interface EnrollRequest {
  courseId: string;
}

export interface NoteResponse {
  id: string;
  title: string;
  description: string;
  noteType: NoteType;
  weekNumber?: number;
  downloadUrl: string;
  fileName?: string;
  contentType?: string;
  fileSize?: number;
  courseTitle: string;
  lecturerName: string;
  uploadedAt: string;
}

export interface CreateNoteRequest {
  courseId: string;
  title: string;
  description: string;
  noteType: NoteType;
  weekNumber?: number;
  published: boolean;
}

export interface UpdateNoteRequest {
  title?: string;
  description?: string;
  noteType?: NoteType;
  weekNumber?: number;
  published?: boolean;
}

export type SubmissionStatus = 'NOT_SUBMITTED' | 'SUBMITTED' | 'REVIEWED';

export type AssignmentType = 'REGULAR' | 'PROGRAMMING';

export type ProgrammingLanguage = 'JAVA' | 'PYTHON' | 'C' | 'CPP';

export interface TestCaseDto {
  id?: string;
  sequence: number;
  label?: string;
  input: string;
  expectedOutput: string;
  weight: number;
  hidden: boolean;
}

export interface AssignmentResponse {
  id: string;
  title: string;
  description: string;
  dueDate: string;
  published: boolean;
  overdue: boolean;
  courseId: string;
  courseCode: string;
  courseTitle: string;
  lecturerName: string;
  createdAt: string;
  assignmentType: AssignmentType;
  programmingLanguage?: ProgrammingLanguage;
  maxScore: number;
  latePenaltyPercent: number;
  testCases?: TestCaseDto[];
  mySubmissionStatus?: SubmissionStatus;
  submissionCount?: number;
  averageScore?: number;
}

export interface CreateAssignmentRequest {
  courseId: string;
  title: string;
  description: string;
  dueDate: string;
  published: boolean;
  assignmentType?: AssignmentType;
  programmingLanguage?: ProgrammingLanguage;
  maxScore?: number;
  latePenaltyPercent?: number;
  testCases?: TestCaseDto[];
}

export interface UpdateAssignmentRequest {
  title?: string;
  description?: string;
  dueDate?: string;
  published?: boolean;
  assignmentType?: AssignmentType;
  programmingLanguage?: ProgrammingLanguage;
  maxScore?: number;
  latePenaltyPercent?: number;
  testCases?: TestCaseDto[];
}

export interface TestCaseResult {
  testCaseId: string;
  label?: string;
  hidden: boolean;
  input?: string;
  expectedOutput?: string;
  actualOutput: string;
  passed: boolean;
  weight: number;
  awarded: number;
  status: 'OK' | 'RUNTIME_ERROR' | 'TIMEOUT' | 'COMPILE_ERROR';
  errorOutput?: string;
  executionTimeMs: number;
}

export interface SubmissionResponse {
  id: string;
  assignmentId: string;
  assignmentTitle: string;
  studentId: string;
  studentName: string;
  fileName: string;
  downloadUrl: string;
  contentType?: string;
  fileSize?: number;
  comment: string;
  reviewed: boolean;
  late: boolean;
  submittedAt: string;
  reviewedAt?: string;
  score?: number;
  maxScore?: number;
  feedback?: string;
  gradedAt?: string;
  gradedByName?: string;
  autoGraded: boolean;
  canRun: boolean;
  hasReadableText: boolean;
  sourceCode?: string;
  lastRunResult?: TestCaseResult[];
  lastRunAt?: string;
  aiLikelihoodPercent?: number;
  aiAnalysisExplanation?: string;
  aiAnalyzedAt?: string;
}

export interface CreateSubmissionRequest {
  assignmentId: string;
  comment?: string;
}

export interface BatchRunResponse {
  totalSubmissions: number;
  ranCount: number;
  skippedCount: number;
  failedCount: number;
}

export interface GradeSubmissionRequest {
  score?: number;
  feedback?: string;
}

export type AnnouncementPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

export interface AnnouncementResponse {
  id: string;
  title: string;
  body: string;
  priority: AnnouncementPriority;
  courseId?: string;
  courseCode?: string;
  courseTitle?: string;
  sitewide: boolean;
  authorName: string;
  published: boolean;
  expired: boolean;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAnnouncementRequest {
  title: string;
  body: string;
  priority?: AnnouncementPriority;
  courseId?: string;
  published: boolean;
  expiresAt?: string;
}

export interface UpdateAnnouncementRequest {
  title?: string;
  body?: string;
  priority?: AnnouncementPriority;
  published?: boolean;
  expiresAt?: string;
}

// ---- AI features ----------------------------------------------------------

export interface GenerateAssignmentRequest {
  prompt: string;
  assignmentType: AssignmentType;
  programmingLanguage?: ProgrammingLanguage;
  testCaseCount?: number;
}

export interface GeneratedAssignmentResponse {
  title: string;
  description: string;
  suggestedMaxScore: number;
  testCases?: TestCaseDto[];
  disclaimer: string;
}

export interface AiAnalysisResponse {
  aiLikelihoodPercent: number;
  explanation: string;
  disclaimer: string;
  analyzedAt: string;
}
