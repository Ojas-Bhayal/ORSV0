package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/UserRegistrationCtl")
public class UserRegistrationCtl extends BaseCtl<UserBean, UserModel> {

	public static final String OP_SIGN_UP = "SignUp";

	private static Logger log = Logger.getLogger(UserRegistrationCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("UserRegistrationCtl Method validate Started");

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

		if (DataValidator.isNotNull(password) && DataValidator.isNotNull(confirmPassword)
				&& !password.equals(confirmPassword)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.password.mismatch"), request);
			pass = false;
		}

		log.debug("UserRegistrationCtl Method validate Ended");
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setRoleId(RoleBean.STUDENT);
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
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		UserBean bean = populateBean(request);
		UserModel model = getModel();
		try {
			long pk = model.registerUser(bean);
			bean.setId(pk);
			ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.user.registered"), request);
		} catch (DuplicateRecordException e) {
			log.error(e);
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.login.duplicate"), request);
		}
		ServletUtility.setBean(bean, request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.USER_REGISTRATION_VIEW;
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

}