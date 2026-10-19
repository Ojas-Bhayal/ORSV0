package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.BaseBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.model.BaseModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.MessageSource;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * Base controller class of project. Loads all system messages from
 * PropertyReader (system.properties) per SE Experiment 2 Constraint 5.
 */
public abstract class BaseCtl<B extends BaseBean, M extends BaseModel> extends HttpServlet {

	public static final String OP_SAVE = "Save";
	public static final String OP_CANCEL = "Cancel";
	public static final String OP_DELETE = "Delete";
	public static final String OP_LIST = "List";
	public static final String OP_SEARCH = "Search";
	public static final String OP_VIEW = "View";
	public static final String OP_NEXT = "Next";
	public static final String OP_PREVIOUS = "Previous";
	public static final String OP_NEW = "New";
	public static final String OP_GO = "Go";
	public static final String OP_BACK = "Back";
	public static final String OP_LOG_OUT = "Logout";

	public static final String HAS_ERROR = "haserror";
	public static final String MESSAGE = "message";
	public static final String MSG_SUCCESS = "success";
	public static final String MSG_ERROR = "error";

	private static Logger log = Logger.getLogger(BaseCtl.class);

	/**
	 * Fail-closed CSRF validation. A request is rejected when the session token,
	 * submitted token, or session itself is missing. Accepts form parameter or
	 * X-CSRF-Token header and compares values in constant time.
	 */
	public static boolean verifyCsrfToken(HttpServletRequest request) {
		if (request == null) {
			return false;
		}
		jakarta.servlet.http.HttpSession session = request.getSession(false);
		if (session == null) {
			return false;
		}
		Object tokenAttribute = session.getAttribute("csrfToken");
		if (!(tokenAttribute instanceof String)) {
			return false;
		}
		String expected = ((String) tokenAttribute).trim();
		if (expected.isEmpty()) {
			return false;
		}
		String provided = request.getParameter("csrfToken");
		if (provided == null || provided.trim().isEmpty()) {
			provided = request.getHeader("X-CSRF-Token");
		}
		if (provided == null || provided.trim().isEmpty()) {
			return false;
		}
		return java.security.MessageDigest.isEqual(
				expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				provided.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	/** Creates an anonymous/session CSRF token on safe GET requests before rendering forms. */
	private void ensureCsrfToken(HttpServletRequest request) {
		if (request == null || !"GET".equalsIgnoreCase(request.getMethod())) {
			return;
		}
		// LoginCtl owns anonymous-session initialization and logout handling. Do not
		// create a session here and then have LoginCtl invalidate it during doGet;
		// that can send two JSESSIONID cookies and leave forms with stale CSRF tokens.
		if (this instanceof LoginCtl) {
			return;
		}
		jakarta.servlet.http.HttpSession session = request.getSession(true);
		ensureCsrfToken(session);
	}

	protected static void ensureCsrfToken(jakarta.servlet.http.HttpSession session) {
		if (session == null) {
			return;
		}
		Object token = session.getAttribute("csrfToken");
		if (!(token instanceof String) || ((String) token).trim().isEmpty()) {
			session.setAttribute("csrfToken", com.sunilos.p4.util.PasswordUtility.generateResetToken());
		}
	}

	/** Default policy for detail-controller deletion: authenticated Admin only. */
	protected boolean isDeleteAuthorized(HttpServletRequest request, UserBean currentUser, long recordId) {
		if (currentUser == null || currentUser.getRoleId() != RoleBean.ADMIN || recordId <= 0) {
			return false;
		}
		try {
			M model = getModel();
			return model != null && model.findByPK(recordId) != null;
		} catch (Exception e) {
			log.error("BaseCtl detail-delete authorization failed closed for ID " + recordId, e);
			return false;
		}
	}

	protected boolean validate(HttpServletRequest request) {
		return true;
	}

	protected void preload(HttpServletRequest request) {
	}

	protected B populateBean(HttpServletRequest request) {
		return null;
	}

	protected BaseBean populateDTO(BaseBean dto, HttpServletRequest request) {
		String createdBy = request.getParameter("createdBy");
		String modifiedBy = null;

		UserBean userbean = (UserBean) request.getSession().getAttribute("user");
		String defaultUser = PropertyReader.getValue("default.user");

		if (userbean == null) {
			createdBy = defaultUser;
			modifiedBy = defaultUser;
		} else {
			modifiedBy = userbean.getLogin();
			if ("null".equalsIgnoreCase(createdBy) || DataValidator.isNull(createdBy)) {
				createdBy = modifiedBy;
			}
		}
		dto.setCreatedBy(createdBy);
		dto.setModifiedBy(modifiedBy);

		long cdt = DataUtility.getLong(request.getParameter("createdDatetime"));
		if (cdt > 0) {
			dto.setCreatedDatetime(DataUtility.getTimestamp(cdt));
		} else {
			dto.setCreatedDatetime(DataUtility.getCurrentTimestamp());
		}
		dto.setModifiedDatetime(DataUtility.getCurrentTimestamp());

		return dto;
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		preload(request);
		getMessageSource(request);
		ensureCsrfToken(request);

		// All BaseCtl POSTs are protected, including save, reset, and delete actions.
		// LoginCtl creates an anonymous token on GET; authenticated /ctl routes also
		// enforce the token in FrontController for defense in depth.
		if ("POST".equalsIgnoreCase(request.getMethod()) && !verifyCsrfToken(request)) {
			// Diagnostic only: never log the token values themselves.
			jakarta.servlet.http.HttpSession csrfSession = request.getSession(false);
			Object expectedCsrf = (csrfSession == null) ? null : csrfSession.getAttribute("csrfToken");
			String submittedCsrf = request.getParameter("csrfToken");
			log.warn("CSRF rejected POST " + request.getRequestURI()
					+ "; sessionPresent=" + (csrfSession != null)
					+ "; expectedTokenPresent=" + (expectedCsrf instanceof String && !((String) expectedCsrf).trim().isEmpty())
					+ "; submittedTokenPresent=" + (submittedCsrf != null && !submittedCsrf.trim().isEmpty()));
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.csrf.invalid"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		String op = DataUtility.getString(request.getParameter("operation"));

		if (OP_CANCEL.equalsIgnoreCase(op)) {
			ServletUtility.redirect(getView(op), request, response);
			return;
		} else if (OP_DELETE.equalsIgnoreCase(op) && !(this instanceof BaseListCtl)) {
			if (!"POST".equalsIgnoreCase(request.getMethod())) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.rest.method.not.allowed"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
			// Defense in depth: deletion must have a valid CSRF token, a live session,
			// a real target ID, and explicit authorization before the model is called.
			if (!verifyCsrfToken(request)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.csrf.invalid"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
			jakarta.servlet.http.HttpSession session = request.getSession(false);
			UserBean currentUser = (session != null) ? (UserBean) session.getAttribute("user") : null;
			long recordId = DataUtility.getLong(request.getParameter("id"));
			if (!isDeleteAuthorized(request, currentUser, recordId)) {
				response.setStatus(currentUser == null ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_FORBIDDEN);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
			doDelete(request, response);
			return;
		}

		BaseBean bean = populateBean(request);

		if ("POST".equals(request.getMethod()) && !validate(request)) {
			ServletUtility.setBean(bean, request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}

		try {
			super.service(request, response);
		} catch (DuplicateRecordException | ApplicationException e) {
			ServletUtility.setBean(bean, request);
			ServletUtility.setErrorMessage(e.getMessage(), request);
			ServletUtility.forwardPage(getView(), request, response);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String op = DataUtility.getString(request.getParameter("operation"));
		long id = DataUtility.getLong(request.getParameter("id"));

		if (id > 0 || op != null) {
			M model = getModel();
			if (model != null && id > 0) {
				BaseBean bean = model.findByPK(id);
				ServletUtility.setBean(bean, request);
			}
		}

		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long id = DataUtility.getLong(request.getParameter("id"));
		B bean = populateBean(request);

		if (id > 0) {
			getModel().update(bean);
		} else {
			long pk = getModel().add(bean);
			bean.setId(pk);
		}
		ServletUtility.setBean(bean, request);
		ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.data.saved"), request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			BaseBean bean = populateBean(request);
			this.getModel().delete(bean);
			ServletUtility.redirect(getView(OP_DELETE), request, response);
		} catch (ApplicationException e) {
			ServletUtility.handleException(e, request, response);
		}
	}

	protected abstract String getView();

	protected abstract String getView(String op);

	protected abstract M getModel();

	public MessageSource getMessageSource(HttpServletRequest request) {
		return MessageSource.getInstance();
	}
}
