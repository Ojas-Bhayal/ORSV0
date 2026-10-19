package com.sunilos.p4.ctl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.model.CollegeModel;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * Server-Side Marks Validation & Direct Marksheet Read/Write Authorization Test Suite
 * (Phase 2, Phase 6 & Phase 7 of ORSV0_7_Implementation_Plan.md).
 */
class MarksheetCtlValidationTest {

	private long col1Id;
	private long col2Id;
	private long cJavaId;
	private long cPyId;
	private long sStrId;
	private long sColId;
	private long sPyId;
	private long stJavaId;
	private long stPyId;

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_ms_val_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_ms_val_db;sql.syntax_mys=true", "sa", ""));
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

		CollegeModel colModel = new CollegeModel();
		CollegeBean col1 = new CollegeBean();
		col1.setName("IIITH"); col1.setAddress("Hyd"); col1.setState("TS"); col1.setCity("Hyd"); col1.setPhoneNo("9826012345");
		col1Id = colModel.add(col1);
		CollegeBean col2 = new CollegeBean();
		col2.setName("NITW"); col2.setAddress("Wgl"); col2.setState("TS"); col2.setCity("Wgl"); col2.setPhoneNo("9826012346");
		col2Id = colModel.add(col2);

		CourseModel cm = new CourseModel();
		CourseBean cj = new CourseBean(); cj.setName("Java"); cj.setDescription("Java"); cj.setDuration("6M");
		cJavaId = cm.add(cj);
		CourseBean cp = new CourseBean(); cp.setName("Python"); cp.setDescription("Python"); cp.setDuration("6M");
		cPyId = cm.add(cp);

		SubjectModel sm = new SubjectModel();
		SubjectBean sj = new SubjectBean(); sj.setName("String"); sj.setDescription("Str"); sj.setCourseId(cJavaId);
		sStrId = sm.add(sj);
		SubjectBean sc = new SubjectBean(); sc.setName("Collections"); sc.setDescription("Col"); sc.setCourseId(cJavaId);
		sColId = sm.add(sc);
		SubjectBean sp = new SubjectBean(); sp.setName("Algorithms"); sp.setDescription("Algo"); sp.setCourseId(cPyId);
		sPyId = sm.add(sp);

		StudentModel stm = new StudentModel();
		StudentBean st1 = new StudentBean();
		st1.setFirstName("Rahul"); st1.setLastName("Sahu"); st1.setEmail("rahul@gmail.com"); st1.setMobileNo("9826011111");
		st1.setDob(new Date()); st1.setCollegeId(col1Id); st1.setCourseId(cJavaId);
		stJavaId = stm.add(st1);

		StudentBean st2 = new StudentBean();
		st2.setFirstName("Rohan"); st2.setLastName("Verma"); st2.setEmail("rohan@gmail.com"); st2.setMobileNo("9826011112");
		st2.setDob(new Date()); st2.setCollegeId(col2Id); st2.setCourseId(cPyId);
		stPyId = stm.add(st2);

		FacultyModel fm = new FacultyModel();
		FacultyBean fac = new FacultyBean();
		fac.setFirstName("Ram"); fac.setLastName("Sharma"); fac.setEmail("ram@gmail.com"); fac.setMobileNo("9856569858");
		fac.setGender("Male"); fac.setDob(new Date()); fac.setCollegeId(col1Id); fac.setCourseId(cJavaId); fac.setSubjectId(sStrId);
		fm.add(fac);

