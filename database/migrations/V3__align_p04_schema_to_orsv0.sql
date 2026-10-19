-- ORSV0 targeted schema alignment for the currently reported p04 schema.
-- MySQL 5.7+ / 8.x. Review the accompanying guide BEFORE execution.
--
-- SAFETY PROPERTIES:
--   * Requires the selected schema to be named p04.
--   * Never drops application tables or rows.
--   * Renames legacy ST_MARKSHEET (PHYSICS/CHEMISTRY/MATHS columns) to
--     ST_MARKSHEET_LEGACY_20261015, preserving all 12 legacy rows.
--   * Creates a clean normalized ST_MARKSHEET table only after the legacy table
--     has been archived. Legacy marks are NOT auto-mapped to Java/String marks.
--   * Adds compatibility metadata columns expected by current Java models and
--     backfills names from authoritative related tables.
--   * Leaves existing enrollment ROLL_NO values NULL because the supplied data
--     has multiple roll numbers for the same STUDENT_ID and cannot be mapped safely.

DELIMITER $$
DROP PROCEDURE IF EXISTS orsv0_v3_align_p04_schema$$
CREATE PROCEDURE orsv0_v3_align_p04_schema()
BEGIN
  IF COALESCE(DATABASE(), '') <> 'p04' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Select database p04 before running this migration. No application tables were changed.';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_course')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_subject')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_student')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_enrollment')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty_allocation')
     OR NOT EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_marksheet') THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'One or more required ORS tables are missing. No schema changes were made.';
  END IF;

  -- Preserve the legacy table intact; never infer Physics/Chemistry/Maths as Java/String marks.
  IF EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_marksheet'
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_marksheet' AND COLUMN_NAME='MARKS'
  ) THEN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_marksheet_legacy_20261015'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Legacy archive ST_MARKSHEET_LEGACY_20261015 already exists while ST_MARKSHEET is still legacy. Stop and inspect tables before rerunning.';
    ELSE
      RENAME TABLE st_marksheet TO st_marksheet_legacy_20261015;
    END IF;
  END IF;

  -- ST_STUDENT compatibility fields expected by StudentModel.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_student' AND COLUMN_NAME='COURSE_ID') THEN
    ALTER TABLE st_student ADD COLUMN COURSE_ID BIGINT NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_student' AND COLUMN_NAME='COURSE_NAME') THEN
    ALTER TABLE st_student ADD COLUMN COURSE_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;

  -- ST_FACULTY compatibility fields expected by FacultyModel.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty' AND COLUMN_NAME='COURSE_ID') THEN
    ALTER TABLE st_faculty ADD COLUMN COURSE_ID BIGINT NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty' AND COLUMN_NAME='COURSE_NAME') THEN
    ALTER TABLE st_faculty ADD COLUMN COURSE_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty' AND COLUMN_NAME='SUBJECT_ID') THEN
    ALTER TABLE st_faculty ADD COLUMN SUBJECT_ID BIGINT NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty' AND COLUMN_NAME='SUBJECT_NAME') THEN
    ALTER TABLE st_faculty ADD COLUMN SUBJECT_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;

  -- ST_ENROLLMENT compatibility fields expected by EnrollmentModel.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_enrollment' AND COLUMN_NAME='STUDENT_NAME') THEN
    ALTER TABLE st_enrollment ADD COLUMN STUDENT_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_enrollment' AND COLUMN_NAME='ROLL_NO') THEN
    ALTER TABLE st_enrollment ADD COLUMN ROLL_NO VARCHAR(100) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_enrollment' AND COLUMN_NAME='COURSE_NAME') THEN
    ALTER TABLE st_enrollment ADD COLUMN COURSE_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_enrollment' AND COLUMN_NAME='SEMESTER') THEN
    ALTER TABLE st_enrollment ADD COLUMN SEMESTER VARCHAR(50) NULL DEFAULT 'Sem 1';
  END IF;

  -- Current legacy schema requires ACADEMIC_YEAR and TERM but has no defaults.
  -- The Java EnrollmentModel omits these columns on insert, so define safe defaults
  -- that match the existing p04 records; existing non-NULL values are preserved.
  ALTER TABLE st_enrollment
    MODIFY COLUMN ACADEMIC_YEAR VARCHAR(20) NOT NULL DEFAULT '2026-2027',
    MODIFY COLUMN TERM VARCHAR(40) NOT NULL DEFAULT 'Semester 1';

  -- ST_FACULTY_ALLOCATION compatibility fields expected by FacultyAllocationModel.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty_allocation' AND COLUMN_NAME='FACULTY_NAME') THEN
    ALTER TABLE st_faculty_allocation ADD COLUMN FACULTY_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty_allocation' AND COLUMN_NAME='FACULTY_EMAIL') THEN
    ALTER TABLE st_faculty_allocation ADD COLUMN FACULTY_EMAIL VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty_allocation' AND COLUMN_NAME='COURSE_NAME') THEN
    ALTER TABLE st_faculty_allocation ADD COLUMN COURSE_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_faculty_allocation' AND COLUMN_NAME='SUBJECT_NAME') THEN
    ALTER TABLE st_faculty_allocation ADD COLUMN SUBJECT_NAME VARCHAR(255) NULL DEFAULT NULL;
  END IF;
END$$
DELIMITER ;

CALL orsv0_v3_align_p04_schema();
DROP PROCEDURE IF EXISTS orsv0_v3_align_p04_schema;

