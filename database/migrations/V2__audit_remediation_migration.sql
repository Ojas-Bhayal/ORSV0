-- =============================================================================
-- ORSV0 Canonical Additive Migration Script: V2__audit_remediation_migration.sql
-- Version: 2.1.0 (ORS-2.1.0-RELEASE-20261014)
-- Supported Upgrade Paths:
--   1. Fresh/current normalized schema (marks are represented by one MARKS value per subject row).
--   2. Existing normalized schema where rows can be linked to a unique student.
--   3. Existing V2 schema (idempotent re-execution).
--
-- SAFETY LIMIT: A legacy st_marksheet with PHYSICS/CHEMISTRY/MATHS columns but no
-- MARKS column is not automatically converted. Its subject columns do not map
-- unambiguously to this application's course/subject model. Preflight stops before
-- schema changes rather than averaging three scores into one unrelated subject.
-- Such legacy data needs a separately reviewed subject-by-subject migration.
--
-- IMPORTANT: Contains NO destructive DROP TABLE statements (temporary procedures are dropped).
-- Always back up your database before running:
--   mysqldump -u root -p p04 > p04_backup.sql
--   mysql -u root -p p04 < database/migrations/V2__audit_remediation_migration.sql
-- =============================================================================

-- Preflight before schema changes: do not silently collapse legacy subject grades.
DROP PROCEDURE IF EXISTS `ors_v2_preflight_legacy_marks`;
DELIMITER $$
CREATE PROCEDURE `ors_v2_preflight_legacy_marks`()
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'MARKS'
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V2 migration stopped safely: existing st_marksheet is not in the normalized per-subject MARKS format. No application tables were changed. Back up the database and map the legacy subject columns to the current course/subject model explicitly.';
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'MARKS'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'STUDENT_ID'
  ) THEN
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_student'
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V2 migration stopped: st_marksheet needs STUDENT_ID backfill but st_student is missing.';
    ELSEIF EXISTS (
      SELECT 1 FROM `st_marksheet` m
      WHERE (SELECT COUNT(*) FROM `st_student` s
             WHERE LOWER(TRIM(CONCAT(s.FIRST_NAME, ' ', s.LAST_NAME))) = LOWER(TRIM(m.NAME))) <> 1
    ) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'V2 migration stopped: some marksheet names do not match exactly one student. Resolve student IDs before applying schema changes.';
    END IF;
  END IF;
END$$
DELIMITER ;
CALL `ors_v2_preflight_legacy_marks`();
DROP PROCEDURE IF EXISTS `ors_v2_preflight_legacy_marks`;

