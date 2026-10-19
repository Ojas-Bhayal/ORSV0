package com.sunilos.p4.ctl;

import java.io.IOException;
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
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.MarksheetModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope;
import com.sunilos.p4.util.ServletUtility;

/**
 * Marksheet List Controller (Phase 3 Row-Level SQL Scoping).
 * Enforces fail-closed row-level scoping in the SQL query itself for Faculty
 * (allocated courses and subjects) and Program Chair (authorized courses).
 */
@WebServlet("/ctl/MarksheetListCtl")
public class MarksheetListCtl extends BaseListCtl<MarksheetBean, MarksheetModel> {

	private static Logger log = Logger.getLogger(MarksheetListCtl.class);

	/** Only an administrator may delete a marksheet record. Grading permission is not delete permission. */
	@Override
	public boolean isDeleteAuthorizedForRecord(UserBean currentUser, long recordId) {
		if (currentUser == null || currentUser.getRoleId() != RoleBean.ADMIN || recordId <= 0) {
			return false;
		}
		try {
			return getModel().findByPK(recordId) != null;
		} catch (Exception e) {
			log.error("MarksheetListCtl delete authorization failed closed for ID " + recordId, e);
			return false;
		}
	}

	@Override
	protected void preload(HttpServletRequest request) {
		CourseModel courseModel = new CourseModel();
		try {
			List<CourseBean> courseList = courseModel.list();
			request.setAttribute("courseList", courseList);
		} catch (ApplicationException e) {
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
		if (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR) {
			DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (!scope.isValid()) {
				denyScope(request, response);
				return false;
			}
			long requestedCourseId = DataUtility.getLong(request.getParameter("courseId"));
			long requestedSubjectId = DataUtility.getLong(request.getParameter("subjectId"));
			if (requestedCourseId > 0 && !scope.isCourseAllowed(requestedCourseId)) {
				denyScope(request, response);
				return false;
			}
			if (user.getRoleId() == RoleBean.FACULTY && requestedSubjectId > 0
					&& !scope.isSubjectAllowed(requestedCourseId, requestedSubjectId)) {
				denyScope(request, response);
				return false;
			}
			request.setAttribute("resolvedDataScope", scope);
		}
		return true;
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

	private void denyScope(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
		ServletUtility.setList(Collections.emptyList(), request);
		request.setAttribute("nextList", Collections.emptyList());
		ServletUtility.setPageNo(1, request);
		ServletUtility.setPageSize(DataUtility.getInt(PropertyReader.getValue("page.size")), request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected MarksheetBean populateBean(HttpServletRequest request) {
		MarksheetBean bean = new MarksheetBean();
		bean.setRollNo(DataUtility.getString(request.getParameter("rollNo")));
		bean.setName(DataUtility.getString(request.getParameter("name")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		bean.setSubjectId(DataUtility.getLong(request.getParameter("subjectId")));

		DataScope scope = (DataScope) request.getAttribute("resolvedDataScope");
		if (scope != null && scope.isValid()) {
			if (!scope.getCourseIds().isEmpty()) {
				bean.setScopedCourseIds(scope.getCourseIds());
			}
			if (scope.getRoleId() == RoleBean.FACULTY && !scope.getSubjectIds().isEmpty()) {
				bean.setScopedSubjectIds(scope.getSubjectIds());
			}
			if (scope.getRoleId() == RoleBean.PROGRAM_CHAIR && scope.getCollegeId() > 0) {
				bean.setScopedCollegeId(scope.getCollegeId());
			}
		}
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_NEW.equals(op)) {
			return ORSView.MARKSHEET_CTL;
		}
		return ORSView.MARKSHEET_LIST_VIEW;
	}

	@Override
	protected MarksheetModel getModel() {
		return new MarksheetModel();
	}
}
