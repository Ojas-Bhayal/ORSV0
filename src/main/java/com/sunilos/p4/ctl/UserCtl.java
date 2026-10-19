package com.sunilos.p4.ctl;

import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.RoleModel;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/UserCtl")
public class UserCtl extends BaseCtl<UserBean, UserModel> {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(UserCtl.class);

	@Override
	protected void preload(HttpServletRequest request) {
		RoleModel model = new RoleModel();
		try {
			List l = model.list();
			request.setAttribute("roleList", l);
		} catch (ApplicationException e) {
			log.error(e);
		}
	}

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("UserCtl Method validate Started");

		boolean pass = true;

		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String login = request.getParameter("login");
		String password = request.getParameter("password");
		String confirmPassword = request.getParameter("confirmPassword");
		String mobileNo = request.getParameter("mobileNo");
		String dob = request.getParameter("dob");

		if (DataValidator.isNull(firstName)) {
			request.setAttribute("firstName", PropertyReader.getValue("error.require", "First Name"));
			pass = false;
		} else if (!DataValidator.isName(firstName)) {
			request.setAttribute("firstName", PropertyReader.getValue("error.name", "First Name"));
			pass = false;
		}

		if (DataValidator.isNull(lastName)) {
			request.setAttribute("lastName", PropertyReader.getValue("error.require", "Last Name"));
			pass = false;
		} else if (!DataValidator.isName(lastName)) {
			request.setAttribute("lastName", PropertyReader.getValue("error.name", "Last Name"));
			pass = false;
		}

		if (DataValidator.isNull(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.require", "Login Id"));
			pass = false;
		} else if (!DataValidator.isEmail(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.email", "Login"));
			pass = false;
		}

		// When editing an existing user, both password fields may be left blank to
		// retain the existing stored hash. If either field is supplied, require a
		// valid password and matching confirmation. New-user creation still requires both.
		boolean isUpdate = DataUtility.getLong(request.getParameter("id")) > 0;
		boolean passwordProvided = DataValidator.isNotNull(password);
		boolean confirmationProvided = DataValidator.isNotNull(confirmPassword);
		if (!isUpdate || passwordProvided || confirmationProvided) {
			if (DataValidator.isNull(password)) {
				request.setAttribute("password", PropertyReader.getValue("error.require", "Password"));
				pass = false;
			} else if (!DataValidator.isPasswordLength(password)) {
				request.setAttribute("password", PropertyReader.getValue("error.password", "Password"));
				pass = false;
			}

			if (DataValidator.isNull(confirmPassword)) {
				request.setAttribute("confirmPassword", PropertyReader.getValue("error.require", "Confirm Password"));
				pass = false;
			} else if (!DataValidator.isPasswordLength(confirmPassword)) {
				request.setAttribute("confirmPassword", PropertyReader.getValue("error.password", "Confirm Password"));
				pass = false;
			}
		}

		if (DataValidator.isNull(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.require", "Mobile No"));
			pass = false;
		} else if (!DataValidator.isPhoneNo(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.phone", "Mobile No"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("gender"))) {
			request.setAttribute("gender", PropertyReader.getValue("error.require", "Gender"));
			pass = false;
		}

		if (DataValidator.isNull(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.require", "Date Of Birth"));
			pass = false;
		} else if (!DataValidator.isDate(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.date", "Date Of Birth"));
			pass = false;
		}

		if ((DataValidator.isNotNull(password) || DataValidator.isNotNull(confirmPassword))
				&& DataValidator.isNotNull(password) && DataValidator.isNotNull(confirmPassword)
				&& !password.equals(confirmPassword)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.password.mismatch"), request);
			pass = false;
		}

		log.debug("UserCtl Method validate Ended");
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setRoleId(DataUtility.getLong(request.getParameter("roleId")));
		bean.setFirstName(DataUtility.getString(request.getParameter("firstName")));
		bean.setLastName(DataUtility.getString(request.getParameter("lastName")));
		bean.setLogin(DataUtility.getString(request.getParameter("login")));
		bean.setPassword(DataUtility.getString(request.getParameter("password")));
		bean.setConfirmPassword(DataUtility.getString(request.getParameter("confirmPassword")));
		bean.setMobileNo(DataUtility.getString(request.getParameter("mobileNo")));
		bean.setGender(DataUtility.getString(request.getParameter("gender")));
		bean.setDob(DataUtility.getDate(request.getParameter("dob")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
			return ORSView.USER_LIST_CTL;
		} else {
			return ORSView.USER_VIEW;
		}
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}
}
