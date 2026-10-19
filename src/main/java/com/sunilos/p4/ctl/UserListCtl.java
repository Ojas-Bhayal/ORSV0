package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/UserListCtl")
public class UserListCtl extends BaseListCtl<UserBean, UserModel> {

	private static Logger log = Logger.getLogger(UserListCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		String firstName = request.getParameter("firstName");
		String login = request.getParameter("login");
		if (DataValidator.isNotNull(firstName) && !DataValidator.isName(firstName)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "First Name"), request);
			pass = false;
		}
		if (DataValidator.isNotNull(login) && !DataValidator.isEmail(login)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.email", "Login ID"), request);
			pass = false;
		}
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
		bean.setGender(DataUtility.getString(request.getParameter("gender")));
		bean.setDob(DataUtility.getDate(request.getParameter("dob")));
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_NEW.equals(op)) {
			return ORSView.USER_CTL;
		} else {
			return ORSView.USER_LIST_VIEW;
		}
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

}
