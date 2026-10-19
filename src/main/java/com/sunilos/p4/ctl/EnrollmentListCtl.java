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
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope;
import com.sunilos.p4.util.ServletUtility;

/**
 * EnrollmentListCtl displays and filters Student-to-Course enrollments
 * (Folder3 Audit P0 Fix #2: Enforces SQL-level row scoping for Faculty and Program Chair
 * and restricts deletion strictly to Admin).
 */
@WebServlet("/ctl/EnrollmentListCtl")
public class EnrollmentListCtl extends BaseListCtl<EnrollmentBean, EnrollmentModel> {

	private static Logger log = Logger.getLogger(EnrollmentListCtl.class);

	@Override
	protected void preload(HttpServletRequest request) {
		try {
			CourseModel courseModel = new CourseModel();
			List<CourseBean> allCourses = courseModel.list();
			HttpSession session = request.getSession(false);
			UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
			if (user != null && (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
				DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
				List<CourseBean> scopedCourses = new ArrayList<>();
				if (scope != null && scope.isValid() && allCourses != null) {
					for (CourseBean c : allCourses) {
						if (scope.getCourseIds().contains(c.getId())) {
							scopedCourses.add(c);
						}
					}
				}
				request.setAttribute("courseList", scopedCourses);
			} else {
				request.setAttribute("courseList", allCourses);
			}
		} catch (Exception e) {
			log.error(e);
		}
	}

	private boolean applyRowScopeOrDeny(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null) {
			denyScope(request, response);
			return false;
		}
		if (user.getRoleId() == RoleBean.STUDENT) {
			denyScope(request, response);
			return false;
		}
		if (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR) {
			DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (scope == null || !scope.isValid()) {
				denyScope(request, response);
				return false;
			}
			long requestedCourseId = DataUtility.getLong(request.getParameter("courseId"));
			if (requestedCourseId > 0 && !scope.isCourseAllowed(requestedCourseId)) {
				denyScope(request, response);
				return false;
			}
			request.setAttribute("resolvedDataScope", scope);
		}
		return true;
	}

	private void denyScope(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
		ServletUtility.setList(Collections.emptyList(), request);
		request.setAttribute("nextList", Collections.emptyList());
		ServletUtility.setPageNo(1, request);
		ServletUtility.setPageSize(DataUtility.getInt(PropertyReader.getValue("page.size")), request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!applyRowScopeOrDeny(request, response)) {
			return;
		}
		super.doGet(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!applyRowScopeOrDeny(request, response)) {
			return;
		}
		super.doPost(request, response);
	}

	@Override
	protected EnrollmentBean populateBean(HttpServletRequest request) {
		EnrollmentBean bean = new EnrollmentBean();
		bean.setStudentName(DataUtility.getString(request.getParameter("studentName")));
		bean.setRollNo(DataUtility.getString(request.getParameter("rollNo")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		bean.setStatus(DataUtility.getString(request.getParameter("status")));

		DataScope scope = (DataScope) request.getAttribute("resolvedDataScope");
		if (scope == null) {
			UserBean user = getCurrentUser(request);
			if (user != null && (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
				scope = ScopeAuthorizationUtil.resolveScope(user);
			}
		}
		if (scope != null && scope.isValid()) {
			if (!scope.getCourseIds().isEmpty()) {
				bean.setScopedCourseIds(scope.getCourseIds());
			}
			if (scope.getRoleId() == RoleBean.PROGRAM_CHAIR && scope.getCollegeId() > 0) {
				bean.setScopedCollegeId(scope.getCollegeId());
			}
		}
		return bean;
	}

	@Override
	protected String getView() {
		return ORSView.ENROLLMENT_LIST_VIEW;
	}

	@Override
	protected String getView(String op) {
		if (OP_NEW.equalsIgnoreCase(op)) {
			return ORSView.ENROLLMENT_CTL;
		}
		return ORSView.ENROLLMENT_LIST_VIEW;
	}

	@Override
	protected EnrollmentModel getModel() {
		return new EnrollmentModel();
	}
}
