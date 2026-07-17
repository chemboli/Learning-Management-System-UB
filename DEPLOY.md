# Deploying SmartLearning to Render

## 0. Rotate your secrets FIRST — do this regardless of Render

Your zip has real credentials committed to git (`docker-compose.yml` and
`application.yml` are both tracked, and your repo has a GitHub remote —
`github.com/chemboli/Final-Year-Project-SmartLearning`):

- **Gmail app password** (`ropd eioh ybtv asvm`) — go to your Google Account →
  Security → App passwords, **revoke this one**, generate a new one, and
  never put it back in a tracked file.
- **Gemini API key** in `.env` — this file is gitignored so it's not in git
  history, but rotate it at https://aistudio.google.com anyway since it's
  been floating around in a shared zip.
- **JWT secret / MinIO admin password** — these were only ever placeholder
  dev values (`password123`, the "THIS_IS_A_SUPER..." string), but you'll
  set real ones as Render env vars below, so they never need to be in a file
  again.

I've already changed the code so none of these need to be hardcoded
anywhere — see "What changed in the code" below.

## 1. Object storage: MinIO won't run as-is on Render

Render has no managed object storage, and it doesn't support
`docker-compose` (each container becomes its own service, so `db`, `minio`,
`pgadmin`, `app` can't just be lifted as one unit). Two options:

- **Recommended: Cloudflare R2** (S3-compatible, free 10GB/month, no credit
  card). Your code already talks to storage through the official MinIO Java
  SDK, which speaks plain S3 API — so R2 works as a drop-in, no code changes:
  1. Create a Cloudflare account → R2 → create a bucket named `smartlearning`.
  2. Create an R2 API token (Account → R2 → Manage API Tokens) — this gives
     you an access key + secret key.
  3. Your endpoint is `https://<account-id>.r2.cloudflarestorage.com`.
  4. Enable public access (or a custom domain) on the bucket for
     `MINIO_PUBLIC_URL`, since that's what's used to build the presigned
     download links your browser hits directly.

- **Alternative: self-host MinIO on Render** as its own "Private Service"
  with a persistent disk (requires a paid instance type — persistent disks
  disable zero-downtime deploys and aren't on the free tier). More moving
  parts for a capstone project; R2 is simpler.

## 2. What changed in the code (already done for you)

- `application.yml`: datasource URL/user/password, MinIO url/keys/region/
  bucket, JWT secret, and `server.port` are now all read from environment
  variables with safe localhost defaults for local dev. Nothing sensitive is
  hardcoded anymore.
- `SecurityConfig.java`: CORS now also accepts origins from
  `APP_CORS_ALLOWED_ORIGINS` (comma-separated), in addition to
  `localhost:4200` for local dev.
- `environment.prod.ts`: `apiUrl` now points at a full backend URL
  (`https://YOUR-BACKEND-SERVICE.onrender.com/api`) instead of the relative
  `/api`, since on Render your frontend and backend live on two different
  subdomains — a relative path would just hit the frontend's own domain.
  **You must edit this to your real backend URL once you have it (step 4).**
- Added `render.yaml` (Render "Blueprint") and this guide.

## 3. Deploy the database + backend

**Option A — Blueprint (fastest):** push this repo to GitHub, then in Render
click **New → Blueprint**, point it at the repo. It reads `render.yaml` and
creates the Postgres DB, the backend web service, and the frontend static
site in one go. You'll still need to fill in the env vars marked
`sync: false` in the Render dashboard (see list below) — Render won't
invent secrets for you.

**Option B — Manual:**
1. **New → PostgreSQL** → name it `smartlearning-db`, free plan. Once
   created, open it and copy the **Internal Database URL**, e.g.
   `postgres://user:pass@dpg-xxxx-a/smartlearning_db`.
2. **New → Web Service** → connect your repo → Runtime: **Docker** →
   Dockerfile path: `work/SmartLearning-Backend/Dockerfile`, Docker
   context: `work/SmartLearning-Backend`.
3. Add these environment variables on the web service:

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/<dbname>` (build this from the Postgres info page — Spring needs the `jdbc:` form, not the `postgres://` one) |
   | `SPRING_DATASOURCE_USERNAME` | from the Postgres info page |
   | `SPRING_DATASOURCE_PASSWORD` | from the Postgres info page |
   | `JWT_SECRET` | a new random 32+ char string |
   | `GEMINI_API_KEY` | your rotated key |
   | `MAIL_USERNAME` | your Gmail address |
   | `MAIL_PASSWORD` | your new Gmail app password |
   | `MINIO_URL` / `MINIO_PUBLIC_URL` | your R2 endpoint (both the same, unless you set up a custom public domain) |
   | `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | your R2 token keys |
   | `MINIO_REGION` | `auto` |
   | `MINIO_BUCKET_NAME` | `smartlearning` |
   | `FRONTEND_URL` | filled in step 4, once you have it |
   | `APP_CORS_ALLOWED_ORIGINS` | filled in step 4, once you have it |

   (`PORT` is injected automatically by Render — the code already reads it.)
4. Deploy. Once live, note the backend URL, e.g.
   `https://smartlearning-backend.onrender.com`.

## 4. Deploy the frontend

1. Edit `work/Smartlearning-Frontend/src/environments/environment.prod.ts`,
   set `apiUrl` to `https://smartlearning-backend.onrender.com/api` (your
   real backend URL from step 3), commit and push.
2. **New → Static Site** → connect your repo:
   - Build command: `cd work/Smartlearning-Frontend && npm ci && npm run build -- --configuration production`
   - Publish directory: `work/Smartlearning-Frontend/dist/smartlearning-frontend/browser`
   - Add a rewrite rule so Angular's client-side routing works on refresh:
     source `/*` → destination `/index.html` (already in `render.yaml` if
     you used the Blueprint).
3. Deploy. Note the resulting URL, e.g.
   `https://smartlearning-frontend.onrender.com`.

## 5. Wire the two together

Back on the **backend** service, set:
- `FRONTEND_URL` = `https://smartlearning-frontend.onrender.com`
- `APP_CORS_ALLOWED_ORIGINS` = `https://smartlearning-frontend.onrender.com`

Redeploy the backend so both take effect. Then open the frontend URL and
test login, file upload, and the invitation email flow end-to-end.

## Notes / gotchas

- Render's **free web service instances spin down after inactivity** and take
  ~30-60s to wake on the next request — expect a cold-start delay on your
  first request after idling. Fine for a capstone demo; mention it if you're
  presenting live.
- Free Postgres on Render **expires after 90 days**. Fine for now, just know
  you'll need to recreate it (and update the datasource env vars) if the
  project needs to keep running past that.
- The code-execution feature (Python/gcc/g++ for auto-grading submissions)
  runs inside the same container via the Dockerfile's `apt-get install`
  step — this works on Render's Docker runtime as-is, no changes needed.