-- Create the canonical per-student, per-course, per-subject marksheet structure.
-- Existing rows are not fabricated/backfilled because the legacy subject fields do not map
-- unambiguously to the current Java course / String subject.
CREATE TABLE IF NOT EXISTS st_marksheet (
  ID BIGINT NOT NULL,
  ROLL_NO VARCHAR(100) NOT NULL,
  STUDENT_ID BIGINT NOT NULL,
  NAME VARCHAR(255) NOT NULL,
  COURSE_ID BIGINT NOT NULL,
  COURSE_NAME VARCHAR(255) NOT NULL,
  SUBJECT_ID BIGINT NOT NULL,
  SUBJECT_NAME VARCHAR(255) NOT NULL,
  MARKS INT NOT NULL,
  CREATED_BY VARCHAR(255) DEFAULT NULL,
  MODIFIED_BY VARCHAR(255) DEFAULT NULL,
  CREATED_DATETIME DATETIME DEFAULT NULL,
  MODIFIED_DATETIME DATETIME DEFAULT NULL,
  PRIMARY KEY (ID),
  UNIQUE KEY uk_student_course_subject (STUDENT_ID, COURSE_ID, SUBJECT_ID),
  UNIQUE KEY uk_roll_course_subject (ROLL_NO, COURSE_ID, SUBJECT_ID),
  KEY idx_marksheet_course_subject_marks (COURSE_ID, SUBJECT_ID, MARKS)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Backfill display fields from normalized relationships. Do not invent roll numbers.
UPDATE st_enrollment e
LEFT JOIN st_student s ON s.ID = e.STUDENT_ID
LEFT JOIN st_course c ON c.ID = e.COURSE_ID
SET e.STUDENT_NAME = NULLIF(TRIM(CONCAT(COALESCE(s.FIRST_NAME,''), ' ', COALESCE(s.LAST_NAME,''))), ''),
    e.COURSE_NAME = c.NAME,
    e.SEMESTER = CASE
      WHEN e.TERM IS NULL OR TRIM(e.TERM) = '' THEN COALESCE(NULLIF(e.SEMESTER,''), 'Sem 1')
      WHEN LOWER(TRIM(e.TERM)) LIKE 'semester %' THEN CONCAT('Sem ', TRIM(SUBSTRING(TRIM(e.TERM), 10)))
      ELSE TRIM(e.TERM)
    END
WHERE e.STUDENT_NAME IS NULL OR e.COURSE_NAME IS NULL OR e.SEMESTER IS NULL OR TRIM(e.SEMESTER)='';

UPDATE st_faculty_allocation a
LEFT JOIN st_faculty f ON f.ID = a.FACULTY_ID
LEFT JOIN st_course c ON c.ID = a.COURSE_ID
LEFT JOIN st_subject s ON s.ID = a.SUBJECT_ID
SET a.FACULTY_NAME = NULLIF(TRIM(CONCAT(COALESCE(f.FIRST_NAME,''), ' ', COALESCE(f.LAST_NAME,''))), ''),
    a.FACULTY_EMAIL = f.EMAIL,
    a.COURSE_NAME = c.NAME,
    a.SUBJECT_NAME = s.NAME
WHERE a.FACULTY_NAME IS NULL OR a.FACULTY_EMAIL IS NULL OR a.COURSE_NAME IS NULL OR a.SUBJECT_NAME IS NULL;

-- Copy a course onto legacy student/faculty profile fields only when there is one unambiguous
-- active enrollment / course-subject allocation. ST_ENROLLMENT and ST_FACULTY_ALLOCATION stay authoritative.
UPDATE st_student st
JOIN (
  SELECT STUDENT_ID, MIN(COURSE_ID) AS COURSE_ID
  FROM st_enrollment
  WHERE UPPER(STATUS)='ACTIVE'
  GROUP BY STUDENT_ID
  HAVING COUNT(DISTINCT COURSE_ID)=1
) e ON e.STUDENT_ID=st.ID
JOIN st_course c ON c.ID=e.COURSE_ID
SET st.COURSE_ID=e.COURSE_ID, st.COURSE_NAME=c.NAME
WHERE st.COURSE_ID IS NULL OR st.COURSE_NAME IS NULL;

UPDATE st_faculty f
JOIN (
  SELECT FACULTY_ID, MIN(COURSE_ID) AS COURSE_ID, MIN(SUBJECT_ID) AS SUBJECT_ID
  FROM st_faculty_allocation
  WHERE SUBJECT_ID IS NOT NULL
  GROUP BY FACULTY_ID
  HAVING COUNT(DISTINCT COURSE_ID)=1 AND COUNT(DISTINCT SUBJECT_ID)=1
) a ON a.FACULTY_ID=f.ID
JOIN st_course c ON c.ID=a.COURSE_ID
JOIN st_subject s ON s.ID=a.SUBJECT_ID
SET f.COURSE_ID=a.COURSE_ID, f.COURSE_NAME=c.NAME, f.SUBJECT_ID=a.SUBJECT_ID, f.SUBJECT_NAME=s.NAME
WHERE f.COURSE_ID IS NULL OR f.COURSE_NAME IS NULL OR f.SUBJECT_ID IS NULL OR f.SUBJECT_NAME IS NULL;

-- Report preserved legacy row count where the archive exists; this is read-only.
SET @orsv0_legacy_count_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='st_marksheet_legacy_20261015'),
  'SELECT ''st_marksheet_legacy_20261015'' AS preserved_table, COUNT(*) AS preserved_legacy_rows FROM st_marksheet_legacy_20261015',
  'SELECT ''st_marksheet_legacy_20261015'' AS preserved_table, NULL AS preserved_legacy_rows'
);
PREPARE orsv0_legacy_count_stmt FROM @orsv0_legacy_count_sql;
EXECUTE orsv0_legacy_count_stmt;
DEALLOCATE PREPARE orsv0_legacy_count_stmt;

-- Verify canonical columns now exist.
SHOW COLUMNS FROM st_marksheet;
SHOW COLUMNS FROM st_enrollment;
SHOW COLUMNS FROM st_faculty_allocation;
