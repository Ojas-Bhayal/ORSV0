package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.model.RoleModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

/**
 * Role functionality Controller. Enforces strict Name (letters only) validation
 * (Requirement 1).
 * 
 * @author Rays EdTech
 * @version 2.0
 * @Copyright (c) Rays EdTech
 */
@WebServlet("/ctl/RoleCtl")
public class RoleCtl extends BaseCtl<RoleBean, RoleModel> {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(RoleCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("RoleCtl Method validate Started");

		boolean pass = true;

		String name = request.getParameter("name");

		if (DataValidator.isNull(name)) {
			request.setAttribute("name", PropertyReader.getValue("error.require", "Name"));
			pass = false;
		} else if (!DataValidator.isName(name)) {
			request.setAttribute("name", PropertyReader.getValue("error.name", "Role Name"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("description"))) {
			request.setAttribute("description", PropertyReader.getValue("error.require", "Description"));
			pass = false;
		}

		log.debug("RoleCtl Method validate Ended");
		return pass;
	}

	@Override
	protected RoleBean populateBean(HttpServletRequest request) {

		log.debug("RoleCtl Method populatebean Started");

		RoleBean bean = new RoleBean();

		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setName(DataUtility.getString(request.getParameter("name")));
		bean.setDescription(DataUtility.getString(request.getParameter("description")));

		populateDTO(bean, request);

		log.debug("RoleCtl Method populatebean Ended");
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
			return ORSView.ROLE_LIST_CTL;
		} else {
			return ORSView.ROLE_VIEW;
		}
	}

	@Override
	protected RoleModel getModel() {
		return new RoleModel();
	}

}