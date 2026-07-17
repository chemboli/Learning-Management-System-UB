# HOW TO RUN — SmartLearning (invitation flow build)

## 0. What was fixed in this zip
- Removed the duplicate `resendInvitation` method in `UserController.java`
  (this ONE error was making the whole build fail — the 99 "cannot find symbol"
  Lombok errors were a chain reaction from it, nothing else was broken).
- Every Java file, the pom, and both YAML configs have been swept for
  duplicates and validated.
- `FRONTEND_URL` added to docker-compose so invitation-email links are correct.

## 1. Backend (Docker) — run from `work/SmartLearning-Backend`

    docker compose down
    docker compose build --no-cache app
    docker compose up -d

`--no-cache` matters ONCE after replacing the code, so Docker can't reuse a
stale layer. After this first clean build you can go back to plain
`docker compose up -d --build`.

### How to know it worked (before touching the UI)
Watch the logs:

    docker logs -f smart_learning_app

Within the first seconds of startup you should see Hibernate run:

    alter table users add column invitation_code varchar(255)
    alter table users add column invitation_expires_at timestamp(6)

That line = the new code is really running. If you don't see it (and the
columns don't exist yet), you are still on an old image.

## 2. Frontend — run from `work/Smartlearning-Frontend`

    npm install
    npm start        (or: npx ng serve)

Open http://localhost:4200

## 3. Test the new flow end-to-end
1. Log in as MASTER → Users → New user. Notice: NO password field anymore.
2. Fill name/email/role → "Create & send invitation".
3. The user appears in the list with an "Invited — pending" badge
   (+ a "Resend invite" button).
4. Check the recipient's inbox: styled HTML email with a big invitation code
   and an "Activate my account" button → http://localhost:4200/activate?code=XXXX
5. Opening the link verifies the code automatically, shows the user's
   name/email READ-ONLY, and asks them to choose a password.
6. After activating they're sent to the login page and can sign in.
7. Trying to log in BEFORE activating gives a clear
   "account has not been activated yet" message.

## 4. Config knobs (application.yml)
- app.frontend-url          → base URL used in the email link (env: FRONTEND_URL)
- app.invitation-expiry-days → default 7
- Invitation email template  → src/main/resources/templates/email/invitation-email.html

## 5. If Gmail rejects sending
The app uses your Gmail app password from docker-compose (MAIL_PASSWORD).
If mails silently don't arrive, check `docker logs smart_learning_app`
for "Failed to send invitation email" — account creation still succeeds;
just fix the mail credentials and hit "Resend invite" on that user.
