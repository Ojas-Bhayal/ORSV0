-- =============================================================================
-- ORSV0 FRESH DATABASE SETUP: p04
-- Generated from the project's canonical database/demo_ors.sql schema.
--
-- DESTRUCTIVE WARNING: this script permanently drops the existing database named
-- p04 (all tables and data in it) and recreates it from scratch. Export a backup
-- first if there is anything you may need later. Do NOT execute against production.
--
-- Creates the normalized Java/course/subject marksheet schema that this source
-- expects, plus demo seed records. It intentionally seeds the four application
-- roles used by FrontController: Admin, Student, ProgramChair, Faculty.
--
-- Demo logins (change passwords after first login):
--   an@gmail.com     / Admin123   (Admin)
--   manish@gmail.com / Stud1234   (Student)
--   chair@gmail.com  / Chair123   (Program Chair)
--   ram@gmail.com    / Facu1234   (Faculty)
--
-- After this script finishes, configure ORS_DB_USER / ORS_DB_PASSWORD if your
-- local MySQL root account uses a password. SMTP is configured separately.
-- =============================================================================

DROP DATABASE IF EXISTS `p04`;
CREATE DATABASE `p04` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `p04`;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `st_role` (
  `ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `MODIFIED_DATETIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_role_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_role` VALUES
(1,'Admin','Administrator Role','root@sunilos.com','root@sunilos.com','2026-05-15 11:43:36','2026-05-15 11:43:36'),
(2,'Student','Student Role','root@sunilos.com','root@sunilos.com','2026-05-15 11:49:09','2026-05-15 11:49:09'),
(3,'ProgramChair','Program Chair and HOD Role','root@sunilos.com','root@sunilos.com','2026-05-15 11:49:30','2026-05-15 11:49:30'),
(4,'Faculty','Faculty Role','root@sunilos.com','root@sunilos.com','2026-05-15 11:49:48','2026-05-15 11:49:48');

CREATE TABLE `st_college` (
  `ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `ADDRESS` varchar(255) DEFAULT NULL,
  `STATE` varchar(255) DEFAULT NULL,
  `CITY` varchar(255) DEFAULT NULL,
  `PHONE_NO` varchar(20) DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_college_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_college` VALUES
(1,'Truba','Indore Bypass Road','MadhyaPradesh','Indore','9826012345','root@sunilos.com','root@sunilos.com','2026-05-15 16:19:14','2026-05-15 16:19:14'),
(2,'SVIM','Gumasta Nagar','MadhyaPradesh','Indore','9826054321','root@sunilos.com','root@sunilos.com','2026-05-15 16:19:14','2026-05-15 16:19:14'),
(3,'JIY','Rau Main Road','MadhyaPradesh','Khargone','8871012345','root@sunilos.com','root@sunilos.com','2026-05-15 16:19:14','2026-05-15 16:19:14'),
(4,'JIU','Indore Road Rau','Gujarat','Bhopal','9165254357','root@sunilos.com','root@sunilos.com','2026-05-15 16:19:14','2026-05-15 16:19:14'),
(5,'MIT','Ujjain Road','MadhyaPradesh','Indore','7896321450','root@sunilos.com','root@sunilos.com','2026-05-15 16:19:14','2026-05-15 16:19:14');

CREATE TABLE `st_course` (
  `ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(500) DEFAULT NULL,
  `DURATION` varchar(100) DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_course_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_course` VALUES
(1,'Java','Java object oriented programming course','02-04','root@sunilos.com','root@sunilos.com','2026-05-30 11:35:37','2026-05-30 11:35:37'),
(2,'Python','Python programming and data structures course','02-04','root@sunilos.com','root@sunilos.com','2026-05-30 11:35:37','2026-05-30 11:35:37');

-- Phase 4 Task 9: Composite uniqueness (COURSE_ID, NAME) so subject names can repeat across courses
CREATE TABLE `st_subject` (
  `ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(500) DEFAULT NULL,
  `COURSE_ID` bigint NOT NULL,
  `COURSE_NAME` varchar(255) DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_course_subject_name` (`COURSE_ID`,`NAME`),
  KEY `idx_subject_course` (`COURSE_ID`),
  CONSTRAINT `fk_subject_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_subject` VALUES
(1,'String','String is an array of characters in Java',1,'Java','root@sunilos.com','root@sunilos.com','2026-05-30 11:36:13','2026-05-30 11:36:13'),
(2,'Collections','Java Collections Framework',1,'Java','root@sunilos.com','root@sunilos.com','2026-05-30 11:36:13','2026-05-30 11:36:13'),
(3,'Algorithms','Python Algorithms and Lists',2,'Python','root@sunilos.com','root@sunilos.com','2026-05-30 11:36:13','2026-05-30 11:36:13');

CREATE TABLE `st_faculty` (
  `ID` bigint NOT NULL,
  `COLLEGE_ID` bigint NOT NULL,
  `COLLEGE_NAME` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) NOT NULL,
  `LAST_NAME` varchar(255) NOT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `MOBILE_NO` varchar(20) NOT NULL,
  `ADDRESS` varchar(255) DEFAULT NULL,
  `GENDER` varchar(50) DEFAULT NULL,
  `DATE_OF_BIRTH` datetime DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  `COURSE_ID` bigint DEFAULT NULL,
  `COURSE_NAME` varchar(255) DEFAULT NULL,
  `SUBJECT_ID` bigint DEFAULT NULL,
  `SUBJECT_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_faculty_email` (`EMAIL`),
  KEY `idx_faculty_college` (`COLLEGE_ID`),
  KEY `idx_faculty_course_subject` (`COURSE_ID`,`SUBJECT_ID`),
  CONSTRAINT `fk_faculty_college` FOREIGN KEY (`COLLEGE_ID`) REFERENCES `st_college` (`ID`),
  CONSTRAINT `fk_faculty_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`),
  CONSTRAINT `fk_faculty_subject` FOREIGN KEY (`SUBJECT_ID`) REFERENCES `st_subject` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_faculty` VALUES
(1,4,'JIU','Ram','Sharma','ram@gmail.com','9856569858','Ganga Bag Colony','Male','1992-12-12 00:00:00','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23',1,'Java',1,'String'),
(2,4,'JIU','Ojas','Bhayal','chair@gmail.com','9165254357','University Campus','Male','1988-08-15 00:00:00','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23',1,'Java',1,'String');

CREATE TABLE `st_faculty_allocation` (
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

INSERT INTO `st_faculty_allocation` VALUES
(1,1,'Ram Sharma','ram@gmail.com',1,'Java',1,'String','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23'),
(2,1,'Ram Sharma','ram@gmail.com',1,'Java',2,'Collections','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23'),
(3,2,'Ojas Bhayal','chair@gmail.com',1,'Java',1,'String','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23'),
(4,2,'Ojas Bhayal','chair@gmail.com',2,'Python',3,'Algorithms','root@sunilos.com','root@sunilos.com','2026-05-14 12:22:23','2026-05-14 12:22:23');

CREATE TABLE `st_student` (
  `ID` bigint NOT NULL,
  `COLLEGE_ID` bigint NOT NULL,
  `COLLEGE_NAME` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) NOT NULL,
  `LAST_NAME` varchar(255) NOT NULL,
  `DATE_OF_BIRTH` datetime DEFAULT NULL,
  `MOBILE_NO` varchar(20) NOT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  `COURSE_ID` bigint NOT NULL,
  `COURSE_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_student_email` (`EMAIL`),
  KEY `idx_student_college` (`COLLEGE_ID`),
  KEY `idx_student_course` (`COURSE_ID`),
  CONSTRAINT `fk_student_college` FOREIGN KEY (`COLLEGE_ID`) REFERENCES `st_college` (`ID`),
  CONSTRAINT `fk_student_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_student` VALUES
(1,4,'JIU','Rahul','Sahu','2004-07-17 00:00:00','9826011111','rahul@gmail.com','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13',1,'Java'),
(2,2,'SVIM','Manish','Sharma','2004-07-18 00:00:00','7896321452','manish@gmail.com','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13',1,'Java'),
(3,4,'JIU','Vipin','Chandore','2001-12-05 00:00:00','9165254357','vipin@gmail.com','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13',1,'Java'),
(4,5,'MIT','Deepak','Kumar','2001-10-05 00:00:00','8165254357','deepak@gmail.com','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13',1,'Java'),
(5,5,'MIT','Sivam','Kumar','2005-06-02 00:00:00','7894563210','sivam@gmail.com','root@sunilos.com','root@sunilos.com','2026-06-01 15:52:26','2026-06-01 15:52:26',1,'Java');

CREATE TABLE `st_enrollment` (
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

INSERT INTO `st_enrollment` VALUES
(1,2,'Manish Sharma','1',1,'Java','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13'),
(2,1,'Rahul Sahu','2',1,'Java','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13'),
(3,3,'Vipin Chandore','3',1,'Java','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13'),
(4,4,'Deepak Kumar','4',1,'Java','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13'),
(5,5,'Sivam Kumar','5',1,'Java','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13'),
(6,2,'Manish Sharma','1',2,'Python','Sem 1','ACTIVE','root@sunilos.com','root@sunilos.com','2026-05-16 16:54:13','2026-05-16 16:54:13');

-- Phase 4 Task 8: Uniqueness constraints on (STUDENT_ID, COURSE_ID, SUBJECT_ID) and (ROLL_NO, COURSE_ID, SUBJECT_ID)
CREATE TABLE `st_marksheet` (
  `ID` bigint NOT NULL,
  `ROLL_NO` varchar(100) NOT NULL,
  `STUDENT_ID` bigint NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `COURSE_ID` bigint NOT NULL,
  `COURSE_NAME` varchar(255) NOT NULL,
  `SUBJECT_ID` bigint NOT NULL,
  `SUBJECT_NAME` varchar(255) NOT NULL,
  `MARKS` int NOT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_student_course_subject` (`STUDENT_ID`,`COURSE_ID`,`SUBJECT_ID`),
  UNIQUE KEY `uk_roll_course_subject` (`ROLL_NO`,`COURSE_ID`,`SUBJECT_ID`),
  KEY `idx_marksheet_course_subject_marks` (`COURSE_ID`,`SUBJECT_ID`,`MARKS`),
  CONSTRAINT `fk_marksheet_student` FOREIGN KEY (`STUDENT_ID`) REFERENCES `st_student` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_marksheet_course` FOREIGN KEY (`COURSE_ID`) REFERENCES `st_course` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `fk_marksheet_subject` FOREIGN KEY (`SUBJECT_ID`) REFERENCES `st_subject` (`ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Demo-only sample marks so a fresh local setup displays result cards immediately.
-- Do not import demo data into a production database containing real results.
INSERT INTO `st_marksheet` (`ID`,`ROLL_NO`,`STUDENT_ID`,`NAME`,`COURSE_ID`,`COURSE_NAME`,`SUBJECT_ID`,`SUBJECT_NAME`,`MARKS`,`CREATED_BY`,`MODIFIED_BY`,`CREATED_DATETIME`,`MODIFIED_DATETIME`) VALUES
(1,'1',2,'Manish Sharma',1,'Java',1,'String',95,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(2,'2',1,'Rahul Sahu',1,'Java',1,'String',88,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(3,'3',3,'Vipin Chandore',1,'Java',1,'String',89,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(4,'4',4,'Deepak Kumar',1,'Java',1,'String',78,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00'),
(5,'5',5,'Sivam Kumar',1,'Java',1,'String',84,'ram@gmail.com','ram@gmail.com','2026-05-30 11:40:00','2026-05-30 11:40:00');

CREATE TABLE `st_user` (
  `ID` bigint NOT NULL,
  `FIRST_NAME` varchar(255) NOT NULL,
  `LAST_NAME` varchar(255) NOT NULL,
  `LOGIN` varchar(255) NOT NULL,
  `PASSWORD` varchar(255) NOT NULL,
  `DOB` datetime DEFAULT NULL,
  `MOBILE_NO` varchar(20) NOT NULL,
  `ROLE_ID` bigint NOT NULL,
  `UNSUCCESSFUL_LOGIN` int DEFAULT 0,
  `GENDER` varchar(50) DEFAULT NULL,
  `LAST_LOGIN` datetime DEFAULT NULL,
  `USER_LOCK` varchar(50) DEFAULT 'Inactive',
  `REGISTERED_IP` varchar(100) DEFAULT NULL,
  `LAST_LOGIN_IP` varchar(100) DEFAULT NULL,
  `CREATED_BY` varchar(255) DEFAULT NULL,
  `MODIFIED_BY` varchar(255) DEFAULT NULL,
  `CREATED_DATETIME` datetime DEFAULT NULL,
  `MODIFIED_DATETIME` datetime DEFAULT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uk_user_login` (`LOGIN`),
  KEY `idx_user_role` (`ROLE_ID`),
  CONSTRAINT `fk_user_role` FOREIGN KEY (`ROLE_ID`) REFERENCES `st_role` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `st_user` VALUES
(1,'Anshul','Prajapati','an@gmail.com','PBKDF2WithHmacSHA256$65536$a1f04c92d83e71b50629ca84e3175df0$0280c1e6ab8c38cd4595e3c28683a630d4ddbffe0fd6b9aaa95c29755e35efd1','2000-05-13 00:00:00','9826012345',1,0,'M',NULL,'Inactive',NULL,NULL,'root@sunilos.com','an@gmail.com','2026-05-15 17:39:20','2026-06-01 12:23:38'),
(2,'Manish','Sharma','manish@gmail.com','PBKDF2WithHmacSHA256$65536$b2e15d03e94f82c6173adb95f4286ea1$e3dfd2c86a9a95d29e3c77f2881058bab92dbcb999b323c7258b486acd5ecb82','2004-07-18 00:00:00','7896321452',2,0,'M',NULL,'Inactive',NULL,NULL,'root@sunilos.com','root@sunilos.com','2026-05-15 17:39:20','2026-05-15 20:19:00'),
(3,'Ojas','Bhayal','chair@gmail.com','PBKDF2WithHmacSHA256$65536$c3d26e14fa5093d7284bec0605397fb2$1747d311e627d9ed70390cd621be90776825863be06f9e776b6c856ec75d6509','1988-08-15 00:00:00','9165254357',3,0,'M',NULL,'Inactive',NULL,NULL,'root@sunilos.com','root@sunilos.com','2026-05-15 17:39:20','2026-05-15 17:39:20'),
(4,'Ram','Sharma','ram@gmail.com','PBKDF2WithHmacSHA256$65536$d4c37f250b61a4e8395cfd17164a80c3$9d85147f358f76113ff99947ae47386e02bf157f28e8b5f06ccf64784f5736e3','1992-12-12 00:00:00','9856569858',4,0,'M',NULL,'Inactive',NULL,NULL,'root@sunilos.com','root@sunilos.com','2026-05-15 17:39:20','2026-05-15 17:39:20');

-- Phase 2 Task 6: Single-Use, Expiring, Hashed Password-Reset Tokens
CREATE TABLE `st_password_reset_token` (
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

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- POST-SETUP VALIDATION
-- Expected tables: 11. Demo seed includes 5 colleges, 2 courses, 3 subjects,
-- 2 faculty, 4 allocations, 5 students, 6 enrollments, 5 normalized marks,
-- 4 users, 4 roles, and 0 password-reset tokens.
-- =============================================================================
SHOW TABLES;

SELECT 'st_role' AS table_name, COUNT(*) AS total_rows FROM st_role
UNION ALL SELECT 'st_college', COUNT(*) FROM st_college
UNION ALL SELECT 'st_course', COUNT(*) FROM st_course
UNION ALL SELECT 'st_subject', COUNT(*) FROM st_subject
UNION ALL SELECT 'st_faculty', COUNT(*) FROM st_faculty
UNION ALL SELECT 'st_faculty_allocation', COUNT(*) FROM st_faculty_allocation
UNION ALL SELECT 'st_student', COUNT(*) FROM st_student
UNION ALL SELECT 'st_enrollment', COUNT(*) FROM st_enrollment
UNION ALL SELECT 'st_marksheet', COUNT(*) FROM st_marksheet
UNION ALL SELECT 'st_user', COUNT(*) FROM st_user
UNION ALL SELECT 'st_password_reset_token', COUNT(*) FROM st_password_reset_token
ORDER BY table_name;

SELECT ID, NAME FROM st_role ORDER BY ID;
SELECT ID, NAME FROM st_course ORDER BY ID;
SELECT ID, NAME, COURSE_ID FROM st_subject ORDER BY ID;
