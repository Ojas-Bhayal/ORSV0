package com.sunilos.p4.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunilos.p4.bean.ORSResponse;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ScopeAuthorizationUtil;
import com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope;

/**
 * StudentRestCtl enforces bean validation and SQL-level row scoping for
 * Faculty and Program Chair roles (Phase 3 Tasks 3, 4, 5, 10).
 */
@WebServlet(urlPatterns = { "/rest/studentctl/*" })
public class StudentRestCtl extends BaseRestController<StudentBean, StudentModel> {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;
		if (user != null && (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
			DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (!scope.isValid()) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				sendResponse(new ORSResponse(false, PropertyReader.getValue("error.scope.denied")), request, response);
				return;
			}
			long pk = DataUtility.getLong((String) request.getAttribute("data"));
			StudentBean target = getModel().findByPK(pk);
			if (target == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				sendResponse(new ORSResponse(false, PropertyReader.getValue("msg.no.record")), request, response);
				return;
			}
			EnrollmentModel enrollmentModel = new EnrollmentModel();
			boolean inCourseScope = scope.getCourseIds().isEmpty()
					|| enrollmentModel.isStudentEnrolledInAnyCourse(pk, scope.getCourseIds());
			boolean inCollegeScope = (user.getRoleId() != RoleBean.PROGRAM_CHAIR) || scope.getCollegeId() <= 0
					|| (target.getCollegeId() != null && target.getCollegeId().longValue() == scope.getCollegeId());
			if (!inCourseScope || !inCollegeScope) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				sendResponse(new ORSResponse(false, PropertyReader.getValue("error.scope.denied")), request, response);
				return;
			}
		}
		super.doGet(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String operation = (String) request.getAttribute("operation");
		HttpSession session = request.getSession(false);
		UserBean user = (session != null) ? (UserBean) session.getAttribute("user") : null;

		if ("search".equals(operation) && user != null
				&& (user.getRoleId() == RoleBean.FACULTY || user.getRoleId() == RoleBean.PROGRAM_CHAIR)) {
			DataScope scope = ScopeAuthorizationUtil.resolveScope(user);
			if (!scope.isValid()) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				sendResponse(new ORSResponse(false, PropertyReader.getValue("error.scope.denied")), request, response);
				return;
			}
			try (InputStream inputStream = request.getInputStream()) {
				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode jsonNode = objectMapper.readTree(inputStream);
				StudentBean searchBean = jsonToBean(jsonNode, getBean());
				if (searchBean.getCourseId() > 0 && !scope.isCourseAllowed(searchBean.getCourseId())) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					sendResponse(new ORSResponse(false, PropertyReader.getValue("error.scope.denied")), request,
							response);
					return;
				}
				if (user.getRoleId() == RoleBean.PROGRAM_CHAIR && searchBean.getCollegeId() != null
						&& searchBean.getCollegeId() > 0 && !scope.isCollegeAllowed(searchBean.getCollegeId())) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					sendResponse(new ORSResponse(false, PropertyReader.getValue("error.scope.denied")), request,
							response);
					return;
				}

				// Push row-level scoping directly into the SQL WHERE clause
				if (!scope.getCourseIds().isEmpty()) {
					searchBean.setScopedCourseIds(scope.getCourseIds());
				}
				if (user.getRoleId() == RoleBean.PROGRAM_CHAIR && scope.getCollegeId() > 0) {
					searchBean.setScopedCollegeId(scope.getCollegeId());
				}

				List<StudentBean> scopedResults = getModel().search(searchBean);
				ORSResponse res = new ORSResponse(true);
				res.addData(scopedResults);
				sendResponse(res, request, response);
				return;
			} catch (IllegalArgumentException e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				sendResponse(new ORSResponse(false, e.getMessage()), request, response);
				return;
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				sendResponse(new ORSResponse(false, e.getMessage()), request, response);
				return;
			}
		}

		super.doPost(request, response);
	}

	@Override
	protected void validateBean(StudentBean bean) {
		if (DataValidator.isNull(bean.getFirstName()) || !DataValidator.isName(bean.getFirstName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "First Name"));
		}
		if (DataValidator.isNull(bean.getLastName()) || !DataValidator.isName(bean.getLastName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Last Name"));
		}
		if (DataValidator.isNull(bean.getEmail()) || !DataValidator.isEmail(bean.getEmail())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.email", "Email"));
		}
		if (DataValidator.isNull(bean.getMobileNo()) || !DataValidator.isPhoneNo(bean.getMobileNo())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.phone", "Mobile No"));
		}
	}

	@Override
	public StudentBean jsonToBean(JsonNode jsonNode, StudentBean bean) {
		bean = super.jsonToBean(jsonNode, bean);
		String firstName = getJsonValue(jsonNode, "firstName");
		String lastName = getJsonValue(jsonNode, "lastName");
		String email = getJsonValue(jsonNode, "email");
		String mobileNo = getJsonValue(jsonNode, "mobileNo");

		if (DataValidator.isNotNull(firstName) && !DataValidator.isName(firstName)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "First Name"));
		}
		if (DataValidator.isNotNull(lastName) && !DataValidator.isName(lastName)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Last Name"));
		}
		if (DataValidator.isNotNull(email) && !DataValidator.isEmail(email)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.email", "Email"));
		}
		if (DataValidator.isNotNull(mobileNo) && !DataValidator.isPhoneNo(mobileNo)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.phone", "Mobile No"));
		}

		bean.setCollegeId(getJsonLongValue(jsonNode, "collegeId"));
		bean.setCollegeName(getJsonValue(jsonNode, "collegeName"));
		bean.setEmail(email);
		bean.setFirstName(firstName);
		bean.setLastName(lastName);
		bean.setMobileNo(mobileNo);
		bean.setCourseId(getJsonLongValue(jsonNode, "courseId"));
		return bean;
	}

	@Override
	protected StudentModel getModel() {
		return new StudentModel();
	}

	@Override
	public StudentBean getBean() {
		return new StudentBean();
	}
}
