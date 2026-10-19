package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.RoleModel;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.MessageSource;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * Login functionality Controller (Phase 2 Security Remediation).
 * - Rejects locked accounts (UserBean.ACTIVE) during login
 * - Rotates HTTP session ID upon successful authentication to prevent session fixation
 * - Enforces 10-minute session inactivity timeout (SE Experiment 2 Constraint 3)
 * - Enforces Email and 8-character password validation
 */
@WebServlet("/LoginCtl")
public class LoginCtl extends BaseCtl<UserBean, UserModel> {

	private static final long serialVersionUID = 1L;
	public static final String OP_REGISTER = "Register";
	public static final String OP_SIGN_IN = "SignIn";
	public static final String OP_SIGN_UP = "SignUp";
	public static final String OP_LOG_OUT = "logout";

	private static Logger log = Logger.getLogger(LoginCtl.class);

	/**
	 * Rotates the HTTP session upon login: invalidates any prior pre-login
	 * session, creates a new session, invokes request.changeSessionId(), and sets
	 * the 10-minute inactivity timeout.
	 */
	public static HttpSession rotateSessionOnLogin(HttpServletRequest request) {
		HttpSession existing = request.getSession(false);
		if (existing != null) {
			try {
				existing.invalidate();
			} catch (IllegalStateException ignored) {
			}
		}
		HttpSession newSession = request.getSession(true);
		try {
			request.changeSessionId();
		} catch (Exception ignored) {
		}
		newSession.setMaxInactiveInterval(10 * 60);
		newSession.setAttribute("csrfToken", com.sunilos.p4.util.PasswordUtility.generateResetToken());
		return newSession;
	}

	@Override
	protected boolean validate(HttpServletRequest request) {
		// Logout is a dedicated CSRF-protected POST action and has no login fields.
		if (OP_LOG_OUT.equalsIgnoreCase(DataUtility.getString(request.getParameter("operation")))) {
			return true;
		}
		log.debug("LoginCtl Method validate Started");

		MessageSource ms = getMessageSource(request);
		boolean pass = true;

		String login = request.getParameter("login");
		String password = request.getParameter("password");

		if (DataValidator.isNull(login)) {
			request.setAttribute("login", ms.get("valid.required"));
			pass = false;
		} else if (!DataValidator.isEmail(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.email", "Login"));
			pass = false;
		}

		if (DataValidator.isNull(password)) {
			request.setAttribute("password", ms.get("valid.required"));
			pass = false;
		} else if (!DataValidator.isPasswordLength(password)) {
			request.setAttribute("password", PropertyReader.getValue("error.password", "Password"));
			pass = false;
		}

		log.debug("LoginCtl Method validate Ended");
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setLogin(DataUtility.getString(request.getParameter("login")));
		bean.setPassword(DataUtility.getString(request.getParameter("password")));
		log.debug("LoginCtl Method populatebean Ended");
		return bean;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String operation = DataUtility.getString(request.getParameter("operation"));

		// Logout must not mutate the session through GET. The header submits logout
		// as a CSRF-protected POST instead.
		if (OP_LOG_OUT.equalsIgnoreCase(operation)) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setHeader("Allow", "POST");
			response.getWriter().write("Logout requires a CSRF-protected POST request.");
			return;
		}

		HttpSession loginSession = request.getSession(true);
		loginSession.setMaxInactiveInterval(10 * 60);
		// Preserve an existing anonymous session/token while navigating between public
		// form pages; rotate the session only after successful authentication.
		BaseCtl.ensureCsrfToken(loginSession);

		if (loginSession.getAttribute("user") != null) {
			ServletUtility.redirect(ORSView.WELCOME_CTL, request, response);
			return;
		}
		super.doGet(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String operation = DataUtility.getString(request.getParameter("operation"));
		if (OP_LOG_OUT.equalsIgnoreCase(operation)) {
			HttpSession existing = request.getSession(false);
			if (existing != null) {
				try { existing.invalidate(); } catch (IllegalStateException ignored) { }
			}
			HttpSession anonymous = request.getSession(true);
			anonymous.setMaxInactiveInterval(10 * 60);
			BaseCtl.ensureCsrfToken(anonymous);
			ServletUtility.redirect(ORSView.LOGIN_CTL, request, response);
			return;
		}

		log.debug("LoginCtl Method doPost Started");

		MessageSource ms = getMessageSource(request);

		String login = DataUtility.getString(request.getParameter("login"));
		String password = DataUtility.getString(request.getParameter("password"));

		UserModel model = getModel();

		// Phase 2 Task 7: Check if account is locked before/during authentication
		UserBean existingUser = model.findByLogin(login);
		if (existingUser != null && UserModel.isLockedFlag(existingUser.getLock())) {
			UserBean formBean = populateBean(request);
			ServletUtility.setBean(formBean, request);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.account.locked"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		UserBean bean = model.authenticate(login, password);

		if (bean == null) {
			bean = populateBean(request);
			ServletUtility.setBean(bean, request);
			ServletUtility.setErrorMessage(ms.get("login.error"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		// Phase 2 Task 8: Rotate session ID after successful login
		HttpSession session = rotateSessionOnLogin(request);
		session.setAttribute("user", bean);

		long roleId = bean.getRoleId();
		RoleModel role = new RoleModel();
		RoleBean rolebean = role.findByPK(roleId);
		if (rolebean != null) {
			session.setAttribute("role", rolebean.getName());
		} else {
			session.setAttribute("role", "User");
		}

		ServletUtility.forwardPage(ORSView.WELCOME_VIEW, request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

	@Override
	protected String getView(String op) {
		return ORSView.LOGIN_VIEW;
	}
}
