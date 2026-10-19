package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.model.RoleModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/RoleListCtl")
public class RoleListCtl extends BaseListCtl<RoleBean, RoleModel> {

	private static Logger log = Logger.getLogger(RoleListCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		String name = request.getParameter("name");
		if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "Role Name"), request);
			pass = false;
		}
		return pass;
	}

	@Override
	protected RoleBean populateBean(HttpServletRequest request) {
		RoleBean bean = new RoleBean();
		bean.setName(DataUtility.getString(request.getParameter("name")));
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.ROLE_LIST_VIEW;
	}

	@Override
	protected RoleModel getModel() {
		return new RoleModel();
	}

}