-- 1. Create ST_ENROLLMENT for many-to-many student-course enrollments
CREATE TABLE IF NOT EXISTS `st_enrollment` (
  `ID` bigint NOT NULL,
  `STUDENT_ID` bigint NOT NULL,
  `STUDENT_NAME` varchar(255) NOT NULL,
  `ROLL_NO` varchar(100) NOT NULL,
  `COURSE_ID` bigint NOT NULL,
  `COURSE_NAME` varchar(255) NOT NULL,
  `SEMESTER` varchar(50) DEFAULT 'Sem 1',
  `STATUS` varchar(50) DEFAULT 'ACTIVE',
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_student_course` (`STUDENT_ID`,`COURSE_ID`),
  KEY `idx_enroll_course_status` (`COURSE_ID`,`STATUS`),
  KEY `idx_enroll_rollno` (`ROLL_NO`),
  CONSTRAINT `fk_enroll_student` FOREIGN KEY (`STUDENT_ID`) REFERENCES `st_student` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_enroll_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create ST_FACULTY_ALLOCATION for many-to-many faculty course/subject allocations
CREATE TABLE IF NOT EXISTS `st_faculty_allocation` (
  `ID` bigint NOT NULL,
  `FACULTY_ID` bigint NOT NULL,
  `FACULTY_NAME` varchar(255) NOT NULL,
  `FACULTY_EMAIL` varchar(255) NOT NULL,
  `COURSE_ID` bigint NOT NULL,
  `COURSE_NAME` varchar(255) NOT NULL,
  `SUBJECT_ID` bigint NOT NULL,
  `SUBJECT_NAME` varchar(255) NOT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_faculty_course_subject` (`FACULTY_ID`,`COURSE_ID`,`SUBJECT_ID`),
  KEY `idx_alloc_email_course_subject` (`FACULTY_EMAIL`,`COURSE_ID`,`SUBJECT_ID`),
  CONSTRAINT `fk_alloc_faculty` FOREIGN KEY (`FACULTY_ID`) REFERENCES `st_faculty` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_alloc_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_alloc_subject` FOREIGN KEY (`SUBJECT_ID`) REFERENCES `st_subject` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Create ST_MARKSHEET if not present
CREATE TABLE IF NOT EXISTS `st_marksheet` (
  `ID` bigint NOT NULL,
  `ROLL_NO` varchar(100) NOT NULL,
  `STUDENT_ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `COURSE_ID` bigint NOT NULL DEFAULT 1,
  `COURSE_NAME` varchar(255) NOT NULL DEFAULT 'Java',
  `SUBJECT_ID` bigint NOT NULL DEFAULT 1,
  `SUBJECT_NAME` varchar(255) NOT NULL DEFAULT 'String',
  `MARKS` int NOT NULL DEFAULT 0,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_student_course_subject` (`STUDENT_ID`,`COURSE_ID`,`SUBJECT_ID`),
  UNIQUE KEY `uk_roll_course_subject` (`ROLL_NO`,`COURSE_ID`,`SUBJECT_ID`),
  KEY `idx_marksheet_course_subject_marks` (`COURSE_ID`,`SUBJECT_ID`,`MARKS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Create ST_PASSWORD_RESET_TOKEN for single-use expiring password reset tokens
CREATE TABLE IF NOT EXISTS `st_password_reset_token` (
  `ID` bigint NOT NULL,
  `USER_ID` bigint NOT NULL,
  `LOGIN` varchar(255) NOT NULL,
  `TOKEN_HASH` varchar(128) NOT NULL,
  `EXPIRES_AT` datetime NOT NULL,
  `USED` tinyint NOT NULL DEFAULT 0,
  `CREATED_DATETIME` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_reset_token_hash` (`TOKEN_HASH`),
  KEY `idx_reset_user_used` (`USER_ID`,`USED`),
  CONSTRAINT `fk_reset_token_user` FOREIGN KEY (`USER_ID`) REFERENCES `st_user` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Idempotent ALTER TABLE retrofit for existing V1 databases where st_student,
--    st_faculty, st_subject, or st_marksheet pre-existed with older columns
DROP PROCEDURE IF EXISTS `ors_v2_retrofit_existing_tables`;
DELIMITER $$
CREATE PROCEDURE `ors_v2_retrofit_existing_tables`()
BEGIN
  -- Retrofit st_student.COURSE_ID and COURSE_NAME if missing
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_student' AND COLUMN_NAME = 'COURSE_ID') THEN
    ALTER TABLE `st_student` ADD COLUMN `COURSE_ID` bigint NOT NULL DEFAULT 1;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_student' AND COLUMN_NAME = 'COURSE_NAME') THEN
    ALTER TABLE `st_student` ADD COLUMN `COURSE_NAME` varchar(255) DEFAULT 'Java';
  END IF;

  -- Retrofit STUDENT_ID if a normalized legacy marks table did not have it.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'STUDENT_ID') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `STUDENT_ID` bigint NULL AFTER `ROLL_NO`;
  END IF;

  -- Retrofit normalized marks columns if missing. Legacy PHYSICS/CHEMISTRY/MATHS
  -- tables are rejected by preflight above to avoid destructive score averaging.
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'COURSE_ID') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `COURSE_ID` bigint NOT NULL DEFAULT 1;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'COURSE_NAME') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `COURSE_NAME` varchar(255) NOT NULL DEFAULT 'Java';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'SUBJECT_ID') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `SUBJECT_ID` bigint NOT NULL DEFAULT 1;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'SUBJECT_NAME') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `SUBJECT_NAME` varchar(255) NOT NULL DEFAULT 'String';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND COLUMN_NAME = 'MARKS') THEN
    ALTER TABLE `st_marksheet` ADD COLUMN `MARKS` int NOT NULL DEFAULT 0;
  END IF;

  -- Recover Student ID only when the full name maps to exactly one student.
  -- Ambiguous/unmatched rows cause a clear failure rather than an arbitrary assignment.
  UPDATE `st_marksheet` m
  INNER JOIN (
    SELECT LOWER(TRIM(CONCAT(FIRST_NAME, ' ', LAST_NAME))) AS FULL_NAME,
           MIN(ID) AS STUDENT_ID
    FROM `st_student`
    GROUP BY LOWER(TRIM(CONCAT(FIRST_NAME, ' ', LAST_NAME)))
    HAVING COUNT(*) = 1
  ) s ON LOWER(TRIM(m.NAME)) = s.FULL_NAME
  SET m.STUDENT_ID = s.STUDENT_ID
  WHERE m.STUDENT_ID IS NULL OR m.STUDENT_ID = 0;

  IF EXISTS (SELECT 1 FROM `st_marksheet` WHERE `STUDENT_ID` IS NULL OR `STUDENT_ID` = 0) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'V2 migration stopped: one or more marksheet rows could not be linked to exactly one student by name. Map those rows explicitly before retrying.';
  END IF;

  ALTER TABLE `st_marksheet` MODIFY COLUMN `STUDENT_ID` bigint NOT NULL;

  -- Ensure composite uniqueness (COURSE_ID, NAME) on st_subject
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_subject' AND INDEX_NAME = 'uk_course_subject_name') THEN
    ALTER TABLE `st_subject` ADD UNIQUE KEY `uk_course_subject_name` (`COURSE_ID`, `NAME`);
  END IF;

  -- Ensure composite uniqueness (STUDENT_ID, COURSE_ID, SUBJECT_ID) on st_marksheet
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND INDEX_NAME = 'uk_student_course_subject') THEN
    ALTER TABLE `st_marksheet` ADD UNIQUE KEY `uk_student_course_subject` (`STUDENT_ID`, `COURSE_ID`, `SUBJECT_ID`);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'st_marksheet' AND INDEX_NAME = 'uk_roll_course_subject') THEN
    ALTER TABLE `st_marksheet` ADD UNIQUE KEY `uk_roll_course_subject` (`ROLL_NO`, `COURSE_ID`, `SUBJECT_ID`);
  END IF;
END$$
DELIMITER ;

CALL `ors_v2_retrofit_existing_tables`();
DROP PROCEDURE IF EXISTS `ors_v2_retrofit_existing_tables`;

-- 6. Backfill active enrollments from existing st_student rows where COURSE_ID is present
INSERT IGNORE INTO `st_enrollment` (`ID`, `STUDENT_ID`, `STUDENT_NAME`, `ROLL_NO`, `COURSE_ID`, `COURSE_NAME`, `SEMESTER`, `STATUS`, `CREATED_BY`, `MODIFIED_BY`, `CREATED_DATETIME`, `MODIFIED_DATETIME`)
SELECT s.ID, s.ID, CONCAT(s.FIRST_NAME, ' ', s.LAST_NAME),
       COALESCE(m.ROLL_NO, CAST(s.ID AS CHAR)), s.COURSE_ID, COALESCE(c.NAME, s.COURSE_NAME),
       'Sem 1', 'ACTIVE', s.CREATED_BY, s.MODIFIED_BY, NOW(), NOW()
FROM `st_student` s
INNER JOIN `st_course` c ON s.COURSE_ID = c.ID
LEFT JOIN (
  SELECT STUDENT_ID, COURSE_ID, MIN(ROLL_NO) AS ROLL_NO
  FROM `st_marksheet`
  GROUP BY STUDENT_ID, COURSE_ID
  HAVING COUNT(DISTINCT ROLL_NO) = 1
) m ON m.STUDENT_ID = s.ID AND m.COURSE_ID = s.COURSE_ID
WHERE s.COURSE_ID IS NOT NULL AND s.COURSE_ID > 0;

-- 7. Backfill allocations from existing st_faculty rows where COURSE_ID and SUBJECT_ID are present
INSERT IGNORE INTO `st_faculty_allocation` (`ID`, `FACULTY_ID`, `FACULTY_NAME`, `FACULTY_EMAIL`, `COURSE_ID`, `COURSE_NAME`, `SUBJECT_ID`, `SUBJECT_NAME`, `CREATED_BY`, `MODIFIED_BY`, `CREATED_DATETIME`, `MODIFIED_DATETIME`)
SELECT f.ID, f.ID, CONCAT(f.FIRST_NAME, ' ', f.LAST_NAME), f.EMAIL, f.COURSE_ID, COALESCE(c.NAME, f.COURSE_NAME), f.SUBJECT_ID, COALESCE(sub.NAME, f.SUBJECT_NAME), f.CREATED_BY, f.MODIFIED_BY, NOW(), NOW()
FROM `st_faculty` f
INNER JOIN `st_course` c ON f.COURSE_ID = c.ID
INNER JOIN `st_subject` sub ON f.SUBJECT_ID = sub.ID
WHERE f.COURSE_ID > 0 AND f.SUBJECT_ID > 0;
