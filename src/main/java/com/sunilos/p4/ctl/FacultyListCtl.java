package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/FacultyListCtl")
public class FacultyListCtl extends BaseListCtl<FacultyBean, FacultyModel> {

	private static Logger log = Logger.getLogger(FacultyListCtl.class);

	private boolean applyRowScopeOrDeny(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null) {
			denyScope(request, response);
			return false;
		}
		if (user.getRoleId() == RoleBean.PROGRAM_CHAIR) {
			DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (scope == null || !scope.isValid()) {
				denyScope(request, response);
				return false;
			}
			long requestedCollegeId = DataUtility.getLong(request.getParameter("collegeId"));
			if (requestedCollegeId > 0 && !scope.isCollegeAllowed(requestedCollegeId)) {
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
	protected void executeListSearch(HttpServletRequest request, HttpServletResponse response, String op)
			throws ServletException, IOException {
		if (!checkSession(request)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		int pageNo = DataUtility.getInt(request.getParameter("pageNo"));
		int pageSize = DataUtility.getInt(request.getParameter("pageSize"));
		pageNo = (pageNo == 0) ? 1 : pageNo;
		pageSize = (pageSize == 0) ? DataUtility.getInt(PropertyReader.getValue("page.size")) : pageSize;
		if (DataValidator.isNotNull(op)) {
			switch (op) {
			case OP_SEARCH: pageNo = 1; break;
			case OP_NEXT: pageNo++; break;
			case OP_PREVIOUS: if (pageNo > 1) pageNo--; break;
			default: break;
			}
		}

		FacultyBean filter = populateBean(request);
		List<FacultyBean> facultyRows = getModel().search(filter, pageNo, pageSize);
		List<FacultyBean> nextRows = getModel().search(filter, pageNo + 1, pageSize);
		ServletUtility.setList(facultyRows, request);
		request.setAttribute("nextList", nextRows);
		if ((facultyRows == null || facultyRows.isEmpty()) && DataValidator.isNull(ServletUtility.getErrorMessage(request))) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("msg.no.record"), request);
		}
		ServletUtility.setPageNo(pageNo, request);
		ServletUtility.setPageSize(pageSize, request);

		Set<Long> visibleFacultyIds = new LinkedHashSet<>();
		if (facultyRows != null) {
			for (FacultyBean faculty : facultyRows) {
				if (faculty != null && faculty.getId() > 0) visibleFacultyIds.add(faculty.getId());
			}
		}
		List<FacultyAllocationBean> assignments = Collections.emptyList();
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user != null && (user.getRoleId() == RoleBean.ADMIN || user.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
			try {
				DataScope scope = (DataScope) request.getAttribute("resolvedDataScope");
				Set<Long> scopedCourseIds = (user.getRoleId() == RoleBean.PROGRAM_CHAIR && scope != null)
						? scope.getCourseIds() : null;
				Long scopedCollegeId = (user.getRoleId() == RoleBean.PROGRAM_CHAIR && scope != null)
						? Long.valueOf(scope.getCollegeId()) : null;
				assignments = new FacultyAllocationModel().findAllocationsForFacultyIds(
						visibleFacultyIds, scopedCourseIds, scopedCollegeId);
			} catch (Exception e) {
				log.error("Unable to load faculty teaching assignments for directory", e);
				ServletUtility.setErrorMessage("Faculty profiles loaded, but one or more teaching assignments could not be loaded.", request);
			}
		}
		request.setAttribute("facultyDirectoryAllocations", assignments);
		ServletUtility.forwardPage(getView(op), request, response);
	}

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String email = request.getParameter("email");
		if (DataValidator.isNotNull(firstName) && !DataValidator.isName(firstName)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "First Name"), request);
			pass = false;
		}
		if (DataValidator.isNotNull(lastName) && !DataValidator.isName(lastName)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "Last Name"), request);
			pass = false;
		}
		if (DataValidator.isNotNull(email) && !DataValidator.isEmail(email)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.email", "Email ID"), request);
			pass = false;
		}
		return pass;
	}

	@Override
	protected FacultyBean populateBean(HttpServletRequest request) {
		FacultyBean bean = new FacultyBean();
		bean.setFirstName(DataUtility.getString(request.getParameter("firstName")));
		bean.setLastName(DataUtility.getString(request.getParameter("lastName")));
		bean.setEmail(DataUtility.getString(request.getParameter("email")));
		bean.setCollegeId(DataUtility.getLong(request.getParameter("collegeId")));

		DataScope scope = (DataScope) request.getAttribute("resolvedDataScope");
		if (scope != null && scope.isValid() && scope.getRoleId() == RoleBean.PROGRAM_CHAIR
				&& scope.getCollegeId() > 0) {
			bean.setCollegeId(scope.getCollegeId());
		}
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.FACULTY_LIST_VIEW;
	}

	@Override
	protected FacultyModel getModel() {
		return new FacultyModel();
	}
}
