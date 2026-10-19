package com.sunilos.p4.ctl;

import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CollegeModel;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

@WebServlet("/ctl/StudentCtl")
public class StudentCtl extends BaseCtl<StudentBean, StudentModel> {

	private static Logger log = Logger.getLogger(StudentCtl.class);

	@Override
	protected void preload(HttpServletRequest request) {
		CollegeModel model = new CollegeModel();
		CourseModel courseModel = new CourseModel();
		try {
			List l = model.list();
			request.setAttribute("collegeList", l);
			List courses = courseModel.list();
			request.setAttribute("courseList", courses);
		} catch (ApplicationException e) {
			log.error(e);
		}
	}

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;

		String firstName = request.getParameter("firstName");
		String lastName = request.getParameter("lastName");
		String mobileNo = request.getParameter("mobileNo");
		String email = request.getParameter("email");
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

		if (DataValidator.isNull(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.require", "Mobile No"));
			pass = false;
		} else if (!DataValidator.isPhoneNo(mobileNo)) {
			request.setAttribute("mobileNo", PropertyReader.getValue("error.phone", "Mobile No"));
			pass = false;
		}

		if (DataValidator.isNull(email)) {
			request.setAttribute("email", PropertyReader.getValue("error.require", "Email"));
			pass = false;
		} else if (!DataValidator.isEmail(email)) {
			request.setAttribute("email", PropertyReader.getValue("error.email", "Email"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("collegeId"))) {
			request.setAttribute("collegeId", PropertyReader.getValue("error.require", "College Name"));
			pass = false;
		}

		if (DataValidator.isNull(request.getParameter("courseId"))) {
			request.setAttribute("courseId", PropertyReader.getValue("error.require", "Course"));
			pass = false;
		}

		if (DataValidator.isNull(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.require", "Date Of Birth"));
			pass = false;
		} else if (!DataValidator.isDate(dob)) {
			request.setAttribute("dob", PropertyReader.getValue("error.date", "Date Of Birth"));
			pass = false;
		}

		return pass;
	}

	@Override
	protected StudentBean populateBean(HttpServletRequest request) {
		StudentBean bean = new StudentBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setFirstName(DataUtility.getString(request.getParameter("firstName")));
		bean.setLastName(DataUtility.getString(request.getParameter("lastName")));
		bean.setDob(DataUtility.getDate(request.getParameter("dob")));
		bean.setMobileNo(DataUtility.getString(request.getParameter("mobileNo")));
		bean.setEmail(DataUtility.getString(request.getParameter("email")));
		bean.setCollegeId(DataUtility.getLong(request.getParameter("collegeId")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected String getView() {
		return getView(null);
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
			return ORSView.STUDENT_LIST_CTL;
		} else {
			return ORSView.STUDENT_VIEW;
		}
	}

	@Override
	protected StudentModel getModel() {
		return new StudentModel();
	}

}
