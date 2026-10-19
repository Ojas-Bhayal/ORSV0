package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ServletUtility;

/**
 * Marksheet Controller (Phase 2 Direct Read & Write Record-Level Authorization):
 * - Enforces fail-closed row-level read authorization in doGet() before placing
 *   any MarksheetBean into the request or rendering a view.
 * - Enforces fail-closed row-level write/delete authorization in doPost() and
 *   validate() for Faculty and Admin roles.
 * - Verifies Student ownership (by authoritative StudentBean ID + active enrollment),
 *   Faculty active course/subject allocation + student active enrollment, and
 *   Program Chair department/college scope.
 */
@WebServlet("/ctl/MarksheetCtl")
public class MarksheetCtl extends BaseCtl<MarksheetBean, MarksheetModel> {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(MarksheetCtl.class);

	/**
	 * Fail-closed row-level read authorization check for a specific MarksheetBean record.
	 * Never trusts a submitted studentId, marksheetId, courseId, subjectId, or rollNo
	 * without verifying the authenticated user's authoritative database scope.
	 */
	public static boolean isAuthorizedToReadMarksheet(UserBean user, MarksheetBean bean) {
		if (user == null || bean == null) {
			return false;
		}
		try {
			long roleId = user.getRoleId();
			if (roleId == RoleBean.ADMIN) {
				return true;
			}
			if (roleId == RoleBean.STUDENT) {
				if (DataValidator.isNull(user.getLogin()) || bean.getStudentId() == null || bean.getStudentId() <= 0) {
					return false;
				}
				StudentModel studentModel = new StudentModel();
				StudentBean student = studentModel.findByEmailId(user.getLogin());
				if (student == null || student.getId() <= 0 || student.getId() != bean.getStudentId().longValue()) {
					return false;
				}
				EnrollmentModel enrollmentModel = new EnrollmentModel();
				return enrollmentModel.isStudentEnrolledInCourse(student.getId(), bean.getCourseId());
			}
			if (roleId == RoleBean.FACULTY) {
				if (DataValidator.isNull(user.getLogin()) || bean.getCourseId() <= 0 || bean.getSubjectId() <= 0
						|| bean.getStudentId() == null || bean.getStudentId() <= 0) {
					return false;
				}
				FacultyAllocationModel allocModel = new FacultyAllocationModel();
				if (!allocModel.isFacultyAllocatedByEmail(user.getLogin(), bean.getCourseId(), bean.getSubjectId())) {
					return false;
				}
				EnrollmentModel enrollmentModel = new EnrollmentModel();
				return enrollmentModel.isStudentEnrolledInCourse(bean.getStudentId(), bean.getCourseId());
			}
			if (roleId == RoleBean.PROGRAM_CHAIR) {
				ScopeAuthorizationUtil.DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
				if (scope == null || !scope.isValid() || !scope.getCourseIds().contains(bean.getCourseId())) {
					return false;
				}
				if (scope.getCollegeId() > 0) {
					if (bean.getStudentId() == null || bean.getStudentId() <= 0) {
						return false;
					}
					StudentModel studentModel = new StudentModel();
					StudentBean student = studentModel.findByPK(bean.getStudentId());
					if (student == null || student.getCollegeId() != scope.getCollegeId()) {
						return false;
					}
				}
				return true;
			}
			return false;
		} catch (Exception e) {
			log.error("Marksheet record read authorization failed closed", e);
			return false;
		}
	}

	/**
	 * Fail-closed row-level write/delete authorization check for a MarksheetBean record.
	 * Only ADMIN and allocated FACULTY may create, modify, or delete marks.
	 */
	public static boolean isAuthorizedToWriteMarksheet(UserBean user, MarksheetBean bean) {
		if (user == null || bean == null) {
			return false;
		}
		long roleId = user.getRoleId();
		if (roleId == RoleBean.ADMIN) {
			return true;
		}
		if (roleId == RoleBean.FACULTY) {
			return isAuthorizedToReadMarksheet(user, bean);
		}
		return false;
	}

