package com.sunilos.p4.rest;

import jakarta.servlet.annotation.WebServlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

@WebServlet(urlPatterns = { "/rest/UserRestCtl", "/rest/UserRestCtl/*", })
public class UserRestCtl extends BaseRestController<UserBean, UserModel> {

	@Override
	protected void validateBean(UserBean bean) {
		if (DataValidator.isNull(bean.getFirstName()) || !DataValidator.isName(bean.getFirstName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "First Name"));
		}
		if (DataValidator.isNull(bean.getLastName()) || !DataValidator.isName(bean.getLastName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Last Name"));
		}
		if (DataValidator.isNull(bean.getLogin()) || !DataValidator.isEmail(bean.getLogin())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.email", "Login"));
		}
		if (DataValidator.isNull(bean.getPassword()) || !DataValidator.isPasswordLength(bean.getPassword())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.password", "Password"));
		}
		if (DataValidator.isNull(bean.getMobileNo()) || !DataValidator.isPhoneNo(bean.getMobileNo())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.phone", "Mobile No"));
		}
	}

	@Override
	public UserBean jsonToBean(JsonNode jsonNode, UserBean bean) {
		bean = super.jsonToBean(jsonNode, bean);
		String firstName = getJsonValue(jsonNode, "firstName");
		String lastName = getJsonValue(jsonNode, "lastName");
		String login = getJsonValue(jsonNode, "login");
		String password = getJsonValue(jsonNode, "password");
		String mobileNo = getJsonValue(jsonNode, "mobileNo");
		String gender = getJsonValue(jsonNode, "gender");

		if (DataValidator.isNotNull(firstName) && !DataValidator.isName(firstName)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "First Name"));
		}
		if (DataValidator.isNotNull(lastName) && !DataValidator.isName(lastName)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Last Name"));
		}
		if (DataValidator.isNotNull(login) && !DataValidator.isEmail(login)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.email", "Login"));
		}
		if (DataValidator.isNotNull(password) && !DataValidator.isPasswordLength(password)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.password", "Password"));
		}
		if (DataValidator.isNotNull(mobileNo) && !DataValidator.isPhoneNo(mobileNo)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.phone", "Mobile No"));
		}

		bean.setFirstName(firstName);
		bean.setLastName(lastName);
		bean.setLogin(login);
		bean.setPassword(password);
		bean.setMobileNo(mobileNo);
		bean.setGender(gender);
		return bean;
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

	@Override
	public UserBean getBean() {
		return new UserBean();
	}

}
