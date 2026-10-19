package com.sunilos.p4.ctl;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunilos.p4.bean.ORSResponse;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * Main FrontController performs session checking, locked-account rejection, and
 * Role-Based Access Control (RBAC) enforcement across BOTH "/ctl/*" and "/rest/*"
 * routes (Phases 2 & 3).
 */
@WebFilter(urlPatterns = { "/ctl/*", "/rest/*" })
public class FrontController implements Filter {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public void init(FilterConfig conf) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		String uri = request.getRequestURI();
		boolean isRestRequest = uri.contains("/rest/");

		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;

		if (user == null || UserModel.isLockedFlag(user.getLock())) {
			if (session != null && user != null && UserModel.isLockedFlag(user.getLock())) {
				session.invalidate();
			}
			if (isRestRequest) {
				sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
						PropertyReader.getValue("error.rest.unauthorized"));
				return;
			}
			request.getSession(true);
			String expiredMsg = PropertyReader.getValue("error.session.expired");
			ServletUtility.setErrorMessage(expiredMsg, request);
			String queryParam = "?" + BaseCtl.MSG_ERROR + "=" + expiredMsg;
			ServletUtility.redirect(ORSView.LOGIN_CTL + queryParam, request, response);
			return;
		}

		// Enforce 10-minute session inactivity timeout (SE Experiment 2 Constraint 3)
		session.setMaxInactiveInterval(resolveSessionTimeoutSeconds());

		// Ensure session carries a cryptographic CSRF token for state-changing POST operations
		Object sessionCsrf = session.getAttribute("csrfToken");
		if (!(sessionCsrf instanceof String) || ((String) sessionCsrf).trim().isEmpty()) {
			session.setAttribute("csrfToken", com.sunilos.p4.util.PasswordUtility.generateResetToken());
		}

		// Enforce CSRF on all state-changing requests to session-authenticated routes.
		// Form submissions use the csrfToken parameter; JSON/API clients may use X-CSRF-Token.
		String method = request.getMethod();
		boolean stateChanging = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
				|| "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
		if (stateChanging && !BaseCtl.verifyCsrfToken(request)) {
			if (isRestRequest) {
				sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
						PropertyReader.getValue("error.csrf.invalid"));
			} else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.csrf.invalid"), request);
				ServletUtility.forwardPage(ORSView.WELCOME_VIEW, request, response);
			}
			return;
		}

		// Prohibit state-mutating deletes over HTTP GET across BOTH /ctl/* and /rest/* endpoints
		String opParam = request.getParameter("operation");
		if ("GET".equalsIgnoreCase(request.getMethod())
				&& (uri.toLowerCase().contains("/delete") || BaseCtl.OP_DELETE.equalsIgnoreCase(opParam))) {
			if (isRestRequest) {
				sendJsonError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
						PropertyReader.getValue("error.rest.method.not.allowed"));
			} else {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.rest.method.not.allowed"), request);
				ServletUtility.forwardPage(ORSView.WELCOME_VIEW, request, response);
			}
			return;
		}

		long roleId = user.getRoleId();

		if (!isAuthorized(roleId, uri, request.getMethod(), isRestRequest)) {
			if (isRestRequest) {
				sendJsonError(response, HttpServletResponse.SC_FORBIDDEN,
						PropertyReader.getValue("error.rest.forbidden"));
				return;
			}
			String accessDeniedMsg = PropertyReader.getValue("error.access.denied");
			ServletUtility.setErrorMessage(accessDeniedMsg, request);
			ServletUtility.forwardPage(ORSView.WELCOME_VIEW, request, response);
			return;
		}

		chain.doFilter(req, resp);
	}

	private int resolveSessionTimeoutSeconds() {
		try {
			int mins = Integer.parseInt(PropertyReader.getValue("session.timeout.minutes"));
			return (mins > 0 ? mins : 10) * 60;
		} catch (Exception e) {
			return 10 * 60;
		}
	}

	private void sendJsonError(HttpServletResponse response, int statusCode, String message) throws IOException {
		response.setStatus(statusCode);
		response.setContentType("application/json;charset=UTF-8");
		ORSResponse orsRes = new ORSResponse(false, message);
		PrintWriter out = response.getWriter();
		out.print(MAPPER.writeValueAsString(orsRes));
		out.flush();
	}

	/**
	 * Public alias for route-level role authorization (Spec 02: FrontControllerAccessTest compatibility).
	 */
	public static boolean isRoleAllowed(long roleId, String uri, String method, boolean isRestRequest) {
		return isAuthorized(roleId, uri, method, isRestRequest);
	}

	/**
	 * Public overload for route-level role authorization with default GET method.
	 */
	public static boolean isRoleAllowed(long roleId, String uri) {
		if (uri == null) {
			return false;
		}
		return isAuthorized(roleId, uri, "GET", uri.contains("/rest/"));
	}

	public static boolean isAuthorized(long roleId, String uri, String method, boolean isRestRequest) {
		if (roleId <= 0 || uri == null || uri.trim().isEmpty()) {
			return false;
		}
		if (roleId != RoleBean.ADMIN && roleId != RoleBean.STUDENT
				&& roleId != RoleBean.PROGRAM_CHAIR && roleId != RoleBean.FACULTY) {
			return false;
		}
		if (roleId == RoleBean.ADMIN) {
			return true;
		}

		if (isRestRequest) {
			String lowerUri = uri.toLowerCase();
			if (roleId == RoleBean.PROGRAM_CHAIR || roleId == RoleBean.FACULTY) {
				if (lowerUri.contains("/rest/studentctl/")
						&& (lowerUri.contains("/get/") || lowerUri.contains("/search"))) {
					return true;
				}
			}
			return false;
		}

		if (uri.endsWith("/MyProfileCtl") || uri.endsWith("/ChangePasswordCtl")) {
			return true;
		}
		// The generic roll-number results screen exposes private marks. Admins were
		// permitted above; students can use it for their own active enrollments.
		// Faculty and Program Chairs use scoped marks-entry/department-report views.
		if (uri.endsWith("/GetMarksheetCtl")) {
			return roleId == RoleBean.STUDENT;
		}
		if (uri.endsWith("/MarksheetMeritListCtl")) {
			return true; // controller enforces enrollment/allocation/department scope
		}

		if (roleId == RoleBean.PROGRAM_CHAIR) {
			return uri.endsWith("/AuditCtl")
					|| uri.endsWith("/FacultyListCtl")
					|| uri.endsWith("/FacultyAllocationListCtl")
					|| uri.endsWith("/CourseListCtl")
					|| uri.endsWith("/SubjectListCtl")
					|| uri.endsWith("/StudentListCtl")
					|| uri.endsWith("/EnrollmentListCtl")
					|| uri.endsWith("/MarksheetListCtl")
					|| uri.contains("ReportCtl");
		}

		if (roleId == RoleBean.FACULTY) {
			return uri.endsWith("/WelcomeCtl")
					|| uri.endsWith("/StudentListCtl")
					|| uri.endsWith("/EnrollmentListCtl")
					|| uri.endsWith("/MarksheetCtl")
					|| uri.endsWith("/MarksheetListCtl")
					|| uri.endsWith("/CourseListCtl")
					|| uri.endsWith("/SubjectListCtl");
		}

		return false;
	}

	@Override
	public void destroy() {
	}
}
