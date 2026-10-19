package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/ChangePasswordCtl")
public class ChangePasswordCtl extends BaseCtl<UserBean, UserModel> {

	public static final String OP_CHANGE_MY_PROFILE = "Change My Profile";

	private static Logger log = Logger.getLogger(ChangePasswordCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("ChangePasswordCtl Method validate Started");

		boolean pass = true;

		String oldPassword = request.getParameter("oldPassword");
		String newPassword = request.getParameter("newPassword");
		String confirmPassword = request.getParameter("confirmPassword");

		if (DataValidator.isNull(oldPassword)) {
			request.setAttribute("oldPassword", PropertyReader.getValue("error.require", "Old Password"));
			pass = false;
		} else if (!DataValidator.isPasswordLength(oldPassword)) {
			request.setAttribute("oldPassword", PropertyReader.getValue("error.password", "Old Password"));
			pass = false;
		}

		if (DataValidator.isNull(newPassword)) {
			request.setAttribute("newPassword", PropertyReader.getValue("error.require", "New Password"));
			pass = false;
		} else if (!DataValidator.isPasswordLength(newPassword)) {
			request.setAttribute("newPassword", PropertyReader.getValue("error.password", "New Password"));
			pass = false;
		}

		if (DataValidator.isNull(confirmPassword)) {
			request.setAttribute("confirmPassword", PropertyReader.getValue("error.require", "Confirm Password"));
			pass = false;
		} else if (!DataValidator.isPasswordLength(confirmPassword)) {
			request.setAttribute("confirmPassword", PropertyReader.getValue("error.password", "Confirm Password"));
			pass = false;
		}

		if (DataValidator.isNotNull(newPassword) && DataValidator.isNotNull(confirmPassword)
				&& !newPassword.equals(confirmPassword)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.password.mismatch"), request);
			pass = false;
		}

		log.debug("ChangePasswordCtl Method validate Ended");
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setPassword(DataUtility.getString(request.getParameter("oldPassword")));
		bean.setConfirmPassword(DataUtility.getString(request.getParameter("confirmPassword")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String op = DataUtility.getString(request.getParameter("operation"));
		String oldPassword = DataUtility.getString(request.getParameter("oldPassword"));
		String newPassword = DataUtility.getString(request.getParameter("newPassword"));
		String confirmPassword = DataUtility.getString(request.getParameter("confirmPassword"));

		UserBean bean = populateBean(request);

		if (!newPassword.equals(confirmPassword)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.password.mismatch"), request);
			ServletUtility.setBean(bean, request);
			ServletUtility.forwardPage(getView(op), request, response);
			return;
		}

		UserModel model = getModel();
		HttpSession session = request.getSession(true);
		UserBean userBean = (UserBean) session.getAttribute("user");
		long id = userBean.getId();

		try {
			boolean flag = model.changePassword(id, oldPassword, newPassword);
			if (flag) {
				bean = model.findByLogin(userBean.getLogin());
				session.setAttribute("user", bean);
				ServletUtility.setBean(bean, request);
				ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.password.changed"), request);
			}
		} catch (Exception e) {
			ServletUtility.setErrorMessage(e.getMessage(), request);
		}
		ServletUtility.forwardPage(getView(op), request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.CHANGE_PASSWORD_VIEW;
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

}
