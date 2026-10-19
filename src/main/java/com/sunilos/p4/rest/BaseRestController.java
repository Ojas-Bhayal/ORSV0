package com.sunilos.p4.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunilos.p4.bean.BaseBean;
import com.sunilos.p4.bean.ORSResponse;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.ctl.FrontController;
import com.sunilos.p4.model.BaseModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;

/**
 * Secure BaseRestController with defense-in-depth authentication, RBAC
 * authorization, Bean validation, and prohibition of state-mutating deletes
 * over HTTP GET (Audit Critical Fix #1).
 * 
 * @author Sunil Sahu
 * @param <B>
 * @param <M>
 */
public abstract class BaseRestController<B extends BaseBean, M extends BaseModel<B>> extends HttpServlet {

	protected abstract M getModel();

	protected void sendResponse(ORSResponse res, HttpServletRequest request, HttpServletResponse response) {
		try {
			response.setContentType("application/json;charset=UTF-8");
			PrintWriter out = response.getWriter();
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(res);
			out.print(json);
			out.close();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Validates bean fields prior to save/add/update. Subclasses override to
	 * enforce strict DataValidator rules.
	 */
	protected void validateBean(B bean) {
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Defense-in-depth authentication & RBAC check
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.unauthorized")), request, response);
			return;
		}

		String uri = request.getRequestURI();
		if (!FrontController.isAuthorized(user.getRoleId(), uri, request.getMethod(), true)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.forbidden")), request, response);
			return;
		}

		String servPath = request.getServletPath();
		int startIndex = uri.indexOf(servPath);
		String urlPart = (startIndex >= 0 && uri.length() > startIndex + servPath.length() + 1)
				? uri.substring(startIndex + servPath.length() + 1)
				: "";
		String[] urlparts = urlPart.isEmpty() ? new String[] { "" } : urlPart.split("/");

		String operation = urlparts[0].toLowerCase();

		// Prohibit delete over HTTP GET
		if ("delete".equals(operation) && "GET".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.method.not.allowed")), request,
					response);
			return;
		}

		if (urlparts.length == 1 && "GET".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			sendResponse(new ORSResponse(false, "Invalid request"), request, response);
			return;
		}

		request.setAttribute("operation", operation);
		if (urlparts.length >= 2) {
			request.setAttribute("data", urlparts[1]);
		}
		super.service(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String operation = (String) request.getAttribute("operation");
		if ("delete".equalsIgnoreCase(operation)) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.method.not.allowed")), request,
					response);
			return;
		}

		long pk = DataUtility.getLong((String) request.getAttribute("data"));
		M model = getModel();
		B bean = model.findByPK(pk);

		if (bean == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("msg.no.record")), request, response);
			return;
		}

		if ("get".equals(operation)) {
			ORSResponse res = new ORSResponse(true);
			res.addData(bean);
			sendResponse(res, request, response);
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			sendResponse(new ORSResponse(false, operation + " is invalid"), request, response);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user == null || user.getRoleId() != RoleBean.ADMIN) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.forbidden")), request, response);
			return;
		}

		long pk = DataUtility.getLong((String) request.getAttribute("data"));
		M model = getModel();
		B bean = model.findByPK(pk);
		if (bean == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("msg.no.record")), request, response);
			return;
		}
		model.delete(bean);
		ORSResponse res = new ORSResponse(true, "Record is successfully deleted");
		res.addData(bean);
		sendResponse(res, request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String operation = (String) request.getAttribute("operation");
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;

		// Mutating operations (save, add, update, delete) require ADMIN role
		if (!"search".equals(operation) && (user == null || user.getRoleId() != RoleBean.ADMIN)) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			sendResponse(new ORSResponse(false, PropertyReader.getValue("error.rest.forbidden")), request, response);
			return;
		}

		M model = getModel();

		if ("delete".equals(operation)) {
			long pk = DataUtility.getLong((String) request.getAttribute("data"));
			B existing = model.findByPK(pk);
			if (existing == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				sendResponse(new ORSResponse(false, PropertyReader.getValue("msg.no.record")), request, response);
				return;
			}
			model.delete(existing);
			ORSResponse res = new ORSResponse(true, "Record is successfully deleted");
			res.addData(existing);
			sendResponse(res, request, response);
			return;
		}

		InputStream inputStream = request.getInputStream();
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			JsonNode jsonNode = objectMapper.readTree(inputStream);
			B bean = jsonToBean(jsonNode, getBean());

			if ("search".equals(operation)) {
				List<B> l = model.search(bean);
				ORSResponse res = new ORSResponse(true);
				res.addData(l);
				sendResponse(res, request, response);
				return;
			}

			validateBean(bean);

			if ("save".equals(operation)) {
				if (bean.getId() > 0) {
					model.update(bean);
				} else {
					long pk = model.add(bean);
					bean.setId(pk);
				}
			} else if ("add".equals(operation)) {
				long pk = model.add(bean);
				bean = model.findByPK(pk);
			} else if ("update".equals(operation)) {
				model.update(bean);
				bean = model.findByPK(bean.getId());
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				sendResponse(new ORSResponse(false, operation + " is invalid"), request, response);
				return;
			}

			ORSResponse res = new ORSResponse(true, PropertyReader.getValue("msg.data.saved"));
			res.addData(bean);
			sendResponse(res, request, response);
		} catch (IllegalArgumentException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			sendResponse(new ORSResponse(false, e.getMessage()), request, response);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			sendResponse(new ORSResponse(false, e.getMessage()), request, response);
		}
	}

	public B jsonToBean(JsonNode jsonNode, B bean) {
		if (jsonNode == null) {
			return bean;
		}
		bean.setId(DataUtility.getLong(getJsonValue(jsonNode, "id")));
		bean.setCreatedDatetime(new Timestamp(DataUtility.getLong(getJsonValue(jsonNode, "createdDatetime"))));
		bean.setModifiedDatetime(new Timestamp(DataUtility.getLong(getJsonValue(jsonNode, "modifiedDatetime"))));
		bean.setModifiedBy(getJsonValue(jsonNode, "modifiedBy"));
		bean.setCreatedBy(getJsonValue(jsonNode, "createdBy"));
		return bean;
	}

	public abstract B getBean();

	public String getJsonValue(JsonNode jsonNode, String key) {
		if (jsonNode == null) {
			return null;
		}
		JsonNode val = jsonNode.get(key);
		return (val != null && !val.isNull()) ? val.asText() : null;
	}

	public long getJsonLongValue(JsonNode jsonNode, String key) {
		if (jsonNode == null) {
			return 0;
		}
		JsonNode val = jsonNode.get(key);
		return (val != null && !val.isNull()) ? val.asLong() : 0;
	}

}