	@Override
	protected void preload(HttpServletRequest request) {
		StudentModel studentModel = new StudentModel();
		CourseModel courseModel = new CourseModel();
		SubjectModel subjectModel = new SubjectModel();
		try {
			HttpSession session = request.getSession(false);
			UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;

			if (currentUser != null && currentUser.getRoleId() == RoleBean.FACULTY) {
				ScopeAuthorizationUtil.DataScope scope = ScopeAuthorizationUtil.resolveScope(currentUser);
				if (!scope.isValid()) {
					request.setAttribute("studentList", Collections.emptyList());
					request.setAttribute("courseList", Collections.emptyList());
					request.setAttribute("subjectList", Collections.emptyList());
					return;
				}
				StudentBean studentFilter = new StudentBean();
				studentFilter.setScopedCourseIds(scope.getCourseIds());
				request.setAttribute("studentList", studentModel.search(studentFilter, 0, 0));

				List<CourseBean> allCourses = courseModel.list();
				List<CourseBean> scopedCourses = new ArrayList<>();
				if (allCourses != null) {
					for (CourseBean c : allCourses) {
						if (scope.getCourseIds().contains(c.getId())) {
							scopedCourses.add(c);
						}
					}
				}
				request.setAttribute("courseList", scopedCourses);

				List<SubjectBean> allSubjects = subjectModel.list();
				List<SubjectBean> scopedSubjects = new ArrayList<>();
				if (allSubjects != null) {
					for (SubjectBean s : allSubjects) {
						if (scope.getSubjectIds().contains(s.getId())) {
							scopedSubjects.add(s);
						}
					}
				}
				request.setAttribute("subjectList", scopedSubjects);
				return;
			}

			List studentList = studentModel.list();
			request.setAttribute("studentList", studentList);

			List<CourseBean> courseList = courseModel.list();
			request.setAttribute("courseList", courseList);

			long selectedCourseId = DataUtility.getLong(request.getParameter("courseId"));
			List<SubjectBean> subjectList;
			if (selectedCourseId > 0) {
				subjectList = subjectModel.findByCourseId(selectedCourseId);
			} else {
				subjectList = subjectModel.list();
			}
			request.setAttribute("subjectList", subjectList);
		} catch (ApplicationException e) {
			log.error(e);
		}
	}

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("MarksheetCtl Method validate Started");

		boolean pass = true;

		if (DataValidator.isNull(request.getParameter("rollNo"))) {
			request.setAttribute("rollNo", PropertyReader.getValue("error.require", "Roll Number"));
			pass = false;
		}

		long studentId = DataUtility.getLong(request.getParameter("studentId"));
		if (studentId <= 0) {
			request.setAttribute("studentId", PropertyReader.getValue("error.require", "Student Name"));
			pass = false;
		}

		long courseId = DataUtility.getLong(request.getParameter("courseId"));
		if (courseId <= 0) {
			request.setAttribute("courseId", PropertyReader.getValue("error.require", "Course"));
			pass = false;
		}

		long subjectId = DataUtility.getLong(request.getParameter("subjectId"));
		if (subjectId <= 0) {
			request.setAttribute("subjectId", PropertyReader.getValue("error.require", "Subject"));
			pass = false;
		}

