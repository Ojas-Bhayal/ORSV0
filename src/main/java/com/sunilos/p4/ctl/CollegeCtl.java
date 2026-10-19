package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.model.CollegeModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

/**
 * College functionality Controller. Enforces strict Name/State/City (letters only)
 * and Phone Number (10 digits starting with 6, 7, 8, or 9) validation
 * (Requirements 1 & 3).
 * 
 * @author Rays EdTech
 * @version 2.0
 * @Copyright (c) Rays EdTech
 */
@WebServlet("/ctl/CollegeCtl")
public class CollegeCtl extends BaseCtl<CollegeBean, CollegeModel> {

	private static final long serialVersionUID = 1L;

	private static Logger log = Logger.getLogger(CollegeCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {

		log.debug("CollegeCtl Method validate Started");

		boolean pass = true;

		String name = request.getParameter("name");
		String state = request.getParameter("state");
		String city = request.getParameter("city");
		String phoneNo = request.getParameter("phoneNo");

		if (DataValidator.isNull(name)) {
			request.setAttribute("name", PropertyReader.getValue("error.require", "Name"));
			pass = false;
		} else if (!DataValidator.isName(name)) {
			request.setAttribute("name", PropertyReader.getValue("error.name", "Name"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("address"))) {
			request.setAttribute("address", PropertyReader.getValue("error.require", "Address"));
			pass = false;
		}

		if (DataValidator.isNull(state)) {
			request.setAttribute("state", PropertyReader.getValue("error.require", "State"));
			pass = false;
		} else if (!DataValidator.isName(state)) {
			request.setAttribute("state", PropertyReader.getValue("error.name", "State"));
			pass = false;
		}

		if (DataValidator.isNull(city)) {
			request.setAttribute("city", PropertyReader.getValue("error.require", "City"));
			pass = false;
		} else if (!DataValidator.isName(city)) {
			request.setAttribute("city", PropertyReader.getValue("error.name", "City"));
			pass = false;
		}

		if (DataValidator.isNull(phoneNo)) {
			request.setAttribute("phoneNo", PropertyReader.getValue("error.require", "Phone No"));
			pass = false;
		} else if (!DataValidator.isPhoneNo(phoneNo)) {
			request.setAttribute("phoneNo", PropertyReader.getValue("error.phone", "Phone No"));
			pass = false;
		}

		log.debug("CollegeCtl Method validate Ended");
		return pass;
	}

	@Override
	protected CollegeBean populateBean(HttpServletRequest request) {

		log.debug("CollegeCtl Method populatebean Started");

		CollegeBean bean = new CollegeBean();

		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setName(DataUtility.getString(request.getParameter("name")));
		bean.setAddress(DataUtility.getString(request.getParameter("address")));
		bean.setState(DataUtility.getString(request.getParameter("state")));
		bean.setCity(DataUtility.getString(request.getParameter("city")));
		bean.setPhoneNo(DataUtility.getString(request.getParameter("phoneNo")));

		populateDTO(bean, request);

		log.debug("CollegeCtl Method populatebean Ended");
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
			return ORSView.COLLEGE_LIST_CTL;
		} else {
			return ORSView.COLLEGE_VIEW;
		}
	}

	@Override
	protected CollegeModel getModel() {
		return new CollegeModel();
	}

}
