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

@WebServlet("/ctl/MyProfileCtl")
public class MyProfileCtl extends BaseCtl<UserBean, UserModel> {

	public static final String OP_CHANGE_MY_PASSWORD = "ChangePassword";

	private static Logger log = Logger.getLogger(MyProfileCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("MyProfileCtl Method validate Started");

		boolean pass = true;

		String op = DataUtility.getString(request.getParameter("operation"));
		if (OP_CHANGE_MY_PASSWORD.equalsIgnoreCase(op) || op == null) {
			return pass;
		}

		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String login = request.getParameter("login");
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

		if (DataValidator.isNotNull(login) && !DataValidator.isEmail(login)) {
			request.setAttribute("login", PropertyReader.getValue("error.email", "Login"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("gender"))) {
			request.setAttribute("gender", PropertyReader.getValue("error.require", "Gender"));
			pass = false;
		}

		if (DataValidator.isNull(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.require", "Mobile No"));
			pass = false;
		} else if (!DataValidator.isPhoneNo(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.phone", "Mobile No"));
			pass = false;
		}

		if (DataValidator.isNull(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.require", "Date Of Birth"));
			pass = false;
		} else if (!DataValidator.isDate(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.date", "Date Of Birth"));
			pass = false;
		}

		log.debug("MyProfileCtl Method validate Ended");
		return pass;
	}

	@Override
	protected UserBean populateBean(HttpServletRequest request) {
		UserBean bean = new UserBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setLogin(DataUtility.getString(request.getParameter("login")));
		bean.setFirstName(DataUtility.getString(request.getParameter("firstName")));
		bean.setLastName(DataUtility.getString(request.getParameter("lastName")));
		bean.setMobileNo(DataUtility.getString(request.getParameter("mobileNo")));
		bean.setGender(DataUtility.getString(request.getParameter("gender")));
		bean.setDob(DataUtility.getDate(request.getParameter("dob")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession(true);
		UserBean userBean = (UserBean) session.getAttribute("user");

		long id = userBean.getId();
		UserModel model = getModel();
		UserBean bean = model.findByPK(id);

		ServletUtility.setBean(bean, request);
		ServletUtility.forwardPage(getView(null), request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String op = DataUtility.getString(request.getParameter("operation"));
		if (OP_CHANGE_MY_PASSWORD.equalsIgnoreCase(op)) {
			ServletUtility.redirect(ORSView.CHANGE_PASSWORD_CTL, request, response);
			return;
		}

		UserBean bean = populateBean(request);
		UserModel model = getModel();
		model.update(bean);
		HttpSession session = request.getSession(true);
		UserBean updated = model.findByPK(bean.getId());
		if (updated != null) {
			session.setAttribute("user", updated);
		}
		ServletUtility.setBean(bean, request);
		ServletUtility.setSuccessMessage(PropertyReader.getValue("msg.profile.saved"), request);
		ServletUtility.forwardPage(getView(), request, response);
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.MY_PROFILE_VIEW;
	}

	@Override
	protected UserModel getModel() {
		return new UserModel();
	}

}