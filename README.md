# ORSV0 — Online Result System (Release 2.1.1)

**Build Identifier:** `ORS-2.1.1-RELEASE-20261015`

## 1. Quick Build & Test Instructions

Ensure JDK 11+ and Apache Maven 3.8+ are installed on your `PATH`:

```bash
mvn clean test package
```

This compiles the Java sources, runs the JUnit 5 tests under `src/test/java`, and produces `target/ORSV0.war`. Use the test count and pass/fail summary from the current Maven run; archived Surefire reports are not evidence for the current source. The integration/latency test uses in-memory HSQLDB; it does not prove performance against a live MySQL/Tomcat deployment.

## 2. Create a clean source-only release ZIP

Run this from PowerShell in the project root when packaging a submission or handoff:

```powershell
.\Create-CleanRelease.ps1
```

The script creates a timestamped ZIP under `release\` and excludes `target\`, compiled classes/WAR files, bundled local Tomcat folders, logs, previous ZIPs and the `release\` output directory. The source ZIP is separate from the built deployable artifact at `target\ORSV0.war`.

## 3. Controlled Tomcat Deployment

Set `TOMCAT_HOME` (or `CATALINA_HOME`) explicitly before deploying (neither `pom.xml`, `build.xml`, `run.bat`, nor `deploy.sh` contains hardcoded developer paths or deletes unrelated webapps):

- **Linux / macOS:**
  ```bash
  export TOMCAT_HOME=/path/to/apache-tomcat
  ./deploy.sh --deploy
  ```
- **Windows:**
  ```cmd
  set TOMCAT_HOME=C:\path\to\apache-tomcat
  run.bat --deploy
  ```
- **Maven Profile:**
  ```bash
  mvn clean test install -Pdeploy-tomcat -Dtomcat.home=/path/to/apache-tomcat
  ```

After deployment, verify the running build identifier at `GET /ORSV0/version` or `GET /ORSV0/HelloServlet` (or `/ORSV0/VERSION.txt`).

## 4. Password Hashing, Legacy SHA-256 Upgrade & Controlled Reset Policy

- **PBKDF2-HMAC-SHA-256 (`PBKDF2WithHmacSHA256`):** All newly created and updated passwords are stored with 65,536 iterations and a unique 128-bit cryptographically random salt (`PBKDF2WithHmacSHA256$65536$<saltHex>$<hashHex>`).
- **Legacy SHA-256 Migration:** Existing accounts stored as 64-character hex SHA-256 digests are verified on login and transparently upgraded in-place to `PBKDF2WithHmacSHA256`.
- **Plaintext Password Policy:** Plaintext passwords are **never** accepted for direct authentication (`PasswordUtility.verify()` / `verifyPassword()` always returns `false` for unhashed values). If a legacy database row contains an unhashed password, `UserModel.authenticate()` rejects direct login without locking the account and directs the user to complete a controlled password reset via a 15-minute single-use hashed token on `/ForgetPasswordCtl`.

## 5. Canonical Database Schema & Safe Migration

- **Canonical Tables:**
  - `st_enrollment` (`STUDENT_ID`, `COURSE_ID`, `ROLL_NO`, `SEMESTER`, `STATUS = 'ACTIVE'`) is the single authoritative enrollment table across `EnrollmentModel`, `DepartmentOverviewModel`, `StudentModel`, and `MarksheetModel`.
  - `st_marksheet` (`STUDENT_ID`, `ROLL_NO`, `COURSE_ID`, `COURSE_NAME`, `SUBJECT_ID`, `SUBJECT_NAME`, `MARKS`) is the single authoritative marks table with composite uniqueness on `(STUDENT_ID, COURSE_ID, SUBJECT_ID)` and `(ROLL_NO, COURSE_ID, SUBJECT_ID)`.
- **Fresh Demo Setup Only (`database/demo_ors.sql`):**
  Contains `DROP TABLE` statements for initializing a brand-new `p04` schema with PBKDF2-HMAC-SHA-256 demo accounts (`an@gmail.com` / `Admin123`, `manish@gmail.com` / `Stud1234`, `chair@gmail.com` / `Chair123`, `ram@gmail.com` / `Facu1234`). It also inserts five sample Java marksheet rows so results appear immediately in a fresh demo setup. Never run this destructive demo script against an existing/production database.
- **Existing Database Additive Migration (`database/migrations/V2__audit_remediation_migration.sql`):**
  Non-destructive, additive migration script (contains **no** `DROP TABLE` statements). Back up your database before running:
  ```bash
  mysqldump -u root -p p04 > p04_backup.sql
  mysql -u root -p p04 < database/migrations/V2__audit_remediation_migration.sql
  ```

## 6. External Environment Configuration (Secrets Outside Source Control)

| Variable | Description | Default |
| :--- | :--- | :--- |
| `ORS_DB_DRIVER` | JDBC Driver class | `com.mysql.cj.jdbc.Driver` |
| `ORS_DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/p04` |
| `ORS_DB_USER` | Database Username | `root` |
| `ORS_DB_PASSWORD` | Database Password | *(empty)* |
| `ORS_PASSWORD_PEPPER` | Optional secret prefix used in PBKDF2 input; keep stable for verification | *(empty)* |
| `ORS_SMTP_USER` / `ORS_SMTP_PASSWORD` | SMTP credentials for email notifications | *(empty)* |
| `ORS_SMTP_HOST` / `ORS_SMTP_PORT` | Gmail SMTP server settings; default host/port | `smtp.gmail.com` / `587` |

### 7. Troubleshooting empty results

A fresh import of `database/demo_ors.sql` now includes five sample Java marksheet records for the included demo students. The optional `database/demo_seed_data.sql` is retained for databases created with an earlier version of the demo schema. For a student result to appear, the `ST_MARKSHEET` row must match the authenticated student ID and course, and the same student/course must have `ST_ENROLLMENT.STATUS = 'ACTIVE'`. If results remain empty, follow `RESULTS_TROUBLESHOOTING.md` and verify the database name, active enrollment, marks rows and server log before changing more code.

## 8. Security remediation package

See [`FIXES_APPLIED_2026-10-15.md`](FIXES_APPLIED_2026-10-15.md) for the source-level fixes applied to this archive, validation limits, and Windows build/deployment steps. In particular, run `mvn clean test package` before deploying; Maven/Tomcat/MySQL runtime validation was not available in the packaging environment.
