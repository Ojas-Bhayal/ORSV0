package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * Marksheet Merit List Controller (Phase 3 & Phase 5).
 * - Filters merit rankings and class averages by BOTH courseId and subjectId
 *   so different subjects (e.g., String vs Collections) are never ranked or
 *   averaged against one another.
 * - Restricts Student benchmark requests strictly to courses in which the
 *   student has an ACTIVE ST_ENROLLMENT record.
 * - Anonymizes peer names ("Anonymous Peer") and roll numbers ("HIDDEN") for
 *   Student viewers strictly using Student ID (never by display name).
 */
@WebServlet("/ctl/MarksheetMeritListCtl")
public class MarksheetMeritListCtl extends BaseListCtl<MarksheetBean, MarksheetModel> {

	private static Logger log = Logger.getLogger(MarksheetMeritListCtl.class);

	public static final String ANONYMOUS_PEER_NAME = "Anonymous Peer";
	public static final String HIDDEN_ROLL_NO = "HIDDEN";

	@Override
	protected void preload(HttpServletRequest request) {
		CourseModel courseModel = new CourseModel();
		SubjectModel subjectModel = new SubjectModel();
		try {
			List<CourseBean> allCourses = courseModel.list();
			List<SubjectBean> allSubjects = subjectModel.list();

			HttpSession session = request.getSession(false);
			UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;
			if (currentUser != null && currentUser.getRoleId() == RoleBean.STUDENT) {
				Set<Long> enrolledCourseIds = resolveEnrolledCourseIdsForStudent(currentUser);
				List<CourseBean> enrolledCourses = new ArrayList<>();
				if (allCourses != null) {
					for (CourseBean c : allCourses) {
						if (enrolledCourseIds.contains(c.getId())) {
							enrolledCourses.add(c);
						}
					}
				}
				List<SubjectBean> enrolledSubjects = new ArrayList<>();
				if (allSubjects != null) {
					for (SubjectBean s : allSubjects) {
						if (enrolledCourseIds.contains(s.getCourseId())) {
							enrolledSubjects.add(s);
						}
					}
				}
				request.setAttribute("courseList", enrolledCourses);
				request.setAttribute("subjectList", enrolledSubjects);
			} else {
				request.setAttribute("courseList", allCourses);
				request.setAttribute("subjectList", allSubjects);
			}
		} catch (ApplicationException e) {
			log.error(e);
		}
	}

	/**
	 * Resolves and validates the target courseId for a Student benchmark request
	 * (Spec 04: Student Benchmark and Active Enrollment):
	 * - If requestedCourseId <= 0, selects the first ACTIVE enrollment ordered by COURSE_ID ASC, ID ASC.
	 * - Never selects an INACTIVE enrollment.
	 * - If no active enrollment exists, or if an explicit requestedCourseId is not actively enrolled,
	 *   throws ApplicationException.
	 */
	public static long resolveAuthorizedStudentBenchmarkCourse(UserBean studentUser, long requestedCourseId)
			throws ApplicationException {
		if (studentUser == null || studentUser.getRoleId() != RoleBean.STUDENT
				|| com.sunilos.p4.util.DataValidator.isNull(studentUser.getLogin())) {
			throw new ApplicationException(PropertyReader.getValue("error.access.denied"));
		}
		StudentModel sModel = new StudentModel();
		StudentBean student = sModel.findByEmailId(studentUser.getLogin());
		if (student == null || student.getId() <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.enrollment.required"));
		}
		EnrollmentModel eModel = new EnrollmentModel();
		if (requestedCourseId <= 0) {
			List<EnrollmentBean> activeEnrollments = eModel.findActiveByStudentId(student.getId());
			if (activeEnrollments == null || activeEnrollments.isEmpty()) {
				throw new ApplicationException(PropertyReader.getValue("error.enrollment.required"));
			}
			return activeEnrollments.get(0).getCourseId();
		}
		if (!eModel.isStudentEnrolledInCourse(student.getId(), requestedCourseId)) {
			throw new ApplicationException(PropertyReader.getValue("error.enrollment.required"));
		}
		return requestedCourseId;
	}

