package com.sunilos.p4.ctl;

import java.io.IOException;

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

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ServletUtility;

/**
 * MarksheetAccessFilter (Spec 02: Marksheet Authorization and Student Benchmark Access).
 * Enforces the role policy matrix across marksheet routes:
 * - Student: Permitted on /ctl/GetMarksheetCtl (own marksheet only) and
 *   /ctl/MarksheetMeritListCtl (anonymized class benchmark for enrolled course/subject only).
 *   Denied on /ctl/MarksheetCtl (marks entry/edit) and /ctl/MarksheetListCtl.
 * - Faculty: Permitted on marksheet routes ONLY when holding an active allocation
 *   in ST_FACULTY_ALLOCATION (removing allocations immediately revokes access).
 * - Program Chair: Permitted on read/overview routes (/ctl/MarksheetListCtl,
 *   /ctl/GetMarksheetCtl, /ctl/MarksheetMeritListCtl, and GET /ctl/MarksheetCtl)
 *   within authorized department/college scope; denied on POST /ctl/MarksheetCtl.
 * - Admin: Permitted per administration policy.
 */
@WebFilter(urlPatterns = { "/ctl/MarksheetCtl", "/ctl/MarksheetListCtl", "/ctl/GetMarksheetCtl",
		"/ctl/MarksheetMeritListCtl" })
public class MarksheetAccessFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public static boolean isRouteAllowed(UserBean user, String uri, String method) {
		if (user == null || uri == null || UserModel.isLockedFlag(user.getLock())) {
			return false;
		}
		long roleId = user.getRoleId();
		if (roleId == RoleBean.ADMIN) {
			return true;
		}
		if (roleId == RoleBean.STUDENT) {
			if (uri.endsWith("/GetMarksheetCtl") || uri.endsWith("/MarksheetMeritListCtl")) {
				return true;
			}
			return false;
		}
		if (roleId == RoleBean.FACULTY) {
			ScopeAuthorizationUtil.DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (scope == null || !scope.isValid()) {
				return false;
			}
			return uri.endsWith("/MarksheetCtl") || uri.endsWith("/MarksheetListCtl")
					|| uri.endsWith("/GetMarksheetCtl") || uri.endsWith("/MarksheetMeritListCtl");
		}
		if (roleId == RoleBean.PROGRAM_CHAIR) {
			ScopeAuthorizationUtil.DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (scope == null || !scope.isValid()) {
				return false;
			}
			if (uri.endsWith("/MarksheetCtl")) {
				return false;
			}
			return uri.endsWith("/MarksheetListCtl") || uri.endsWith("/GetMarksheetCtl")
					|| uri.endsWith("/MarksheetMeritListCtl");
		}
		return false;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;

		if (user == null || UserModel.isLockedFlag(user.getLock())) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.session.expired"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		if (!isRouteAllowed(user, request.getRequestURI(), request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.WELCOME_VIEW, request, response);
			return;
		}

		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {
	}
}
