package com.sunilos.p4.ctl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;

/**
 * Route-Level, Filter-Level, and List-Deletion Role Authorization Test Suite
 * (Folder3 Audit P0 Fixes #1, #2, and Section 4 Filter Consistency).
 */
class FrontControllerAccessTest {

	@Test
	void testAdminRolePolicy() {
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/ctl/UserCtl", "POST", false));
		assertTrue(FrontController.isRoleAllowed(RoleBean.ADMIN, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertTrue(FrontController.isRoleAllowed(RoleBean.ADMIN, "/ORSV0/ctl/AuditCtl"));
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/rest/UserRestCtl/get/1", "GET", true));
	}

	@Test
	void testStudentRoleAndBenchmarkPolicy() {
		assertTrue(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/GetMarksheetCtl", "GET", false));
		assertFalse(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/ctl/GetMarksheetCtl", "GET", false),
				"Faculty must use scoped marks-entry routes, not the generic student results search");
		assertFalse(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/GetMarksheetCtl", "GET", false),
				"Program Chair must use the department-scoped audit workflow");
		assertTrue(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetMeritListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/MyProfileCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/ChangePasswordCtl"));

		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetCtl", "GET", false));
		assertFalse(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertFalse(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetListCtl"));
		assertFalse(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/EnrollmentListCtl"));
		assertFalse(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/AuditCtl"));
		assertFalse(FrontController.isRoleAllowed(RoleBean.STUDENT, "/ORSV0/ctl/UserCtl"));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/rest/studentctl/get/1", "GET", true));
	}

	@Test
	void testFacultyAndProgramChairAndUnknownRolePolicy() {
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/MarksheetListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/StudentListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/EnrollmentListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/CourseListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/SubjectListCtl"));
		assertTrue(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/rest/studentctl/search", "POST", true));
		assertFalse(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/AuditCtl"));
		assertFalse(FrontController.isRoleAllowed(RoleBean.FACULTY, "/ORSV0/ctl/UserCtl"));

		assertTrue(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/AuditCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/MarksheetListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/EnrollmentListCtl"));
		assertTrue(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/FacultyAllocationListCtl"));
		assertFalse(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/MarksheetCtl", "GET", false));
		assertFalse(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertFalse(FrontController.isRoleAllowed(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/UserCtl"));

		assertFalse(FrontController.isRoleAllowed(0L, "/ORSV0/ctl/GetMarksheetCtl"));
		assertFalse(FrontController.isRoleAllowed(-1L, "/ORSV0/ctl/WelcomeCtl"));
		assertFalse(FrontController.isRoleAllowed(999L, "/ORSV0/ctl/GetMarksheetCtl"));
		assertFalse(FrontController.isAuthorized(999L, "/ORSV0/ctl/MarksheetMeritListCtl", "GET", false));

		UserBean unknownUser = new UserBean();
		unknownUser.setLogin("unknown@gmail.com");
		unknownUser.setRoleId(999L);
		assertFalse(MarksheetAccessFilter.isRouteAllowed(unknownUser, "/ORSV0/ctl/MarksheetMeritListCtl", "GET"));
		assertFalse(MarksheetAccessFilter.isRouteAllowed(null, "/ORSV0/ctl/MarksheetMeritListCtl", "GET"));
	}

	@Test
	void testFrontControllerAndMarksheetAccessFilterConsistencyAndListDeleteProtection() {
		UserBean admin = new UserBean(); admin.setLogin("an@gmail.com"); admin.setRoleId(RoleBean.ADMIN);
		UserBean student = new UserBean(); student.setLogin("rahul@gmail.com"); student.setRoleId(RoleBean.STUDENT);
		UserBean chair = new UserBean(); chair.setLogin("chair@gmail.com"); chair.setRoleId(RoleBean.PROGRAM_CHAIR);
		UserBean faculty = new UserBean(); faculty.setLogin("ram@gmail.com"); faculty.setRoleId(RoleBean.FACULTY);

		// Section 4 Consistency: Both FrontController and MarksheetAccessFilter deny Program Chair and Student on /ctl/MarksheetCtl (GET and POST)
		for (String method : new String[] { "GET", "POST" }) {
			assertEquals(
					FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/MarksheetCtl", method, false),
					MarksheetAccessFilter.isRouteAllowed(chair, "/ORSV0/ctl/MarksheetCtl", method),
					"FrontController and MarksheetAccessFilter must agree on Program Chair access to MarksheetCtl");
			assertEquals(
					FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetCtl", method, false),
					MarksheetAccessFilter.isRouteAllowed(student, "/ORSV0/ctl/MarksheetCtl", method),
					"FrontController and MarksheetAccessFilter must agree on Student access to MarksheetCtl");
		}

		// P0 Fix #1: Generic list deletion is strictly forbidden for Faculty, Program Chair, and Student on StudentListCtl, EnrollmentListCtl, FacultyListCtl, CourseListCtl, and SubjectListCtl
		StudentListCtl studentListCtl = new StudentListCtl();
		EnrollmentListCtl enrollmentListCtl = new EnrollmentListCtl();
		FacultyListCtl facultyListCtl = new FacultyListCtl();
		FacultyAllocationListCtl allocListCtl = new FacultyAllocationListCtl();
		CourseListCtl courseListCtl = new CourseListCtl();
		SubjectListCtl subjectListCtl = new SubjectListCtl();

		for (BaseListCtl<?, ?> listCtl : new BaseListCtl<?, ?>[] {
				studentListCtl, enrollmentListCtl, facultyListCtl, allocListCtl, courseListCtl, subjectListCtl }) {
			assertTrue(listCtl.isDeleteAuthorizedForRecord(admin, 1L));
			assertFalse(listCtl.isDeleteAuthorizedForRecord(faculty, 1L),
					"Faculty must not delete records from " + listCtl.getClass().getSimpleName());
			assertFalse(listCtl.isDeleteAuthorizedForRecord(chair, 1L),
					"Program Chair must not delete records from " + listCtl.getClass().getSimpleName());
			assertFalse(listCtl.isDeleteAuthorizedForRecord(student, 1L),
					"Student must not delete records from " + listCtl.getClass().getSimpleName());
			assertFalse(listCtl.isDeleteAuthorizedForRecord(null, 1L));
		}
	}
}