	private static Set<Long> resolveEnrolledCourseIdsForStudent(UserBean studentUser) {
		Set<Long> courseIds = new HashSet<>();
		if (studentUser == null) {
			return courseIds;
		}
		try {
			StudentModel sModel = new StudentModel();
			StudentBean student = sModel.findByEmailId(studentUser.getLogin());
			if (student != null && student.getId() > 0) {
				EnrollmentModel eModel = new EnrollmentModel();
				List<EnrollmentBean> enrollments = eModel.findActiveByStudentId(student.getId());
				if (enrollments != null) {
					for (EnrollmentBean e : enrollments) {
						if (e.getCourseId() > 0) {
							courseIds.add(e.getCourseId());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to resolve student enrolled courses", e);
		}
		return courseIds;
	}

	/**
	 * Redacts peer student names and roll numbers strictly using the student's
	 * linked Student ID (Phase 3 Task 6 & Phase 5 Task 6: never matches by name).
	 */
	public static List<MarksheetBean> anonymizePeerRecordsByStudentId(long ownStudentId, List<MarksheetBean> list) {
		if (list == null) {
			return Collections.emptyList();
		}
		List<MarksheetBean> sanitized = new ArrayList<>(list.size());
		for (MarksheetBean bean : list) {
			boolean isSelf = (ownStudentId > 0 && bean.getStudentId() != null
					&& bean.getStudentId().longValue() == ownStudentId);
			if (!isSelf) {
				bean.setId(0L);
				bean.setStudentId(0L);
				bean.setName(ANONYMOUS_PEER_NAME);
				bean.setRollNo(HIDDEN_ROLL_NO);
				bean.setCreatedBy(null);
				bean.setModifiedBy(null);
			}
			sanitized.add(bean);
		}
		return sanitized;
	}

	/**
	 * Redacts peer student names and roll numbers for Student role viewers.
	 * Resolves ownStudentId via StudentModel.findByEmailId(currentUser.getLogin()).
	 */
	public static List<MarksheetBean> anonymizePeerRecordsForStudent(UserBean currentUser, List<MarksheetBean> list) {
		if (list == null || currentUser == null || currentUser.getRoleId() != RoleBean.STUDENT) {
			return list;
		}
		long ownStudentId = -1L;
		try {
			StudentModel sModel = new StudentModel();
			StudentBean student = sModel.findByEmailId(currentUser.getLogin());
			if (student != null && student.getId() > 0) {
				ownStudentId = student.getId();
			}
		} catch (Exception ignored) {
		}
		// Phase 7: Never fall back to login ST_USER.ID; if no linked StudentBean exists, ownStudentId remains -1 so all records are anonymized
		return anonymizePeerRecordsByStudentId(ownStudentId, list);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.debug("MarksheetMeritListCtl doPost Start");

		if (!checkSession(request)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		String op = DataUtility.getString(request.getParameter("operation"));
		if (OP_BACK.equalsIgnoreCase(op)) {
			ServletUtility.redirect(ORSView.WELCOME_CTL, request, response);
			return;
		}

		int pageNo = DataUtility.getInt(request.getParameter("pageNo"));
		int pageSize = DataUtility.getInt(request.getParameter("pageSize"));

		pageNo = (pageNo == 0) ? 1 : pageNo;
		pageSize = (pageSize == 0) ? DataUtility.getInt(PropertyReader.getValue("page.size")) : pageSize;

		long courseId = DataUtility.getLong(request.getParameter("courseId"));
		long subjectId = DataUtility.getLong(request.getParameter("subjectId"));

		HttpSession session = request.getSession(false);
		UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;

		// Phase 5 Task 5: Restrict student benchmark views to courses in which the student is actively enrolled
		if (currentUser != null && currentUser.getRoleId() == RoleBean.STUDENT) {
			try {
				StudentModel sModel = new StudentModel();
				StudentBean student = sModel.findByEmailId(currentUser.getLogin());
				EnrollmentModel eModel = new EnrollmentModel();
				if (student == null || student.getId() <= 0) {
					denyStudentBenchmark(request, response, pageNo, pageSize);
					return;
				}
				if (courseId <= 0) {
					// Spec 04: Filter strictly for ACTIVE enrollments so an inactive course is never selected as default
					List<EnrollmentBean> activeEnrollments = eModel.findActiveByStudentId(student.getId());
					if (activeEnrollments == null || activeEnrollments.isEmpty()) {
						denyStudentBenchmark(request, response, pageNo, pageSize);
						return;
					}
					courseId = activeEnrollments.get(0).getCourseId();
				} else if (!eModel.isStudentEnrolledInCourse(student.getId(), courseId)) {
					denyStudentBenchmark(request, response, pageNo, pageSize);
					return;
				}
			} catch (Exception e) {
				log.error("Student benchmark enrollment check failed closed", e);
				denyStudentBenchmark(request, response, pageNo, pageSize);
				return;
			}
		}

		// Spec 02 & Spec 05: Enforce Faculty and Program Chair scope on merit list queries
		if (currentUser != null && (currentUser.getRoleId() == RoleBean.FACULTY
				|| currentUser.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
			com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope scope = com.sunilos.p4.util.ScopeAuthorizationUtil
					.resolveScope(currentUser);
			if (scope == null || !scope.isValid()) {
				denyStudentBenchmark(request, response, pageNo, pageSize);
				return;
			}
			if (courseId <= 0 && !scope.getCourseIds().isEmpty()) {
				courseId = scope.getCourseIds().iterator().next();
			} else if (courseId > 0 && !scope.getCourseIds().contains(courseId)) {
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
				ServletUtility.setList(Collections.emptyList(), request);
				request.setAttribute("classAverage", 0.0);
				ServletUtility.setPageNo(pageNo, request);
				ServletUtility.setPageSize(pageSize, request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
			if (currentUser.getRoleId() == RoleBean.FACULTY && subjectId > 0
					&& !scope.getSubjectIds().contains(subjectId)) {
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
				ServletUtility.setList(Collections.emptyList(), request);
				request.setAttribute("classAverage", 0.0);
				ServletUtility.setPageNo(pageNo, request);
				ServletUtility.setPageSize(pageSize, request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
		}

		// Default courseId and subjectId to the first available subject of the selected course so String and Collections are never ranked together
		try {
			if (courseId <= 0) {
				CourseModel cModel = new CourseModel();
				List<CourseBean> courses = cModel.list();
				if (courses != null && !courses.isEmpty()) {
					courseId = courses.get(0).getId();
				}
			}
			SubjectModel subModel = new SubjectModel();
			if (subjectId > 0) {
				SubjectBean subj = subModel.findByPK(subjectId);
				if (subj == null || (courseId > 0 && subj.getCourseId() != courseId)) {
					// Phase 7: Reject invalid course/subject combinations instead of silently falling back
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.subject.course.mismatch"), request);
					ServletUtility.setList(Collections.emptyList(), request);
					request.setAttribute("selectedCourseId", courseId);
					request.setAttribute("selectedSubjectId", subjectId);
					request.setAttribute("classAverage", 0.0);
					ServletUtility.setPageNo(pageNo, request);
					ServletUtility.setPageSize(pageSize, request);
					ServletUtility.forwardPage(getView(), request, response);
					return;
				}
				if (courseId <= 0) {
					courseId = subj.getCourseId();
				}
			} else if (courseId > 0) {
				List<SubjectBean> courseSubjects = subModel.findByCourseId(courseId);
				if (courseSubjects != null && !courseSubjects.isEmpty()) {
					subjectId = courseSubjects.get(0).getId();
				}
			}
		} catch (Exception e) {
			log.warn("Could not resolve default course/subject for merit list: " + e.getMessage());
		}

		request.setAttribute("selectedCourseId", courseId);
		request.setAttribute("selectedSubjectId", subjectId);

		MarksheetModel model = getModel();
		List<MarksheetBean> list = model.getMeritListByCourseAndSubject(courseId, subjectId, pageNo, pageSize);
		list = anonymizePeerRecordsForStudent(currentUser, list);

		double classAvg = 0.0;
		if (courseId > 0 && subjectId > 0) {
			classAvg = model.getClassAverageForCourseAndSubject(courseId, subjectId);
		}
		request.setAttribute("classAverage", classAvg);

		ServletUtility.setList(list, request);

		if (list == null || list.isEmpty()) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("msg.no.record"), request);
		}

		ServletUtility.setPageNo(pageNo, request);
		ServletUtility.setPageSize(pageSize, request);
		ServletUtility.forwardPage(getView(), request, response);

		log.debug("MarksheetMeritListCtl doPost End");
	}

	private void denyStudentBenchmark(HttpServletRequest request, HttpServletResponse response, int pageNo,
			int pageSize) throws ServletException, IOException {
		ServletUtility.setErrorMessage(PropertyReader.getValue("error.enrollment.required"), request);
		ServletUtility.setList(Collections.emptyList(), request);
		request.setAttribute("classAverage", 0.0);
		ServletUtility.setPageNo(pageNo, request);
		ServletUtility.setPageSize(pageSize, request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.MARKSHEET_MERIT_LIST_VIEW;
	}

	@Override
	protected MarksheetModel getModel() {
		return new MarksheetModel();
	}
}
