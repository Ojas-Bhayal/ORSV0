package com.sunilos.p4.rest;

import jakarta.servlet.annotation.WebServlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.model.RoleModel;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

@WebServlet(urlPatterns = { "/rest/rolectl/*" })
public class RoleRestCtl extends BaseRestController<RoleBean, RoleModel> {

	@Override
	protected void validateBean(RoleBean bean) {
		if (DataValidator.isNull(bean.getName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.require", "Role Name"));
		}
		if (!DataValidator.isName(bean.getName())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Role Name"));
		}
		if (DataValidator.isNull(bean.getDescription())) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.require", "Description"));
		}
	}

	@Override
	public RoleBean jsonToBean(JsonNode jsonNode, RoleBean bean) {
		bean = super.jsonToBean(jsonNode, bean);
		String name = getJsonValue(jsonNode, "name");
		if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
			throw new IllegalArgumentException(PropertyReader.getValue("error.name", "Role Name"));
		}
		bean.setName(name);
		bean.setDescription(getJsonValue(jsonNode, "description"));
		return bean;
	}

	@Override
	protected RoleModel getModel() {
		return new RoleModel();
	}

	@Override
	public RoleBean getBean() {
		return new RoleBean();
	}

}
