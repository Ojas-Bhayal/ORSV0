package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.model.CollegeModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/CollegeListCtl")
public class CollegeListCtl extends BaseListCtl<CollegeBean, CollegeModel> {

	private static Logger log = Logger.getLogger(CollegeListCtl.class);

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		String name = request.getParameter("name");
		String city = request.getParameter("city");
		if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "College Name"), request);
			pass = false;
		}
		if (DataValidator.isNotNull(city) && !DataValidator.isName(city)) {
			ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "City"), request);
			pass = false;
		}
		return pass;
	}

	@Override
	protected CollegeBean populateBean(HttpServletRequest request) {
		CollegeBean bean = new CollegeBean();
		bean.setName(DataUtility.getString(request.getParameter("name")));
		bean.setCity(DataUtility.getString(request.getParameter("city")));
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		return ORSView.COLLEGE_LIST_VIEW;
	}

	@Override
	protected CollegeModel getModel() {
		return new CollegeModel();
	}

}