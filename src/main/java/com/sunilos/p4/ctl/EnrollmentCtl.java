package com.sunilos.p4.ctl;

import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.EnrollmentModel;
import com.sunilos.p4.model.StudentModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

/**
 * EnrollmentCtl manages Student-to-Course enrollments (Phase 6 Task 6).
 */
@WebServlet("/ctl/EnrollmentCtl")
public class EnrollmentCtl extends BaseCtl<EnrollmentBean, EnrollmentModel> {

	private static Logger log = Logger.getLogger(EnrollmentCtl.class);

	@Override
	protected void preload(HttpServletRequest request) {
		try {
			StudentModel studentModel = new StudentModel();
			CourseModel courseModel = new CourseModel();
			List<StudentBean> studentList = studentModel.list();
			List<CourseBean> courseList = courseModel.list();
			request.setAttribute("studentList", studentList);
			request.setAttribute("courseList", courseList);
		} catch (Exception e) {
			log.error("Error preloading students and courses for EnrollmentCtl", e);
		}
	}

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		if (DataUtility.getLong(request.getParameter("studentId")) <= 0) {
			request.setAttribute("studentId", PropertyReader.getValue("error.require", "Student"));
			pass = false;
		}
		if (DataUtility.getLong(request.getParameter("courseId")) <= 0) {
			request.setAttribute("courseId", PropertyReader.getValue("error.require", "Course"));
			pass = false;
		}
		if (DataValidator.isNull(request.getParameter("rollNo"))) {
			request.setAttribute("rollNo", PropertyReader.getValue("error.require", "Roll Number"));
			pass = false;
		}
		return pass;
	}

	@Override
	protected EnrollmentBean populateBean(HttpServletRequest request) {
		EnrollmentBean bean = new EnrollmentBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setStudentId(DataUtility.getLong(request.getParameter("studentId")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		bean.setRollNo(DataUtility.getString(request.getParameter("rollNo")));
		bean.setSemester(DataUtility.getString(request.getParameter("semester")));
		bean.setStatus(DataUtility.getString(request.getParameter("status")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected String getView() {
		return ORSView.ENROLLMENT_VIEW;
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op)) {
			return ORSView.ENROLLMENT_LIST_CTL;
		}
		return ORSView.ENROLLMENT_VIEW;
	}

	@Override
	protected EnrollmentModel getModel() {
		return new EnrollmentModel();
	}
}
