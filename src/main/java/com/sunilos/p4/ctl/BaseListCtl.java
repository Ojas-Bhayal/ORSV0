package com.sunilos.p4.ctl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.BaseBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.BaseModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Base list controller class extended by all list pages in the application.
 * Security Hardening (Folder3 Audit P0 Fix #1):
 * - Prohibits delete operations over HTTP GET (returns HTTP 405 Method Not Allowed).
 * - Enforces CSRF token verification on state-changing delete requests.
 * - Enforces per-record deletion authorization via isDeleteAuthorizedForRecord(user, id)
 *   BEFORE any record is deleted. By default, only RoleBean.ADMIN is permitted to delete
 *   from list controllers; subclasses that permit scoped deletion (such as MarksheetListCtl)
 *   must verify record-level ownership/allocation for every target ID.
 */
public abstract class BaseListCtl<B extends BaseBean, M extends BaseModel> extends BaseCtl<B, M> {

	private static Logger log = Logger.getLogger(BaseListCtl.class);

	/**
	 * Verifies active user session (SE Experiment 6 Sequence Diagram message 3).
	 */
	protected boolean checkSession(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		return user != null;
	}

	protected UserBean getCurrentUser(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return (session != null) ? (UserBean) session.getAttribute("user") : null;
	}

	/**
	 * Per-record deletion authorization check (Folder3 Audit P0 Fix #1).
	 * Default policy: Only ADMIN may delete records from list controllers.
	 * Subclasses (e.g., MarksheetListCtl) may override to allow scoped deletion
	 * only after verifying the target record in the database.
	 */
	public boolean isDeleteAuthorizedForRecord(UserBean currentUser, long recordId) {
		if (currentUser == null || recordId <= 0) {
			return false;
		}
		return currentUser.getRoleId() == RoleBean.ADMIN;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String op = DataUtility.getString(request.getParameter("operation"));
		if (OP_DELETE.equalsIgnoreCase(op)) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.rest.method.not.allowed"), request);
			ServletUtility.forwardPage(getView(), request, response);
			return;
		}
		executeListSearch(request, response, op);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		log.debug("List doPost started");

		if (!checkSession(request)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		String op = DataUtility.getString(request.getParameter("operation"));
		M model = getModel();

		if (OP_DELETE.equalsIgnoreCase(op)) {
			if (!"POST".equalsIgnoreCase(request.getMethod())) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.rest.method.not.allowed"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}
			if (!verifyCsrfToken(request)) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
				ServletUtility.forwardPage(getView(), request, response);
				return;
			}

			UserBean currentUser = getCurrentUser(request);
			String[] ids = request.getParameterValues("ids");
			if (ids == null || ids.length == 0) {
				ServletUtility.setErrorMessage(PropertyReader.getValue("error.require", "Select at least one record"), request);
			} else {
				List<Long> parsedIds = new ArrayList<>(ids.length);
				for (String rawId : ids) {
					long targetId = DataUtility.getLong(rawId);
					if (targetId <= 0 || !isDeleteAuthorizedForRecord(currentUser, targetId)) {
						// Fail closed: do not delete any record if even one ID is unauthorized
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						ServletUtility.setErrorMessage(PropertyReader.getValue("error.scope.denied"), request);
						executeListSearch(request, response, null);
						return;
					}
					parsedIds.add(targetId);
				}
				for (Long targetId : parsedIds) {
					model.delete(targetId.intValue());
				}
			}
		}

		executeListSearch(request, response, op);
		log.debug("List doPost finish");
	}

	protected void executeListSearch(HttpServletRequest request, HttpServletResponse response, String op)
			throws ServletException, IOException {
		if (!checkSession(request)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.access.denied"), request);
			ServletUtility.forwardPage(ORSView.LOGIN_VIEW, request, response);
			return;
		}

		M model = getModel();
		int pageNo = DataUtility.getInt(request.getParameter("pageNo"));
		int pageSize = DataUtility.getInt(request.getParameter("pageSize"));

		pageNo = (pageNo == 0) ? 1 : pageNo;
		pageSize = (pageSize == 0) ? DataUtility.getInt(PropertyReader.getValue("page.size")) : pageSize;

		BaseBean bean = populateBean(request);

		if (DataValidator.isNotNull(op)) {
			switch (op) {
			case OP_SEARCH:
				pageNo = 1;
				break;
			case OP_NEXT:
				pageNo++;
				break;
			case OP_PREVIOUS:
				if (pageNo > 1)
					pageNo--;
				break;
			}
		}

		List<B> list = model.search(bean, pageNo, pageSize);
		List<B> nextList = model.search(bean, pageNo + 1, pageSize);

		ServletUtility.setList(list, request);
		request.setAttribute("nextList", nextList);

		if ((list == null || list.size() == 0) && DataValidator.isNull(ServletUtility.getErrorMessage(request))) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("msg.no.record"), request);
		}

		ServletUtility.setPageNo(pageNo, request);
		ServletUtility.setPageSize(pageSize, request);

		ServletUtility.forwardPage(getView(op), request, response);
	}

}