		String name = request.getParameter("name");
		if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
			request.setAttribute("name", PropertyReader.getValue("error.name", "Student Name"));
			pass = false;
		}

		String marksStr = request.getParameter("marks");
		if (DataValidator.isNull(marksStr)) {
			request.setAttribute("marks", PropertyReader.getValue("error.require", "Marks"));
			pass = false;
		} else if (!DataValidator.isInteger(marksStr)) {
			request.setAttribute("marks", PropertyReader.getValue("error.integer", "Marks"));
			pass = false;
		} else {
			int marks = DataUtility.getInt(marksStr);
			if (marks < 0) {
				request.setAttribute("marks", PropertyReader.getValue("error.marks.min"));
				pass = false;
			} else if (marks > 100) {
				request.setAttribute("marks", PropertyReader.getValue("error.marks.max"));
				pass = false;
			}
		}

		if (pass && courseId > 0 && subjectId > 0 && studentId > 0) {
			try {
				SubjectModel subjectModel = new SubjectModel();
				SubjectBean subject = subjectModel.findByPK(subjectId);
				if (subject == null || subject.getCourseId() != courseId) {
					request.setAttribute("subjectId", PropertyReader.getValue("error.subject.course.mismatch"));
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.subject.course.mismatch"), request);
					pass = false;
				}

				EnrollmentModel enrollmentModel = new EnrollmentModel();
				if (pass && !enrollmentModel.isStudentEnrolledInCourse(studentId, courseId)) {
					request.setAttribute("courseId", PropertyReader.getValue("error.enrollment.required"));
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.enrollment.required"), request);
					pass = false;
				} else if (pass && !enrollmentModel.verifyRollNoBelongsToStudent(studentId, courseId,
						request.getParameter("rollNo"))) {
					request.setAttribute("rollNo", PropertyReader.getValue("error.rollno.student.mismatch"));
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.rollno.student.mismatch"), request);
					pass = false;
				}
			} catch (Exception e) {
				log.error("Error validating course/subject/enrollment relationships", e);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.enrollment.required"), request);
				pass = false;
			}
		}

		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user != null && user.getRoleId() == RoleBean.FACULTY) {
			try {
				FacultyAllocationModel allocModel = new FacultyAllocationModel();
				if (!allocModel.isFacultyAllocatedByEmail(user.getLogin(), courseId, subjectId)) {
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
					pass = false;
				}
			} catch (Exception e) {
				log.error("Faculty grading scope check failed closed", e);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
				pass = false;
			}
		}

		log.debug("MarksheetCtl Method validate Ended");
		return pass;
	}

	@Override
	protected MarksheetBean populateBean(HttpServletRequest request) {
		log.debug("MarksheetCtl Method populatebean Started");

		MarksheetBean bean = new MarksheetBean();

		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setRollNo(DataUtility.getString(request.getParameter("rollNo")));
		bean.setName(DataUtility.getString(request.getParameter("name")));
		bean.setStudentId(DataUtility.getLong(request.getParameter("studentId")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		bean.setSubjectId(DataUtility.getLong(request.getParameter("subjectId")));

		String marksParam = request.getParameter("marks");
		if (DataValidator.isNotNull(marksParam) && DataValidator.isInteger(marksParam)) {
			bean.setMarks(DataUtility.getInt(marksParam));
		}

		HttpSession session = request.getSession(false);
		UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (currentUser != null && currentUser.getRoleId() == RoleBean.FACULTY) {
			bean.setActorFacultyEmail(currentUser.getLogin());
		}
		populateDTO(bean, request);

		log.debug("MarksheetCtl Method populatebean Ended");
		return bean;
	}

	/**
	 * Phase 2: Enforces row-level read authorization before placing any MarksheetBean
	 * in the request for ?id=... or ?rollNo=... lookups.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession sessCheck = request.getSession(false); if (sessCheck == null || sessCheck.getAttribute("user") == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;

		long id = DataUtility.getLong(request.getParameter("id"));
		String rollNo = DataUtility.getString(request.getParameter("rollNo"));

		MarksheetModel model = getModel();
		try {
			MarksheetBean bean = null;
			boolean lookupRequested = false;
			if (id > 0) {
				lookupRequested = true;
				bean = model.findByPK(id);
			} else if (DataValidator.isNotNull(rollNo)) {
				lookupRequested = true;
				bean = model.findByRollNo(rollNo);
			}

			if (lookupRequested) {
				if (bean == null || !isAuthorizedToReadMarksheet(currentUser, bean)) {
					// Return safe forbidden response without leaking private marks, roll number, or record existence
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
					ServletUtility.forwardPage(getView(), request, response);
					return;
				}
				ServletUtility.setBean(bean, request);
			}
		} catch (Exception e) {
			log.error("Direct marksheet read failed closed", e);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession sessCheck = request.getSession(false); if (sessCheck == null || sessCheck.getAttribute("user") == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (currentUser == null
				|| (currentUser.getRoleId() != RoleBean.ADMIN && currentUser.getRoleId() != RoleBean.FACULTY)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		long existingId = DataUtility.getLong(request.getParameter("id"));
		if (existingId > 0 && currentUser.getRoleId() == RoleBean.FACULTY) {
			try {
				MarksheetBean existing = getModel().findByPK(existingId);
				if (existing == null || !isAuthorizedToWriteMarksheet(currentUser, existing)) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
					ServletUtility.forwardPage(getView(), request, response);
					return;
				}
			} catch (Exception e) {
				log.error("Marksheet write authorization failed closed", e);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
		}

		super.doPost(request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected MarksheetModel getModel() {
		return new MarksheetModel();
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
			return ORSView.MARKSHEET_LIST_CTL;
		} else {
			return ORSView.MARKSHEET_VIEW;
		}
	}

}
