package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * Get Marksheet by given roll number (Phase 3 & Phase 5).
 * - Enforces Student Ownership strictly by the authenticated user's linked
 *   StudentBean ID (never by display name) and requires an ACTIVE ST_ENROLLMENT
 *   for each returned course.
 * - Enforces fail-closed Faculty Course Scope via FacultyAllocationModel.
 */
@WebServlet("/ctl/GetMarksheetCtl")
public class GetMarksheetCtl extends BaseCtl<MarksheetBean, MarksheetModel> {

	private static Logger log = Logger.getLogger(GetMarksheetCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		if (DataValidator.isNull(request.getParameter("rollNo"))) {
			request.setAttribute("rollNo", PropertyReader.getValue("error.require", "Roll Number"));
			pass = false;
		}
		return pass;
	}

	/**
	 * Verifies whether all marksheet records belong to the given linked studentId
	 * (Phase 3 Tasks 5, 6, 7: Student ID authority, zero name-based matching,
	 * fail-closed default).
	 */
	public static boolean isOwnedByStudentId(long linkedStudentId, List<MarksheetBean> marksheets) {
		if (linkedStudentId <= 0 || marksheets == null || marksheets.isEmpty()) {
			return false;
		}
		for (MarksheetBean ms : marksheets) {
			if (ms == null || ms.getStudentId() == null || ms.getStudentId().longValue() != linkedStudentId) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Resolves the authenticated UserBean's linked StudentBean by login email and
	 * verifies Student ID ownership (fail-closed if lookup fails or throws).
	 */
	public static boolean isOwnedByStudent(UserBean user, List<MarksheetBean> marksheets) {
		if (user == null || DataValidator.isNull(user.getLogin()) || marksheets == null || marksheets.isEmpty()) {
			return false;
		}
		try {
			StudentModel sModel = new StudentModel();
			StudentBean student = sModel.findByEmailId(user.getLogin());
			if (student == null || student.getId() <= 0) {
				return false;
			}
			return isOwnedByStudentId(student.getId(), marksheets);
		} catch (Exception e) {
			log.error("Student ownership check failed closed", e);
			return false;
		}
	}

	/**
	 * Loads one course result per active enrollment using Student ID + Course ID.
	 * Roll numbers are display/search values only; they are not used as the
	 * authenticated student's identity.
	 */
	private List<MarksheetBean> loadActiveEnrollmentResults(StudentBean student) throws com.sunilos.p4.exception.ApplicationException {
		Map<Long, MarksheetBean> byCourse = new LinkedHashMap<>();
		if (student == null || student.getId() <= 0) {
			return new ArrayList<>();
		}
		EnrollmentModel enrollmentModel = new EnrollmentModel();
		List<EnrollmentBean> activeEnrollments = enrollmentModel.findActiveByStudentId(student.getId());
		if (activeEnrollments == null || activeEnrollments.isEmpty()) {
			return new ArrayList<>();
		}
		for (EnrollmentBean enrollment : activeEnrollments) {
			if (enrollment == null || enrollment.getCourseId() <= 0
					|| !"ACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
				continue;
			}
			List<MarksheetBean> courseResults = getModel().findCourseWiseMarksByStudentIdAndCourseId(
					student.getId(), enrollment.getCourseId());
			if (courseResults == null) {
				continue;
			}
			for (MarksheetBean result : courseResults) {
				if (result == null || result.getStudentId() == null
						|| result.getStudentId().longValue() != student.getId()
						|| result.getCourseId() != enrollment.getCourseId()) {
					continue;
				}
				if (DataValidator.isNotNull(enrollment.getRollNo())) {
					result.setRollNo(enrollment.getRollNo().trim());
				}
				byCourse.put(result.getCourseId(), result);
				break; // model returns one grouped result per course
			}
		}
		return new ArrayList<>(byCourse.values());
	}

	private void renderStudentResults(HttpServletRequest request, List<MarksheetBean> results) {
		if (results != null && !results.isEmpty()) {
			ServletUtility.setBean(results.get(0), request);
			request.setAttribute("courseMarksheetList", results);
		} else {
			request.removeAttribute("courseMarksheetList");
			ServletUtility.setErrorMessage(PropertyReader.getValue("msg.no.record"), request);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null || (user.getRoleId() != RoleBean.STUDENT && user.getRoleId() != RoleBean.ADMIN)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(user == null ? ORSView.LOGIN_VIEW : getView(), request, response);
			return;
		}

		if (user.getRoleId() == RoleBean.STUDENT) {
			try {
				StudentBean student = new StudentModel().findByEmailId(user.getLogin());
				if (student == null || student.getId() <= 0) {
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.student.ownership"), request);
				} else {
					List<EnrollmentBean> activeEnrollments = new EnrollmentModel().findActiveByStudentId(student.getId());
					if (activeEnrollments == null || activeEnrollments.isEmpty()) {
						ServletUtility.setErrorMessage(PropertyReader.getValue("error.enrollment.required"), request);
					} else {
						renderStudentResults(request, loadActiveEnrollmentResults(student));
					}
				}
			} catch (Exception e) {
				log.error("Unable to load marksheet results for authenticated student", e);
				ServletUtility.setErrorMessage("Unable to load results right now. Please retry or contact the administrator.", request);
			}
		}
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String rollNo = DataUtility.getString(request.getParameter("rollNo"));
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		if (user.getRoleId() == RoleBean.STUDENT) {
			try {
				StudentBean student = new StudentModel().findByEmailId(user.getLogin());
				if (student == null || student.getId() <= 0) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.student.ownership"), request);
					ServletUtility.forwardPage(getView(), request, response);
					return;
				}
				List<EnrollmentBean> activeEnrollments = new EnrollmentModel().findActiveByStudentId(student.getId());
				boolean ownActiveRoll = false;
				if (activeEnrollments != null) {
					for (EnrollmentBean enrollment : activeEnrollments) {
						if (enrollment != null && "ACTIVE".equalsIgnoreCase(enrollment.getStatus())
								&& DataValidator.isNotNull(enrollment.getRollNo())
								&& enrollment.getRollNo().trim().equalsIgnoreCase(rollNo.trim())) {
							ownActiveRoll = true;
							break;
						}
					}
				}
				if (!ownActiveRoll) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.rollno.student.mismatch"), request);
					ServletUtility.forwardPage(getView(), request, response);
					return;
				}
				List<MarksheetBean> results = loadActiveEnrollmentResults(student);
				if (results.isEmpty()) {
					ServletUtility.setErrorMessage(PropertyReader.getValue("msg.no.record"), request);
				} else {
					ServletUtility.setSuccessMessage("Showing results for your active enrollments.", request);
				}
				renderStudentResults(request, results);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			} catch (Exception e) {
				log.error("Student result search failed closed", e);
				ServletUtility.setErrorMessage("Unable to load results right now. Please retry or contact the administrator.", request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
		}

		// The generic roll-number search is for Admin only. Faculty/Program Chair use
		// allocation-scoped marks-entry and department-report workflows instead.
		if (user.getRoleId() != RoleBean.ADMIN) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		List<MarksheetBean> courseWiseMarksheets = getModel().findCourseWiseMarksByRollNo(rollNo);
		if (courseWiseMarksheets == null || courseWiseMarksheets.isEmpty()) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.rollno.notfound"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}
		ServletUtility.setBean(courseWiseMarksheets.get(0), request);
		request.setAttribute("courseMarksheetList", courseWiseMarksheets);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.GET_MARKSHEET_VIEW;
	}

	@Override
	protected MarksheetModel getModel() {
		return new MarksheetModel();
	}
}
