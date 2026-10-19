package com.sunilos.p4.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.util.JDBCDataSource;

class MarksheetAndEnrollmentModelTest {

	private long colId;
	private long cJavaId;
	private long cPyId;
	private long sStrId;
	private long sPyId;
	private long st1Id;
	private long st2Id;

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_ms_enr_test_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_ms_enr_test_db;sql.syntax_mys=true", "sa", ""));
		System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_USER", "sa");
		System.setProperty("ORS_DB_PASSWORD", "");
		JDBCDataSource.closeConnection(null);

		try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.execute("DROP TABLE ST_MARKSHEET IF EXISTS");
			st.execute("DROP TABLE ST_FACULTY_ALLOCATION IF EXISTS");
			st.execute("DROP TABLE ST_ENROLLMENT IF EXISTS");
			st.execute("DROP TABLE ST_FACULTY IF EXISTS");
			st.execute("DROP TABLE ST_STUDENT IF EXISTS");
			st.execute("DROP TABLE ST_SUBJECT IF EXISTS");
			st.execute("DROP TABLE ST_COURSE IF EXISTS");
			st.execute("DROP TABLE ST_COLLEGE IF EXISTS");

			st.execute("CREATE TABLE ST_COLLEGE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), ADDRESS VARCHAR(255), STATE VARCHAR(50), CITY VARCHAR(50), PHONE_NO VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			st.execute("CREATE TABLE ST_COURSE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), DURATION VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			st.execute("CREATE TABLE ST_SUBJECT (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_sc UNIQUE (COURSE_ID, NAME))");
			st.execute("CREATE TABLE ST_STUDENT (ID BIGINT PRIMARY KEY, COLLEGE_ID BIGINT, COLLEGE_NAME VARCHAR(100), FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), DATE_OF_BIRTH TIMESTAMP, MOBILE_NO VARCHAR(20), EMAIL VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, COURSE_ID BIGINT, COURSE_NAME VARCHAR(100))");
			st.execute("CREATE TABLE ST_ENROLLMENT (ID BIGINT PRIMARY KEY, STUDENT_ID BIGINT, STUDENT_NAME VARCHAR(100), ROLL_NO VARCHAR(50), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SEMESTER VARCHAR(50), STATUS VARCHAR(20), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_enr UNIQUE (STUDENT_ID, COURSE_ID))");
			st.execute("CREATE TABLE ST_FACULTY (ID BIGINT PRIMARY KEY, COLLEGE_ID BIGINT, COLLEGE_NAME VARCHAR(100), FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), EMAIL VARCHAR(100), MOBILE_NO VARCHAR(20), ADDRESS VARCHAR(255), GENDER VARCHAR(20), DATE_OF_BIRTH TIMESTAMP, CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100))");
			st.execute("CREATE TABLE ST_FACULTY_ALLOCATION (ID BIGINT PRIMARY KEY, FACULTY_ID BIGINT, FACULTY_NAME VARCHAR(100), FACULTY_EMAIL VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_fa UNIQUE (FACULTY_ID, COURSE_ID, SUBJECT_ID))");
			st.execute("CREATE TABLE ST_MARKSHEET (ID BIGINT PRIMARY KEY, ROLL_NO VARCHAR(50), STUDENT_ID BIGINT, NAME VARCHAR(100), COURSE_ID BIGINT, COURSE_NAME VARCHAR(100), SUBJECT_ID BIGINT, SUBJECT_NAME VARCHAR(100), MARKS INT, CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP, CONSTRAINT uq_ms UNIQUE (STUDENT_ID, COURSE_ID, SUBJECT_ID))");
			conn.commit();
		}

		CollegeBean col = new CollegeBean();
		col.setName("JIU"); col.setAddress("Rau"); col.setState("MP"); col.setCity("Indore"); col.setPhoneNo("9165254357");
		colId = new CollegeModel().add(col);

		CourseModel cm = new CourseModel();
		CourseBean cj = new CourseBean(); cj.setName("Java"); cj.setDescription("J"); cj.setDuration("6M");
		cJavaId = cm.add(cj);
		CourseBean cp = new CourseBean(); cp.setName("Python"); cp.setDescription("P"); cp.setDuration("6M");
		cPyId = cm.add(cp);

		SubjectModel sm = new SubjectModel();
		SubjectBean sj = new SubjectBean(); sj.setName("String"); sj.setDescription("Str"); sj.setCourseId(cJavaId);
		sStrId = sm.add(sj);
		SubjectBean sp = new SubjectBean(); sp.setName("Algorithms"); sp.setDescription("Algo"); sp.setCourseId(cPyId);
		sPyId = sm.add(sp);

		StudentModel stm = new StudentModel();
		StudentBean st1 = new StudentBean();
		st1.setFirstName("Manish"); st1.setLastName("Sharma"); st1.setEmail("manish@gmail.com"); st1.setMobileNo("7896321452");
		st1.setDob(new Date()); st1.setCollegeId(colId); st1.setCourseId(cJavaId);
		st1Id = stm.add(st1);

		StudentBean st2 = new StudentBean();
		st2.setFirstName("Vipin"); st2.setLastName("Chandore"); st2.setEmail("vipin@gmail.com"); st2.setMobileNo("9165254358");
		st2.setDob(new Date()); st2.setCollegeId(colId); st2.setCourseId(cJavaId);
		st2Id = stm.add(st2);

		EnrollmentBean pyEnr = new EnrollmentBean();
		pyEnr.setStudentId(st1Id); pyEnr.setStudentName("Manish Sharma"); pyEnr.setRollNo("1");
		pyEnr.setCourseId(cPyId); pyEnr.setCourseName("Python"); pyEnr.setSemester("Sem 1"); pyEnr.setStatus("ACTIVE");
		new EnrollmentModel().add(pyEnr);

		FacultyBean fac = new FacultyBean();
		fac.setFirstName("Ram"); fac.setLastName("Sharma"); fac.setEmail("ram@gmail.com"); fac.setMobileNo("9856569858");
		fac.setGender("Male"); fac.setDob(new Date()); fac.setCollegeId(colId); fac.setCourseId(cJavaId); fac.setSubjectId(sStrId);
		new FacultyModel().add(fac);
	}

	@Test
	void testMarksheetCourseWiseGroupingAndClassAverageEnrollmentFiltering() throws Exception {
		FacultyAllocationModel allocModel = new FacultyAllocationModel();
		assertTrue(allocModel.isFacultyAllocatedByEmail("ram@gmail.com", cJavaId, sStrId));
		assertFalse(allocModel.isFacultyAllocatedByEmail("unassigned@gmail.com", cJavaId, sStrId));

		MarksheetModel mm = new MarksheetModel();

		// Phase 4: Missing/invalid course or mismatched subject throws ApplicationException instead of returning 0.0
		assertThrows(ApplicationException.class, () -> mm.getClassAverageForCourse(0L));
		assertThrows(ApplicationException.class, () -> mm.getClassAverageForCourse(9999L));
		assertThrows(ApplicationException.class, () -> mm.getClassAverageForCourseAndSubject(0L, sStrId));
		assertThrows(ApplicationException.class, () -> mm.getClassAverageForCourseAndSubject(cJavaId, 0L));
		assertThrows(ApplicationException.class, () -> mm.getClassAverageForCourseAndSubject(cJavaId, sPyId));

		MarksheetBean m1 = new MarksheetBean();
		m1.setStudentId(st1Id); m1.setCourseId(cJavaId); m1.setSubjectId(sStrId); m1.setMarks(90);
		mm.add(m1);

		MarksheetBean m2 = new MarksheetBean();
		m2.setStudentId(st2Id); m2.setCourseId(cJavaId); m2.setSubjectId(sStrId); m2.setMarks(70);
		mm.add(m2);

		MarksheetBean mPy = new MarksheetBean();
		mPy.setStudentId(st1Id); mPy.setCourseId(cPyId); mPy.setSubjectId(sPyId); mPy.setMarks(88);
		mm.add(mPy);

		// Both st1 (90) and st2 (70) are ACTIVE -> average = 80.0
		assertTrue(Math.abs(mm.getClassAverageForCourseAndSubject(cJavaId, sStrId) - 80.0) < 0.01);
		assertTrue(Math.abs(mm.getClassAverageForCourse(cJavaId) - 80.0) < 0.01);

		// Mark st2's enrollment as INACTIVE -> st2 (70) must be excluded, so average becomes 90.0
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_ENROLLMENT SET STATUS = 'INACTIVE' WHERE STUDENT_ID = ? AND COURSE_ID = ?")) {
			ps.setLong(1, st2Id);
			ps.setLong(2, cJavaId);
			ps.executeUpdate();
			conn.commit();
		}
		assertTrue(Math.abs(mm.getClassAverageForCourseAndSubject(cJavaId, sStrId) - 90.0) < 0.01,
				"Inactive enrollment must be excluded from subject class average");
		assertTrue(Math.abs(mm.getClassAverageForCourse(cJavaId) - 90.0) < 0.01,
				"Inactive enrollment must be excluded from course class average");

		// Delete st2's enrollment row completely -> missing enrollment row must also be excluded (average remains 90.0)
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("DELETE FROM ST_ENROLLMENT WHERE STUDENT_ID = ? AND COURSE_ID = ?")) {
			ps.setLong(1, st2Id);
			ps.setLong(2, cJavaId);
			ps.executeUpdate();
			conn.commit();
		}
		assertTrue(Math.abs(mm.getClassAverageForCourseAndSubject(cJavaId, sStrId) - 90.0) < 0.01,
				"Missing enrollment row must not be included in subject class average");

		List<MarksheetBean> courses = mm.findCourseWiseMarksByRollNo("1");
		assertNotNull(courses);
		assertEquals(2, courses.size());
		assertEquals("Java", courses.get(0).getCourseName());
		assertEquals(Integer.valueOf(90), courses.get(0).getSubjectMarks().get("String"));
		assertEquals("Python", courses.get(1).getCourseName());
		assertEquals(Integer.valueOf(88), courses.get(1).getSubjectMarks().get("Algorithms"));

		// A student's roll number may differ by active course. The authenticated
		// results workflow must query by authoritative Student ID + Course ID,
		// not assume that every enrollment reuses the same roll number.
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement updateEnrollment = conn.prepareStatement(
						"UPDATE ST_ENROLLMENT SET ROLL_NO = ? WHERE STUDENT_ID = ? AND COURSE_ID = ?");
				PreparedStatement updateMarks = conn.prepareStatement(
						"UPDATE ST_MARKSHEET SET ROLL_NO = ? WHERE STUDENT_ID = ? AND COURSE_ID = ?")) {
			updateEnrollment.setString(1, "PY-1");
			updateEnrollment.setLong(2, st1Id);
			updateEnrollment.setLong(3, cPyId);
			updateEnrollment.executeUpdate();
			updateMarks.setString(1, "PY-1");
			updateMarks.setLong(2, st1Id);
			updateMarks.setLong(3, cPyId);
			updateMarks.executeUpdate();
			conn.commit();
		}
		List<MarksheetBean> pythonByIdentity = mm.findCourseWiseMarksByStudentIdAndCourseId(st1Id, cPyId);
		assertEquals(1, pythonByIdentity.size());
		assertEquals("Python", pythonByIdentity.get(0).getCourseName());
		assertEquals(Integer.valueOf(88), pythonByIdentity.get(0).getSubjectMarks().get("Algorithms"));
		assertEquals("PY-1", pythonByIdentity.get(0).getRollNo());
	}

	@Test
	void testCourseWiseDisplayCalculationsUseAvailableBeanApi() {
		MarksheetBean bean = new MarksheetBean();
		java.util.Map<String, Integer> subjectMarks = new java.util.LinkedHashMap<>();
		subjectMarks.put("String", 95);
		subjectMarks.put("Collections", 70);
		bean.setSubjectMarks(subjectMarks);
		assertEquals(165, bean.getTotalCourseMarks());
		assertEquals(200, bean.getMaxCourseMarks());
		assertTrue(Math.abs(bean.getCoursePercentage() - 82.5) < 0.01);
	}

	@Test
	void testEnrollmentModelSqlLevelRowScopingForFacultyAndProgramChair() throws Exception {
		CollegeModel colModel = new CollegeModel();
		CollegeBean col2 = new CollegeBean();
		col2.setName("NITW"); col2.setAddress("Wgl"); col2.setState("TS"); col2.setCity("Wgl"); col2.setPhoneNo("9826012999");
		long col2Id = colModel.add(col2);

		StudentBean stOtherCol = new StudentBean();
		stOtherCol.setFirstName("Rohan"); stOtherCol.setLastName("Verma"); stOtherCol.setEmail("rohan.other@gmail.com");
		stOtherCol.setMobileNo("9826011999"); stOtherCol.setDob(new Date()); stOtherCol.setCollegeId(col2Id); stOtherCol.setCourseId(cPyId);
		new StudentModel().add(stOtherCol);

		EnrollmentModel em = new EnrollmentModel();

		// 1. Faculty scoped to cJavaId only sees enrollments in cJavaId (never cPyId)
		EnrollmentBean facFilter = new EnrollmentBean();
		facFilter.setScopedCourseIds(java.util.Collections.singleton(cJavaId));
		List<EnrollmentBean> facEnrollments = em.search(facFilter, 0, 0);
		assertEquals(2, facEnrollments.size());
		for (EnrollmentBean eb : facEnrollments) {
			assertEquals(cJavaId, eb.getCourseId());
		}

		// 2. Program Chair scoped to colId only sees enrollments for students in colId (excludes stOtherCol in col2Id)
		EnrollmentBean chairFilter = new EnrollmentBean();
		chairFilter.setScopedCollegeId(colId);
		List<EnrollmentBean> chairEnrollments = em.search(chairFilter, 0, 0);
		assertEquals(3, chairEnrollments.size());
		for (EnrollmentBean eb : chairEnrollments) {
			assertTrue(eb.getStudentId() == st1Id || eb.getStudentId() == st2Id);
		}
	}
	@Test
	void testMeritListExcludesMarksWhereSubjectBelongsToDifferentCourse() throws Exception {
		// Simulate a bad legacy/corrupt row: Java course ID paired with a Python subject ID.
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement(
						"INSERT INTO ST_MARKSHEET (ID, ROLL_NO, STUDENT_ID, NAME, COURSE_ID, COURSE_NAME, SUBJECT_ID, SUBJECT_NAME, MARKS, CREATED_BY, MODIFIED_BY, CREATED_DATETIME, MODIFIED_DATETIME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
			ps.setLong(1, 999);
			ps.setString(2, "ROLL-CROSS-COURSE");
			ps.setLong(3, st1Id);
			ps.setString(4, "Manish Sharma");
			ps.setLong(5, cJavaId);
			ps.setString(6, "Java");
			ps.setLong(7, sPyId);
			ps.setString(8, "Algorithms");
			ps.setInt(9, 99);
			ps.setString(10, "test");
			ps.setString(11, "test");
			ps.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
			ps.setTimestamp(13, new java.sql.Timestamp(System.currentTimeMillis()));
			ps.executeUpdate();
		}

		List<MarksheetBean> javaMerit = new MarksheetModel().getMeritListByCourseAndSubject(cJavaId, 0L, 1, 10);
		assertTrue(javaMerit.isEmpty(), "Marks with a subject from another course must not appear in this course's merit list");
	}

}
