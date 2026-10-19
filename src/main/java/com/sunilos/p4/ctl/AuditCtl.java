package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope;
import com.sunilos.p4.util.ServletUtility;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Program Chair Departmental Audit & Enrollment Analysis Controller.
 * Phase 6 Task 8: Uses controlled error handling around asynchronous
 * CompletableFuture audit queries so database failures produce controlled
 * application errors rather than unhandled exceptions.
 */
@WebServlet("/ctl/AuditCtl")
public class AuditCtl extends BaseListCtl<CourseBean, CourseModel> {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(AuditCtl.class);

	private static <T> CompletableFuture<List<T>> supplyListSafely(Supplier<List<T>> supplier,
			AtomicReference<Exception> firstError) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				List<T> res = supplier.get();
				return res != null ? res : Collections.emptyList();
			} catch (Exception e) {
				log.error("Controlled error in asynchronous audit query", e);
				firstError.compareAndSet(null, e);
				return Collections.emptyList();
			}
		});
	}

	@Override
	protected CourseBean populateBean(HttpServletRequest request) {
		CourseBean bean = new CourseBean();
		bean.setName(DataUtility.getString(request.getParameter("courseName")));
		return bean;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log.debug("AuditCtl doPost Started");

		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;

		if (!checkSession(request) || user == null
				|| (user.getRoleId() != RoleBean.PROGRAM_CHAIR && user.getRoleId() != RoleBean.ADMIN)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		// Phase 3 Task 5: Enforce Program Chair department/college scope
		DataScope chairScope = ScopeAuthorizationUtil.resolveScope(user);
		long requestedCollegeId = DataUtility.getLong(request.getParameter("collegeId"));
		long requestedCourseId = DataUtility.getLong(request.getParameter("courseId"));
		if (user.getRoleId() == RoleBean.PROGRAM_CHAIR) {
			// A department audit must have an authoritative college AND course scope.
			// Without both, fail closed rather than falling back to unscoped list queries.
			if (chairScope == null || !chairScope.isValid() || chairScope.getCollegeId() <= 0 || chairScope.getCourseIds().isEmpty()
					|| (requestedCollegeId > 0 && !chairScope.isCollegeAllowed(requestedCollegeId))
					|| (requestedCourseId > 0 && !chairScope.isCourseAllowed(requestedCourseId))) {
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
				ServletUtility.setList(Collections.emptyList(), request);
				ServletUtility.forwardPage(ORSView.AUDIT_VIEW, request, response);
				return;
			}
		}

		int pageNo = DataUtility.getInt(request.getParameter("pageNo"));
		int pageSize = DataUtility.getInt(request.getParameter("pageSize"));
		pageNo = (pageNo == 0) ? 1 : pageNo;
		pageSize = (pageSize == 0) ? DataUtility.getInt(PropertyReader.getValue("page.size")) : pageSize;

		final int finalPageNo = pageNo;
		final int finalPageSize = pageSize;

		CourseModel courseModel = new CourseModel();
		SubjectModel subjectModel = new SubjectModel();
		FacultyModel facultyModel = new FacultyModel();
		FacultyAllocationModel allocationModel = new FacultyAllocationModel();
		StudentModel studentModel = new StudentModel();
		EnrollmentModel enrollmentModel = new EnrollmentModel();
		MarksheetModel marksheetModel = new MarksheetModel();

		CourseBean courseBean = populateBean(request);
		FacultyBean facultyBean = new FacultyBean();
		StudentBean studentBean = new StudentBean();
		SubjectBean subjectBean = new SubjectBean();
		FacultyAllocationBean allocationBean = new FacultyAllocationBean();
		EnrollmentBean enrollmentBean = new EnrollmentBean();
		MarksheetBean meritBean = new MarksheetBean();
		if (user.getRoleId() == RoleBean.PROGRAM_CHAIR) {
			// Apply SQL-level scope to EVERY dashboard list; never use list() or an
			// unfiltered merit-list query for a Program Chair.
			courseBean.setScopedCourseIds(chairScope.getCourseIds());
			subjectBean.setScopedCourseIds(chairScope.getCourseIds());
			if (chairScope.getCollegeId() > 0) {
				facultyBean.setCollegeId(chairScope.getCollegeId());
				studentBean.setScopedCollegeId(chairScope.getCollegeId());
				allocationBean.setScopedCollegeId(chairScope.getCollegeId());
				enrollmentBean.setScopedCollegeId(chairScope.getCollegeId());
				meritBean.setScopedCollegeId(chairScope.getCollegeId());
			}
			studentBean.setScopedCourseIds(chairScope.getCourseIds());
			allocationBean.setScopedCourseIds(chairScope.getCourseIds());
			enrollmentBean.setScopedCourseIds(chairScope.getCourseIds());
			meritBean.setScopedCourseIds(chairScope.getCourseIds());
			if (!chairScope.getSubjectIds().isEmpty()) {
				meritBean.setScopedSubjectIds(chairScope.getSubjectIds());
			}
		}

		AtomicReference<Exception> asyncError = new AtomicReference<>();

		CompletableFuture<List<CourseBean>> courseFuture = supplyListSafely(
				() -> courseModel.search(courseBean), asyncError);
		CompletableFuture<List<SubjectBean>> subjectFuture = supplyListSafely(
				() -> subjectModel.search(subjectBean), asyncError);
		CompletableFuture<List<FacultyBean>> facultyFuture = supplyListSafely(
				() -> facultyModel.search(facultyBean), asyncError);
		CompletableFuture<List<FacultyAllocationBean>> allocationFuture = supplyListSafely(
				() -> allocationModel.search(allocationBean, 0, 0), asyncError);
		CompletableFuture<List<StudentBean>> studentFuture = supplyListSafely(
				() -> studentModel.search(studentBean), asyncError);
		CompletableFuture<List<EnrollmentBean>> enrollmentFuture = supplyListSafely(
				() -> enrollmentModel.search(enrollmentBean, 0, 0), asyncError);
		CompletableFuture<List<MarksheetBean>> meritFuture = supplyListSafely(
				() -> user.getRoleId() == RoleBean.PROGRAM_CHAIR
						? marksheetModel.search(meritBean, finalPageNo, finalPageSize)
						: marksheetModel.getMeritList(finalPageNo, finalPageSize), asyncError);

		try {
			CompletableFuture.allOf(courseFuture, subjectFuture, facultyFuture, allocationFuture, studentFuture,
					enrollmentFuture, meritFuture).join();
		} catch (Exception e) {
			log.error("Asynchronous audit execution interrupted", e);
			asyncError.compareAndSet(null, e);
		}

		if (asyncError.get() != null) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.audit.query"), request);
		}

		List<CourseBean> courseList = courseFuture.getNow(Collections.emptyList());
		List<SubjectBean> subjectList = subjectFuture.getNow(Collections.emptyList());
		List<FacultyBean> facultyList = facultyFuture.getNow(Collections.emptyList());
		List<FacultyAllocationBean> allocationList = allocationFuture.getNow(Collections.emptyList());
		List<StudentBean> studentList = studentFuture.getNow(Collections.emptyList());
		List<EnrollmentBean> enrollmentList = enrollmentFuture.getNow(Collections.emptyList());
		List<MarksheetBean> meritList = meritFuture.getNow(Collections.emptyList());

		Map<String, Integer> courseEnrollmentMap = new LinkedHashMap<>();
		Map<String, Double> courseAverageMap = new LinkedHashMap<>();
		Map<Long, Double> subjectAverageMap = new LinkedHashMap<>();
		boolean averageQueryFailed = false;
		if (courseList != null) {
			for (CourseBean c : courseList) {
				int headcount = 0;
				try {
					headcount = user.getRoleId() == RoleBean.PROGRAM_CHAIR
							? enrollmentModel.getEnrollmentCountByCourseAndCollege(c.getId(), chairScope.getCollegeId())
							: enrollmentModel.getEnrollmentCountByCourse(c.getId());
				} catch (ApplicationException e) {
					averageQueryFailed = true;
					log.error("Unable to calculate enrollment count for course ID " + c.getId(), e);
				}
				courseEnrollmentMap.put(c.getName(), headcount);
				try {
					double average = user.getRoleId() == RoleBean.PROGRAM_CHAIR
							? marksheetModel.getClassAverageForCourseAndCollege(c.getId(), chairScope.getCollegeId())
							: marksheetModel.getClassAverageForCourse(c.getId());
					courseAverageMap.put(c.getName(), average);
				} catch (ApplicationException e) {
					// A schema mismatch must not blank the entire Audit page. Keep the
					// course rows visible and show the benchmark as unavailable.
					averageQueryFailed = true;
					courseAverageMap.put(c.getName(), Double.NaN);
					log.error("Unable to calculate class average for course ID " + c.getId()
							+ ". Check ST_MARKSHEET/ST_SUBJECT/ST_ENROLLMENT schema and data.", e);
				}
			}
		}
		// The audit must expose per-subject benchmarks as required by Experiment 1.
		// Program Chairs are additionally restricted to their resolved college scope.
		if (subjectList != null) {
			for (SubjectBean subject : subjectList) {
				try {
					double average = user.getRoleId() == RoleBean.PROGRAM_CHAIR
							? marksheetModel.getClassAverageForCourseSubjectAndCollege(
									subject.getCourseId(), subject.getId(), chairScope.getCollegeId())
							: marksheetModel.getClassAverageForCourseAndSubject(subject.getCourseId(), subject.getId());
					subjectAverageMap.put(subject.getId(), average);
				} catch (ApplicationException e) {
					averageQueryFailed = true;
					subjectAverageMap.put(subject.getId(), Double.NaN);
					log.error("Unable to calculate class average for subject ID " + subject.getId(), e);
				}
			}
		}
		if (averageQueryFailed && asyncError.get() == null) {
			ServletUtility.setErrorMessage(
					"Some course or subject averages are unavailable because no eligible marks were found or a query failed. Check the Tomcat log for query errors.",
					request);
		}

		request.setAttribute("courseList", courseList);
		request.setAttribute("subjectList", subjectList);
		request.setAttribute("facultyList", facultyList);
		request.setAttribute("allocationList", allocationList);
		request.setAttribute("studentList", studentList);
		request.setAttribute("enrollmentList", enrollmentList);
		request.setAttribute("meritList", meritList);
		request.setAttribute("courseEnrollmentMap", courseEnrollmentMap);
		request.setAttribute("courseAverageMap", courseAverageMap);
		request.setAttribute("subjectAverageMap", subjectAverageMap);

		ServletUtility.setList(courseList, request);
		ServletUtility.setPageNo(pageNo, request);
		ServletUtility.setPageSize(pageSize, request);

		ServletUtility.forwardPage(ORSView.AUDIT_VIEW, request, response);
		log.debug("AuditCtl doPost Ended");
	}

	@Override
	protected String getView() {
		return ORSView.AUDIT_VIEW;
	}

	@Override
	protected String getView(String op) {
		return ORSView.AUDIT_VIEW;
	}

	@Override
	protected CourseModel getModel() {
		return new CourseModel();
	}
}
