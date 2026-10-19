package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.RecordNotFoundException;
import com.sunilos.p4.model.UserModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * ForgetPasswordCtl handles both generating a single-use expiring password
 * reset token (OP_GO) and consuming the reset token to set a new 8-character
 * password (OP_RESET_PASSWORD) (Phase 2 Task 6).
 */
@WebServlet("/ForgetPasswordCtl")
public class ForgetPasswordCtl extends BaseCtl<UserBean, UserModel> {

	public static final String OP_RESET_PASSWORD = "ResetPassword";
	private static Logger log = Logger.getLogger(ForgetPasswordCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		String op = DataUtility.getString(request.getParameter("operation"));
		String login = request.getParameter("login");

		if (DataValidator.isNull(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.require", "Email Id"));
			pass = false;
		} else if (!DataValidator.isEmail(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.email", "Login"));
			pass = false;
		}

		if (OP_RESET_PASSWORD.equalsIgnoreCase(op)) {
			String token = request.getParameter("resetToken");
			String newPassword = request.getParameter("newPassword");
			String confirmPassword = request.getParameter("confirmPassword");

			if (DataValidator.isNull(token)) {
				request.setAttribute("resetToken", PropertyReader.getValue("error.require", "Reset Token"));
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
			} else if (newPassword != null && !newPassword.equals(confirmPassword)) {
				request.setAttribute("confirmPassword", PropertyReader.getValue("error.password.mismatch"));
				pass = false;
			}
		}
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setLogin(DataUtility.getString(request.getParameter("login")));
		return bean;
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String op = DataUtility.getString(request.getParameter("operation"));
		UserBean bean = populateBean(request);
		UserModel model = getModel();

		try {
			if (OP_RESET_PASSWORD.equalsIgnoreCase(op)) {
				String token = DataUtility.getString(request.getParameter("resetToken"));
				String newPassword = DataUtility.getString(request.getParameter("newPassword"));
				model.consumePasswordResetToken(bean.getLogin(), token, newPassword);
				ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.password.reset.complete"), request);
			} else {
				model.requestPasswordResetNonEnumerating(bean.getLogin());
				ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.password.sent"), request);
			}
		} catch (RecordNotFoundException | ApplicationException e) {
			ServletUtility.setErrorMessage(e.getMessage(), request);
			log.error(e);
		}

		ServletUtility.setBean(bean, request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected String getView() {
		return ORSView.FORGET_PASSWORD_VIEW;
	}

	@Override
	protected String getView(String op) {
		return ORSView.FORGET_PASSWORD_VIEW;
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}
}