		FacultyBean chair = new FacultyBean();
		chair.setFirstName("Ojas"); chair.setLastName("Bhayal"); chair.setEmail("chair@gmail.com"); chair.setMobileNo("9165254357");
		chair.setGender("Male"); chair.setDob(new Date()); chair.setCollegeId(col1Id); chair.setCourseId(cJavaId); chair.setSubjectId(sStrId);
		fm.add(chair);
	}

	@Test
	void testMarksRangeAndRelationshipValidation() throws Exception {
		MarksheetModel mm = new MarksheetModel();

		// 1. Out-of-range marks (< 0 or > 100) rejected
		MarksheetBean neg = new MarksheetBean();
		neg.setStudentId(stJavaId); neg.setCourseId(cJavaId); neg.setSubjectId(sStrId); neg.setMarks(-5);
		assertThrows(ApplicationException.class, () -> mm.add(neg));

		MarksheetBean over = new MarksheetBean();
		over.setStudentId(stJavaId); over.setCourseId(cJavaId); over.setSubjectId(sStrId); over.setMarks(105);
		assertThrows(ApplicationException.class, () -> mm.add(over));

		// 2. Cross-course subject rejected
		MarksheetBean crossSubj = new MarksheetBean();
		crossSubj.setStudentId(stJavaId); crossSubj.setCourseId(cJavaId); crossSubj.setSubjectId(sPyId); crossSubj.setMarks(85);
		assertThrows(ApplicationException.class, () -> mm.add(crossSubj));

		// 3. Unenrolled course rejected
		MarksheetBean unenrolled = new MarksheetBean();
		unenrolled.setStudentId(stJavaId); unenrolled.setCourseId(cPyId); unenrolled.setSubjectId(sPyId); unenrolled.setMarks(85);
		assertThrows(ApplicationException.class, () -> mm.add(unenrolled));

		// 4. Spoofed roll number rejected
		MarksheetBean spoofRoll = new MarksheetBean();
		spoofRoll.setStudentId(stJavaId); spoofRoll.setRollNo("SPOOF999"); spoofRoll.setCourseId(cJavaId); spoofRoll.setSubjectId(sStrId); spoofRoll.setMarks(85);
		assertThrows(ApplicationException.class, () -> mm.add(spoofRoll));

		// 5. Unallocated faculty rejected
		MarksheetBean unallocFac = new MarksheetBean();
		unallocFac.setStudentId(stPyId); unallocFac.setCourseId(cPyId); unallocFac.setSubjectId(sPyId); unallocFac.setMarks(85);
		unallocFac.setActorFacultyEmail("ram@gmail.com");
		assertThrows(ApplicationException.class, () -> mm.add(unallocFac));

		// 6. Valid marks saved & authoritative roll number resolved; duplicate rejected
		MarksheetBean valid = new MarksheetBean();
		valid.setStudentId(stJavaId); valid.setCourseId(cJavaId); valid.setSubjectId(sStrId); valid.setMarks(91);
		valid.setActorFacultyEmail("ram@gmail.com");
		long mid = mm.add(valid);
		assertTrue(mid > 0);
		assertEquals("1", mm.findByPK(mid).getRollNo());

		MarksheetBean dup = new MarksheetBean();
		dup.setStudentId(stJavaId); dup.setCourseId(cJavaId); dup.setSubjectId(sStrId); dup.setMarks(95);
		assertThrows(DuplicateRecordException.class, () -> mm.add(dup));
	}

	@Test
	void testDirectMarksheetReadAndWriteRowLevelAuthorizationMatrix() throws Exception {
		MarksheetModel mm = new MarksheetModel();
		MarksheetBean mJavaStr = new MarksheetBean();
		mJavaStr.setStudentId(stJavaId); mJavaStr.setCourseId(cJavaId); mJavaStr.setSubjectId(sStrId); mJavaStr.setMarks(90);
		long mJavaStrId = mm.add(mJavaStr);
		MarksheetBean savedJavaStr = mm.findByPK(mJavaStrId);

		MarksheetBean mJavaCol = new MarksheetBean();
		mJavaCol.setStudentId(stJavaId); mJavaCol.setCourseId(cJavaId); mJavaCol.setSubjectId(sColId); mJavaCol.setMarks(84);
		long mJavaColId = mm.add(mJavaCol);
		MarksheetBean savedJavaCol = mm.findByPK(mJavaColId);

		MarksheetBean mPyAlgo = new MarksheetBean();
		mPyAlgo.setStudentId(stPyId); mPyAlgo.setCourseId(cPyId); mPyAlgo.setSubjectId(sPyId); mPyAlgo.setMarks(88);
		long mPyAlgoId = mm.add(mPyAlgo);
		MarksheetBean savedPyAlgo = mm.findByPK(mPyAlgoId);

		UserBean studentA = new UserBean();
		studentA.setLogin("rahul@gmail.com");
		studentA.setRoleId(RoleBean.STUDENT);

		UserBean facultyRam = new UserBean();
		facultyRam.setLogin("ram@gmail.com");
		facultyRam.setRoleId(RoleBean.FACULTY);

		UserBean chairCol1 = new UserBean();
		chairCol1.setLogin("chair@gmail.com");
		chairCol1.setRoleId(RoleBean.PROGRAM_CHAIR);

		UserBean admin = new UserBean();
		admin.setLogin("an@gmail.com");
		admin.setRoleId(RoleBean.ADMIN);

		// 1. Unauthenticated request is rejected
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(null, savedJavaStr));

		// 2. Student A can read own marksheet, but CANNOT read Student B's marksheet or write any marksheet
		assertTrue(MarksheetCtl.isAuthorizedToReadMarksheet(studentA, savedJavaStr));
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(studentA, savedPyAlgo));
		assertFalse(MarksheetCtl.isAuthorizedToWriteMarksheet(studentA, savedJavaStr));

		// 3. Faculty Ram can read/write allocated Java/String, but CANNOT read/write unallocated Java/Collections or Python/Algorithms
		assertTrue(MarksheetCtl.isAuthorizedToReadMarksheet(facultyRam, savedJavaStr));
		assertTrue(MarksheetCtl.isAuthorizedToWriteMarksheet(facultyRam, savedJavaStr));
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(facultyRam, savedJavaCol),
				"Faculty must not read marksheet for unallocated subject in same course");
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(facultyRam, savedPyAlgo),
				"Faculty must not read marksheet for student outside allocated course");

		// 4. Program Chair of College 1 can read College 1 student's marksheet, CANNOT read College 2 student's marksheet, and CANNOT write
		assertTrue(MarksheetCtl.isAuthorizedToReadMarksheet(chairCol1, savedJavaStr));
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(chairCol1, savedPyAlgo),
				"Program Chair must not read records from another college/department");
		assertFalse(MarksheetCtl.isAuthorizedToWriteMarksheet(chairCol1, savedJavaStr));

		// 5. Admin can read and write all records
		assertTrue(MarksheetCtl.isAuthorizedToReadMarksheet(admin, savedPyAlgo));
		assertTrue(MarksheetCtl.isAuthorizedToWriteMarksheet(admin, savedPyAlgo));

		// 6. Verify MarksheetAccessFilter permits Student on GetMarksheetCtl and MarksheetMeritListCtl, but denies MarksheetCtl
		assertTrue(MarksheetAccessFilter.isRouteAllowed(studentA, "/ORSV0/ctl/GetMarksheetCtl", "GET"));
		assertTrue(MarksheetAccessFilter.isRouteAllowed(studentA, "/ORSV0/ctl/MarksheetMeritListCtl", "GET"),
				"Student must be allowed to reach MarksheetMeritListCtl for anonymized benchmark");
		assertFalse(MarksheetAccessFilter.isRouteAllowed(studentA, "/ORSV0/ctl/MarksheetCtl", "GET"),
				"Student must not access faculty marks-entry controller");
		assertFalse(MarksheetAccessFilter.isRouteAllowed(studentA, "/ORSV0/ctl/MarksheetListCtl", "GET"));
		assertTrue(MarksheetAccessFilter.isRouteAllowed(facultyRam, "/ORSV0/ctl/MarksheetCtl", "POST"));
		assertFalse(MarksheetAccessFilter.isRouteAllowed(chairCol1, "/ORSV0/ctl/MarksheetCtl", "GET"));
		assertFalse(MarksheetAccessFilter.isRouteAllowed(chairCol1, "/ORSV0/ctl/MarksheetCtl", "POST"));

		// 6b. Deletion is an administrative operation; grading allocation grants
		// permission to read/write assigned marks, not to delete marksheet records.
		MarksheetListCtl msListCtl = new MarksheetListCtl();
		assertTrue(msListCtl.isDeleteAuthorizedForRecord(admin, mJavaStrId));
		assertFalse(msListCtl.isDeleteAuthorizedForRecord(facultyRam, mJavaStrId),
				"Faculty grading allocation must not grant marksheet deletion permission");
		assertFalse(msListCtl.isDeleteAuthorizedForRecord(facultyRam, mJavaColId),
				"Faculty Ram must NOT delete marksheet record for unallocated Java/Collections");
		assertFalse(msListCtl.isDeleteAuthorizedForRecord(facultyRam, mPyAlgoId),
				"Faculty Ram must NOT delete marksheet record for unallocated Python/Algorithms");
		assertFalse(msListCtl.isDeleteAuthorizedForRecord(chairCol1, mJavaStrId),
				"Program Chair must NOT delete marksheet records from MarksheetListCtl");
		assertFalse(msListCtl.isDeleteAuthorizedForRecord(studentA, mJavaStrId),
				"Student must NOT delete marksheet records from MarksheetListCtl");

		// 7. Deactivate/remove faculty allocation, then verify read/write AND ScopeAuthorizationUtil/MarksheetAccessFilter are immediately denied even though ST_FACULTY still has courseId/subjectId
		FacultyAllocationModel fam = new FacultyAllocationModel();
		List<FacultyAllocationBean> ramAllocs = fam.findByFacultyEmail("ram@gmail.com");
		for (FacultyAllocationBean fa : ramAllocs) {
			fam.delete(fa);
		}
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(facultyRam, savedJavaStr),
				"Deactivated/removed allocation must immediately deny read access");
		assertFalse(com.sunilos.p4.util.ScopeAuthorizationUtil.resolveScope(facultyRam).isValid(),
				"Stale course/subject fields on ST_FACULTY must not keep Faculty scope active after allocation removal");
		assertFalse(MarksheetAccessFilter.isRouteAllowed(facultyRam, "/ORSV0/ctl/MarksheetCtl", "GET"),
				"MarksheetAccessFilter must deny Faculty after allocation removal");

		// 7. Deactivate student enrollment, then verify Student read is denied
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_ENROLLMENT SET STATUS = 'INACTIVE' WHERE STUDENT_ID = ?")) {
			ps.setLong(1, stJavaId);
			ps.executeUpdate();
			conn.commit();
		}
		assertFalse(MarksheetCtl.isAuthorizedToReadMarksheet(studentA, savedJavaStr),
				"Inactive enrollment must deny student marksheet read");
	}

	@Test
	void testStudentBenchmarkDefaultCourseSelectsOnlyActiveEnrollment() throws Exception {
		// Enroll Rahul (stJavaId) in Python (cPyId) as ACTIVE as well
		com.sunilos.p4.model.EnrollmentModel em = new com.sunilos.p4.model.EnrollmentModel();
		com.sunilos.p4.bean.EnrollmentBean pyEnr = new com.sunilos.p4.bean.EnrollmentBean();
		pyEnr.setStudentId(stJavaId);
		pyEnr.setStudentName("Rahul Sahu");
		pyEnr.setRollNo("1");
		pyEnr.setCourseId(cPyId);
		pyEnr.setCourseName("Python");
		pyEnr.setSemester("Sem 1");
		pyEnr.setStatus("ACTIVE");
		em.add(pyEnr);

		UserBean studentA = new UserBean();
		studentA.setLogin("rahul@gmail.com");
		studentA.setRoleId(RoleBean.STUDENT);

		// 1. Multiple active enrollments -> deterministic default selects lowest courseId (cJavaId)
		assertEquals(cJavaId, MarksheetMeritListCtl.resolveAuthorizedStudentBenchmarkCourse(studentA, 0L));

		// 2. Mark first enrollment (cJavaId) INACTIVE -> default selection MUST skip inactive cJavaId and choose active cPyId!
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_ENROLLMENT SET STATUS = 'INACTIVE' WHERE STUDENT_ID = ? AND COURSE_ID = ?")) {
			ps.setLong(1, stJavaId);
			ps.setLong(2, cJavaId);
			ps.executeUpdate();
			conn.commit();
		}
		assertEquals(cPyId, MarksheetMeritListCtl.resolveAuthorizedStudentBenchmarkCourse(studentA, 0L),
				"Default benchmark course selection must never choose an INACTIVE enrollment");

		// 3. Explicitly requesting inactive course cJavaId must be rejected
		assertThrows(ApplicationException.class,
				() -> MarksheetMeritListCtl.resolveAuthorizedStudentBenchmarkCourse(studentA, cJavaId));

		// 4. Mark second enrollment (cPyId) INACTIVE too -> student has NO active enrollments -> must be rejected
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_ENROLLMENT SET STATUS = 'INACTIVE' WHERE STUDENT_ID = ? AND COURSE_ID = ?")) {
			ps.setLong(1, stJavaId);
			ps.setLong(2, cPyId);
			ps.executeUpdate();
			conn.commit();
		}
		assertThrows(ApplicationException.class,
				() -> MarksheetMeritListCtl.resolveAuthorizedStudentBenchmarkCourse(studentA, 0L));
	}
	@Test
	void testCourseWiseResultsUseStudentAndCourseIdentityAcrossActiveEnrollments() throws Exception {
		MarksheetModel model = new MarksheetModel();

		MarksheetBean javaString = new MarksheetBean();
		javaString.setStudentId(stJavaId);
		javaString.setCourseId(cJavaId);
		javaString.setSubjectId(sStrId);
		javaString.setMarks(91);
		model.add(javaString);

		MarksheetBean javaCollections = new MarksheetBean();
		javaCollections.setStudentId(stJavaId);
		javaCollections.setCourseId(cJavaId);
		javaCollections.setSubjectId(sColId);
		javaCollections.setMarks(87);
		model.add(javaCollections);

		EnrollmentBean additionalCourse = new EnrollmentBean();
		additionalCourse.setStudentId(stJavaId);
		additionalCourse.setCourseId(cPyId);
		additionalCourse.setRollNo("RAHUL-PY-01");
		additionalCourse.setSemester("Sem 1");
		additionalCourse.setStatus("ACTIVE");
		new EnrollmentModel().add(additionalCourse);

		MarksheetBean pythonAlgorithms = new MarksheetBean();
		pythonAlgorithms.setStudentId(stJavaId);
		pythonAlgorithms.setCourseId(cPyId);
		pythonAlgorithms.setSubjectId(sPyId);
		pythonAlgorithms.setMarks(84);
		model.add(pythonAlgorithms);

		List<MarksheetBean> javaResults = model.findCourseWiseMarksByStudentIdAndCourseId(stJavaId, cJavaId);
		List<MarksheetBean> pythonResults = model.findCourseWiseMarksByStudentIdAndCourseId(stJavaId, cPyId);

		assertEquals(1, javaResults.size(), "A student should have one grouped result per course");
		assertEquals(2, javaResults.get(0).getSubjectMarks().size(), "Java result should include both subjects");
		assertEquals(1, pythonResults.size(), "Additional course result must be found by student ID + course ID");
		assertEquals(cPyId, pythonResults.get(0).getCourseId());
		assertEquals(stJavaId, pythonResults.get(0).getStudentId());
	}

}
