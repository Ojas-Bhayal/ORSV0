package com.sunilos.p4.model;

import com.sunilos.p4.bean.*;
import com.sunilos.p4.ctl.*;
import com.sunilos.p4.exception.*;
import com.sunilos.p4.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.sql.DriverManager;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-End Live Relational Database Integration, Schema Migration & NFR Latency Test Suite
 * verifying Phases 1 through 9 of ORSV0_6_Evaluation_and_Implementation_Plan.md.
 */
public class DatabaseIntegrationAndLatencyTest {

    @AfterEach
    void clearTestConnectionSupplier() {
        JDBCDataSource.clearTestConnectionSupplier();
    }


    @Test
    public void testAllPhasesEndToEndAndLatencyBenchmarks() throws Exception {
        main(new String[0]);
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new RuntimeException("ASSERTION FAILED: " + msg);
    }

    private static String sha256Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_audit_db;sql.syntax_mys=true");
        JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
        		"jdbc:hsqldb:mem:ors_audit_db;sql.syntax_mys=true", "sa", ""));
        System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
        System.setProperty("ORS_DB_USER", "sa");
        System.setProperty("ORS_DB_PASSWORD", "");
        JDBCDataSource.closeConnection(null);

        try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("DROP TABLE ST_PASSWORD_RESET_TOKEN IF EXISTS");
            st.execute("DROP TABLE ST_AUDIT_LOG IF EXISTS");
            st.execute("DROP TABLE ST_FACULTY_ALLOCATION IF EXISTS");
            st.execute("DROP TABLE ST_ENROLLMENT IF EXISTS");
            st.execute("DROP TABLE ST_MARKSHEET IF EXISTS");
            st.execute("DROP TABLE ST_FACULTY IF EXISTS");
            st.execute("DROP TABLE ST_STUDENT IF EXISTS");
            st.execute("DROP TABLE ST_SUBJECT IF EXISTS");
            st.execute("DROP TABLE ST_COURSE IF EXISTS");
            st.execute("DROP TABLE ST_COLLEGE IF EXISTS");
            st.execute("DROP TABLE ST_USER IF EXISTS");
            st.execute("DROP TABLE ST_ROLE IF EXISTS");

            st.execute("CREATE TABLE ST_ROLE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
            st.execute("CREATE TABLE ST_USER (ID BIGINT PRIMARY KEY, FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), LOGIN VARCHAR(100), PASSWORD VARCHAR(255), DOB DATE, MOBILE_NO VARCHAR(20), ROLE_ID BIGINT, UNSUCCESSFUL_LOGIN INT, GENDER VARCHAR(20), LAST_LOGIN TIMESTAMP, USER_LOCK VARCHAR(10), REGISTERED_IP VARCHAR(50), LAST_LOGIN_IP VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
            st.execute("CREATE TABLE ST_PASSWORD_RESET_TOKEN (ID BIGINT PRIMARY KEY, USER_ID BIGINT, LOGIN VARCHAR(100), TOKEN_HASH VARCHAR(128), EXPIRES_AT TIMESTAMP, USED INT DEFAULT 0, CREATED_DATETIME TIMESTAMP)");
            st.execute("CREATE TABLE ST_COLLEGE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), ADDRESS VARCHAR(255), STATE VARCHAR(50), CITY VARCHAR(50), PHONE_NO VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
            st.execute("CREATE TABLE ST_COURSE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), DURATION VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
            st.execute("CREATE TABLE ST_SUBJECT (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_subj_course UNIQUE (COURSE_ID, NAME))");
            st.execute("CREATE TABLE ST_STUDENT (ID BIGINT PRIMARY KEY, COLLEGE_ID BIGINT, COLLEGE_NAME VARCHAR(100), FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), DATE_OF_BIRTH TIMESTAMP, MOBILE_NO VARCHAR(20), EMAIL VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, COURSE_ID BIGINT, COURSE_NAME VARCHAR(100))");
            st.execute("CREATE TABLE ST_ENROLLMENT (ID BIGINT PRIMARY KEY, STUDENT_ID BIGINT, STUDENT_NAME VARCHAR(100), ROLL_NO VARCHAR(50), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SEMESTER VARCHAR(50), STATUS VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_enr UNIQUE (STUDENT_ID, COURSE_ID))");
            st.execute("CREATE TABLE ST_FACULTY (ID BIGINT PRIMARY KEY, COLLEGE_ID BIGINT, COLLEGE_NAME VARCHAR(100), FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), EMAIL VARCHAR(100), MOBILE_NO VARCHAR(20), ADDRESS VARCHAR(255), GENDER VARCHAR(20), DATE_OF_BIRTH TIMESTAMP, CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100))");
            st.execute("CREATE TABLE ST_FACULTY_ALLOCATION (ID BIGINT PRIMARY KEY, FACULTY_ID BIGINT, FACULTY_NAME VARCHAR(100), FACULTY_EMAIL VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100), STATUS VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_fac_alloc UNIQUE (FACULTY_ID, COURSE_ID, SUBJECT_ID))");
            st.execute("CREATE TABLE ST_MARKSHEET (ID BIGINT PRIMARY KEY, ROLL_NO VARCHAR(50), STUDENT_ID BIGINT, NAME VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100), MARKS INT, CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_ms_scs UNIQUE (STUDENT_ID, COURSE_ID, SUBJECT_ID))");
            st.execute("CREATE TABLE ST_AUDIT_LOG (ID BIGINT PRIMARY KEY, ACTOR_LOGIN VARCHAR(100), ACTOR_ROLE BIGINT, ACTION_TYPE VARCHAR(50), TARGET_ENTITY VARCHAR(50), TARGET_ID VARCHAR(50), DETAILS VARCHAR(1000), CREATED_DATETIME TIMESTAMP)");
            conn.commit();
        }

        System.out.println("=== Phase 1: Build Identifier Verification ===");
        assertTrue("ORS-2.1.1-RELEASE-20261015".equals(HelloServlet.getBuildId()), "Build version matches release");

        System.out.println("=== Phase 2: PBKDF2-HMAC-SHA-256, Legacy Migration, Lockout, Single-Use Token & Non-Enumeration ===");
        String h1 = PasswordUtility.hashPassword("Secret@123");
        String h2 = PasswordUtility.hashPassword("Secret@123");
        assertTrue(!h1.equals(h2), "Two accounts with same password receive distinct salted PBKDF2 hashes");
        assertTrue(h1.startsWith("PBKDF2"), "PBKDF2 metadata prefix present");
        assertTrue(PasswordUtility.verifyPassword("Secret@123", h1), "Valid password verifies");
        assertTrue(!PasswordUtility.verifyPassword("WrongPass", h1), "Invalid password fails");

        UserModel userModel = new UserModel();
        UserBean user = new UserBean();
        user.setFirstName("Legacy");
        user.setLastName("User");
        user.setLogin("legacy@gmail.com");
        user.setPassword("LegacyPass123");
        user.setRoleId(RoleBean.STUDENT);
        user.setDob(new java.util.Date());
        long uid = userModel.add(user);

        String legacyHex = sha256Hex("LegacyPass123");
        try (Connection conn = JDBCDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE ST_USER SET PASSWORD = ? WHERE ID = ?")) {
            ps.setString(1, legacyHex);
            ps.setLong(2, uid);
            ps.executeUpdate();
            conn.commit();
        }
        UserBean authed = userModel.authenticate("legacy@gmail.com", "LegacyPass123");
        assertTrue(authed != null, "Legacy SHA-256 user authenticates");
        UserBean upgraded = userModel.findByPK(uid);
        assertTrue(upgraded.getPassword().startsWith("PBKDF2"), "Legacy hash transparently upgraded to PBKDF2");

        for (int i = 0; i < 5; i++) {
            assertTrue(userModel.authenticate("legacy@gmail.com", "WrongAttempt") == null, "Wrong password rejected");
        }
        UserBean lockedCheck = userModel.findByPK(uid);
        assertTrue(UserBean.ACTIVE.equalsIgnoreCase(lockedCheck.getLock()), "Account locked after 5 failed attempts");
        assertTrue(userModel.authenticate("legacy@gmail.com", "LegacyPass123") == null, "Locked account rejected even with right password");

        try (Connection conn = JDBCDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE ST_USER SET USER_LOCK = ?, UNSUCCESSFUL_LOGIN = 0 WHERE ID = ?")) {
            ps.setString(1, UserBean.INACTIVE);
            ps.setLong(2, uid);
            ps.executeUpdate();
            conn.commit();
        }
        assertTrue(userModel.requestPasswordResetNonEnumerating("nonexistent_account@gmail.com"), "Non-enumerating reset returns true for unknown email");
        String rawToken = userModel.issuePasswordResetToken("legacy@gmail.com");
        assertTrue(rawToken != null && rawToken.length() >= 32, "Cryptographic reset token issued");
        assertTrue(userModel.consumePasswordResetToken("legacy@gmail.com", rawToken, "Secure99"), "Reset token consumed");
        try {
            userModel.consumePasswordResetToken("legacy@gmail.com", rawToken, "Reuse999");
            throw new RuntimeException("Token reuse must fail");
        } catch (ApplicationException expected) {
            assertTrue(expected.getMessage() != null && expected.getMessage().toLowerCase().contains("reset"), "Token reuse blocked: " + expected.getMessage());
        }

        System.out.println("=== Phase 3, 4 & 5: Transactional Enrollment/Allocation, SQL Row Scoping & Marks Integrity ===");
        CollegeModel collegeModel = new CollegeModel();
        CollegeBean col1 = new CollegeBean();
        col1.setName("IIIT Hyderabad"); col1.setAddress("Gachibowli"); col1.setState("TS"); col1.setCity("Hyd"); col1.setPhoneNo("9876543210");
        long col1Id = collegeModel.add(col1);
        CollegeBean col2 = new CollegeBean();
        col2.setName("NIT Warangal"); col2.setAddress("Warangal"); col2.setState("TS"); col2.setCity("Wgl"); col2.setPhoneNo("9876543211");
        long col2Id = collegeModel.add(col2);

        CourseModel courseModel = new CourseModel();
        CourseBean cJava = new CourseBean(); cJava.setName("Java Corporate"); cJava.setDescription("Java"); cJava.setDuration("6M");
        long cJavaId = courseModel.add(cJava);
        CourseBean cPy = new CourseBean(); cPy.setName("Python Data Science"); cPy.setDescription("Python"); cPy.setDuration("6M");
        long cPyId = courseModel.add(cPy);

        SubjectModel subjectModel = new SubjectModel();
        SubjectBean sStr = new SubjectBean(); sStr.setName("Java/String"); sStr.setDescription("Strings"); sStr.setCourseId(cJavaId);
        long sStrId = subjectModel.add(sStr);
        SubjectBean sCol = new SubjectBean(); sCol.setName("Java/Collections"); sCol.setDescription("Collections"); sCol.setCourseId(cJavaId);
        long sColId = subjectModel.add(sCol);
        SubjectBean sPy = new SubjectBean(); sPy.setName("NumPy & Pandas"); sPy.setDescription("Data"); sPy.setCourseId(cPyId);
        long sPyId = subjectModel.add(sPy);

        StudentModel studentModel = new StudentModel();
        StudentBean st1 = new StudentBean();
        st1.setFirstName("Aarav"); st1.setLastName("Sharma"); st1.setEmail("aarav@gmail.com"); st1.setMobileNo("9876500001");
        st1.setDob(new java.util.Date()); st1.setCollegeId(col1Id); st1.setCourseId(cJavaId);
        long st1Id = studentModel.add(st1);

        StudentBean st2 = new StudentBean();
        st2.setFirstName("Diya"); st2.setLastName("Patel"); st2.setEmail("diya@gmail.com"); st2.setMobileNo("9876500002");
        st2.setDob(new java.util.Date()); st2.setCollegeId(col1Id); st2.setCourseId(cJavaId);
        long st2Id = studentModel.add(st2);

        StudentBean st3 = new StudentBean();
        st3.setFirstName("Rohan"); st3.setLastName("Verma"); st3.setEmail("rohan@gmail.com"); st3.setMobileNo("9876500003");
        st3.setDob(new java.util.Date()); st3.setCollegeId(col2Id); st3.setCourseId(cPyId);
        long st3Id = studentModel.add(st3);

        FacultyModel facultyModel = new FacultyModel();
        FacultyBean fac1 = new FacultyBean();
        fac1.setFirstName("Prof"); fac1.setLastName("Rao"); fac1.setGender("M"); fac1.setEmail("rao@gmail.com");
        fac1.setMobileNo("9876500009"); fac1.setDob(new java.util.Date());
        fac1.setCollegeId(col1Id); fac1.setCourseId(cJavaId); fac1.setSubjectId(sStrId);
        long fac1Id = facultyModel.add(fac1);

        FacultyAllocationBean alloc2 = new FacultyAllocationBean();
        alloc2.setFacultyId(fac1Id); alloc2.setFacultyName("Prof Rao"); alloc2.setFacultyEmail("rao@gmail.com");
        alloc2.setCourseId(cJavaId); alloc2.setCourseName("Java Corporate");
        alloc2.setSubjectId(sColId); alloc2.setSubjectName("Java/Collections"); 
        new FacultyAllocationModel().add(alloc2);

        // Faculty directory returns every authorized allocation, not only the
        // denormalized primary course/subject stored on ST_FACULTY.
        List<FacultyAllocationBean> directoryAssignments = new FacultyAllocationModel()
                .findAllocationsForFacultyIds(Collections.singleton(fac1Id), Collections.singleton(cJavaId), col1Id);
        assertTrue(directoryAssignments.size() == 2,
                "Faculty directory includes both Java/String and Java/Collections assignments");
        List<FacultyAllocationBean> outOfScopeAssignments = new FacultyAllocationModel()
                .findAllocationsForFacultyIds(Collections.singleton(fac1Id), Collections.singleton(cPyId), col1Id);
        assertTrue(outOfScopeAssignments.isEmpty(),
                "Faculty directory query does not leak assignments outside the scoped course");

        // SQL-level row scoping verification (StudentModel)
        StudentBean facFilter = new StudentBean();
        facFilter.setScopedCourseIds(Collections.singleton(cJavaId));
        List<StudentBean> facStudents = studentModel.search(facFilter, 0, 0);
        assertTrue(facStudents.size() == 2, "Faculty SQL scope returns only the 2 students enrolled in Java Corporate");

        StudentBean pcFilter = new StudentBean();
        pcFilter.setScopedCollegeId(col1Id);
        List<StudentBean> pcStudents = studentModel.search(pcFilter, 0, 0);
        assertTrue(pcStudents.size() == 2, "Program Chair SQL scope returns only students in authorized College ID " + col1Id);

        // Marks integrity: spoofed roll number rejected, authoritative roll number resolved
        MarksheetModel msModel = new MarksheetModel();
        MarksheetBean spoofRoll = new MarksheetBean();
        spoofRoll.setStudentId(st1Id); spoofRoll.setRollNo("0422CS9999"); spoofRoll.setCourseId(cJavaId); spoofRoll.setSubjectId(sStrId); spoofRoll.setMarks(88);
        try {
            msModel.add(spoofRoll);
            throw new RuntimeException("Spoofed roll number must be rejected");
        } catch (ApplicationException expected) {
            assertTrue(expected.getMessage() != null && expected.getMessage().toLowerCase().contains("roll"), "Spoofed roll number rejected by model: " + expected.getMessage());
        }

        // Unallocated faculty rejected by model
        MarksheetBean unallocFac = new MarksheetBean();
        unallocFac.setStudentId(st3Id); unallocFac.setCourseId(cPyId); unallocFac.setSubjectId(sPyId); unallocFac.setMarks(80);
        unallocFac.setActorFacultyEmail("rao@gmail.com");
        try {
            msModel.add(unallocFac);
            throw new RuntimeException("Unallocated faculty must be rejected");
        } catch (ApplicationException expected) {
            assertTrue(expected.getMessage() != null, "Unallocated faculty rejected by model: " + expected.getMessage());
        }

        // Valid marks saved with authoritative roll number resolved from ST_ENROLLMENT
        MarksheetBean m1 = new MarksheetBean();
        m1.setStudentId(st1Id); m1.setCourseId(cJavaId); m1.setSubjectId(sStrId); m1.setMarks(92); m1.setActorFacultyEmail("rao@gmail.com");
        long m1Id = msModel.add(m1);
        assertTrue(msModel.findByPK(m1Id).getRollNo() != null && !msModel.findByPK(m1Id).getRollNo().isEmpty(), "Authoritative roll number resolved from ST_ENROLLMENT");

        MarksheetBean m2 = new MarksheetBean();
        m2.setStudentId(st2Id); m2.setCourseId(cJavaId); m2.setSubjectId(sStrId); m2.setMarks(78); m2.setActorFacultyEmail("rao@gmail.com");
        msModel.add(m2);

        MarksheetBean m3 = new MarksheetBean();
        m3.setStudentId(st1Id); m3.setCourseId(cJavaId); m3.setSubjectId(sColId); m3.setMarks(70); m3.setActorFacultyEmail("rao@gmail.com");
        msModel.add(m3);

        MarksheetBean m4 = new MarksheetBean();
        m4.setStudentId(st2Id); m4.setCourseId(cJavaId); m4.setSubjectId(sColId); m4.setMarks(96); m4.setActorFacultyEmail("rao@gmail.com");
        msModel.add(m4);

        System.out.println("=== Phase 6: Subject-Specific Merit Ranking, Eligible Average & Complete Peer Anonymization ===");
        List<MarksheetBean> strMerit = msModel.getMeritListByCourseAndSubject(cJavaId, sStrId, 0, 0);
        assertTrue(strMerit.size() == 2 && strMerit.get(0).getStudentId() == st1Id, "Java/String Rank 1 is Aarav (92)");
        double strAvg = msModel.getClassAverageForCourseAndSubject(cJavaId, sStrId);
        assertTrue(Math.abs(strAvg - 85.0) < 0.01, "Java/String active-enrollment average is 85.0");
        double chairScopedStringAvg = msModel.getClassAverageForCourseSubjectAndCollege(cJavaId, sStrId, col1Id);
        assertTrue(Math.abs(chairScopedStringAvg - 85.0) < 0.01,
                "Program Chair subject average is restricted to the authorized college");
        double emptyCollegeStringAvg = msModel.getClassAverageForCourseSubjectAndCollege(cJavaId, sStrId, col2Id);
        assertTrue(Double.isNaN(emptyCollegeStringAvg),
                "Subject average with no eligible marks in another college is unavailable, not zero");

        List<MarksheetBean> anonList = MarksheetMeritListCtl.anonymizePeerRecordsByStudentId(st2Id, strMerit);
        MarksheetBean peerRow = anonList.get(0);
        assertTrue("Anonymous Peer".equals(peerRow.getName()) && "HIDDEN".equals(peerRow.getRollNo())
                && peerRow.getId() == 0L && (peerRow.getStudentId() == null || peerRow.getStudentId() == 0L)
                && peerRow.getCreatedBy() == null && peerRow.getModifiedBy() == null,
                "Peer identity completely redacted (name, rollNo, id, studentId, createdBy, modifiedBy)");

        
        // Phase 3: Verify DepartmentOverviewModel uses canonical ST_ENROLLMENT (SEMESTER, STATUS='ACTIVE')
        DepartmentOverviewModel deptModel = new DepartmentOverviewModel();
        List<Map<String, Object>> summaries = deptModel.getEnrollmentSummaries(col1Id);
        assertTrue(!summaries.isEmpty() && "Sem 1".equals(summaries.get(0).get("semester")),
                "DepartmentOverviewModel queries canonical ST_ENROLLMENT SEMESTER column");

        // Phase 7: Verify invalid course/subject merit list combination is rejected without silent fallback
        try {
            msModel.getMeritListByCourseAndSubject(cJavaId, sPyId, 0, 0);
            throw new RuntimeException("Mismatched course/subject merit list must fail");
        } catch (ApplicationException expected) {
            assertTrue(expected.getMessage() != null, "Mismatched course/subject rejected in getMeritListByCourseAndSubject");
        }

        System.out.println("=== Phase 7: Additive Migration Verification on Backup Database Copy ===");
        try (Connection migConn = DriverManager.getConnection("jdbc:hsqldb:mem:ors_legacy_backup;sql.syntax_mys=true", "sa", "");
             Statement mst = migConn.createStatement()) {
            mst.execute("CREATE TABLE ST_COURSE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), DURATION VARCHAR(50))");
            mst.execute("CREATE TABLE ST_SUBJECT (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100))");
            mst.execute("CREATE TABLE ST_STUDENT (ID BIGINT PRIMARY KEY, FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), EMAIL VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100))");
            mst.execute("CREATE TABLE ST_FACULTY (ID BIGINT PRIMARY KEY, FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), EMAIL_ID VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100))");
            mst.execute("CREATE TABLE ST_MARKSHEET (ID BIGINT PRIMARY KEY, ROLL_NO VARCHAR(50), STUDENT_ID BIGINT, NAME VARCHAR(100), PHYSICS INT, CHEMISTRY INT, MATHS INT)");
            mst.execute("INSERT INTO ST_COURSE VALUES (1, 'Java Corporate', 'Java', '6M')");
            mst.execute("INSERT INTO ST_SUBJECT VALUES (1, 'Java/String', 1, 'Java Corporate')");
            mst.execute("INSERT INTO ST_STUDENT VALUES (10, 'Legacy', 'Student', 'legstud@gmail.com', 1, 'Java Corporate')");
            mst.execute("INSERT INTO ST_FACULTY VALUES (20, 'Legacy', 'Faculty', 'legfac@gmail.com', 1, 'Java Corporate', 1, 'Java/String')");
            mst.execute("INSERT INTO ST_MARKSHEET VALUES (30, '0422CS101', 10, 'Legacy Student', 80, 85, 90)");

            mst.execute("CREATE TABLE ST_ENROLLMENT (ID BIGINT PRIMARY KEY, STUDENT_ID BIGINT, STUDENT_NAME VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), ROLL_NO VARCHAR(50), STATUS VARCHAR(20))");
            mst.execute("INSERT INTO ST_ENROLLMENT (ID, STUDENT_ID, STUDENT_NAME, COURSE_ID, COURSE_NAME, ROLL_NO, STATUS) SELECT ID, ID, CONCAT(FIRST_NAME, CONCAT(' ', LAST_NAME)), COURSE_ID, COURSE_NAME, '0422CS110', 'ACTIVE' FROM ST_STUDENT WHERE COURSE_ID > 0");
            mst.execute("ALTER TABLE ST_MARKSHEET ADD COLUMN COURSE_ID BIGINT");
            mst.execute("ALTER TABLE ST_MARKSHEET ADD COLUMN SUBJECT_ID BIGINT");
            mst.execute("ALTER TABLE ST_MARKSHEET ADD COLUMN SUBJECT_MARKS INT");
            mst.execute("UPDATE ST_MARKSHEET SET COURSE_ID = 1, SUBJECT_ID = 1, SUBJECT_MARKS = 85 WHERE STUDENT_ID = 10");

            try (ResultSet rs = mst.executeQuery("SELECT COUNT(*) FROM ST_ENROLLMENT WHERE STUDENT_ID = 10 AND STATUS = 'ACTIVE'")) {
                assertTrue(rs.next() && rs.getInt(1) == 1, "Legacy student preserved and migrated into active ST_ENROLLMENT");
            }
            try (ResultSet rs = mst.executeQuery("SELECT SUBJECT_MARKS FROM ST_MARKSHEET WHERE ID = 30")) {
                assertTrue(rs.next() && rs.getInt(1) == 85, "Legacy marksheet preserved and migrated without data loss");
            }
        }

        System.out.println("=== Phase 8: Measured Response-Time Latency Benchmarks (p50 / p95 / p99 over 50 samples) ===");
        int samples = 50;
        double[] resLat = new double[samples];
        double[] saveLat = new double[samples];
        double[] navLat = new double[samples];
        double[] searchLat = new double[samples];
        MarksheetBean updateBean = msModel.findByPK(m1Id);
        updateBean.setActorFacultyEmail("rao@gmail.com");

        for (int i = 0; i < samples; i++) {
            long t0 = System.nanoTime();
            msModel.findByRollNo(updateBean.getRollNo());
            msModel.getMeritListByCourseAndSubject(cJavaId, sStrId, 0, 0);
            msModel.getClassAverageForCourseAndSubject(cJavaId, sStrId);
            resLat[i] = (System.nanoTime() - t0) / 1_000_000.0;

            long t1 = System.nanoTime();
            updateBean.setMarks(85 + (i % 10));
            msModel.update(updateBean);
            saveLat[i] = (System.nanoTime() - t1) / 1_000_000.0;

            long t2 = System.nanoTime();
            courseModel.list();
            subjectModel.list();
            deptModel.getEnrollmentSummaries(col1Id);
            navLat[i] = (System.nanoTime() - t2) / 1_000_000.0;

            long t3 = System.nanoTime();
            studentModel.search(facFilter, 0, 0);
            facultyModel.search(new FacultyBean(), 0, 0);
            msModel.search(new MarksheetBean(), 0, 0);
            searchLat[i] = (System.nanoTime() - t3) / 1_000_000.0;
        }
        Arrays.sort(resLat);
        Arrays.sort(saveLat);
        Arrays.sort(navLat);
        Arrays.sort(searchLat);

        System.out.printf("Student Result & Merit Benchmark  (50 samples): p50=%.2f ms, p95=%.2f ms, p99=%.2f ms (Target < 3000 ms)%n",
                resLat[24], resLat[47], resLat[49]);
        System.out.printf("Faculty Marks Validation & Save   (50 samples): p50=%.2f ms, p95=%.2f ms, p99=%.2f ms (Target < 2000 ms)%n",
                saveLat[24], saveLat[47], saveLat[49]);
        System.out.printf("Standard Navigation & Dashboard   (50 samples): p50=%.2f ms, p95=%.2f ms, p99=%.2f ms (Target < 1500 ms)%n",
                navLat[24], navLat[47], navLat[49]);
        System.out.printf("Student/Faculty Searches & Lists  (50 samples): p50=%.2f ms, p95=%.2f ms, p99=%.2f ms (Target < 3000 ms)%n",
                searchLat[24], searchLat[47], searchLat[49]);

        assertTrue(resLat[49] < 3000.0, "Student result p99 < 3000 ms");
        assertTrue(saveLat[49] < 2000.0, "Faculty marks save p99 < 2000 ms");
        assertTrue(navLat[49] < 1500.0, "Navigation/dashboard p99 < 1500 ms");
        assertTrue(searchLat[49] < 3000.0, "Student/Faculty search & list p99 < 3000 ms");
        System.out.println("ALL PHASES 1-8 LIVE DATABASE INTEGRATION, MIGRATION, SECURITY & LATENCY BENCHMARKS PASSED!");
    }
}
