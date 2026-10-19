package com.sunilos.p4.ctl;

import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.FacultyAllocationModel;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

/**
 * FacultyAllocationCtl manages Faculty-to-Course/Subject teaching allocations
 * (Phase 6 Task 6).
 */
@WebServlet("/ctl/FacultyAllocationCtl")
public class FacultyAllocationCtl extends BaseCtl<FacultyAllocationBean, FacultyAllocationModel> {

	private static Logger log = Logger.getLogger(FacultyAllocationCtl.class);

	@Override
	protected void preload(HttpServletRequest request) {
		try {
			FacultyModel facultyModel = new FacultyModel();
			CourseModel courseModel = new CourseModel();
			SubjectModel subjectModel = new SubjectModel();
			List<FacultyBean> facultyList = facultyModel.list();
			List<CourseBean> courseList = courseModel.list();
			List<SubjectBean> subjectList = subjectModel.list();
			request.setAttribute("facultyList", facultyList);
			request.setAttribute("courseList", courseList);
			request.setAttribute("subjectList", subjectList);
		} catch (Exception e) {
			log.error("Error preloading lists for FacultyAllocationCtl", e);
		}
	}

	@Override
	protected boolean validate(HttpServletRequest request) {
		boolean pass = true;
		long facultyId = DataUtility.getLong(request.getParameter("facultyId"));
		long courseId = DataUtility.getLong(request.getParameter("courseId"));
		long subjectId = DataUtility.getLong(request.getParameter("subjectId"));

		if (facultyId <= 0) {
			request.setAttribute("facultyId", PropertyReader.getValue("error.require", "Faculty"));
			pass = false;
		}
		if (courseId <= 0) {
			request.setAttribute("courseId", PropertyReader.getValue("error.require", "Course"));
			pass = false;
		}
		if (subjectId <= 0) {
			request.setAttribute("subjectId", PropertyReader.getValue("error.require", "Subject"));
			pass = false;
		}
		if (pass) {
			try {
				SubjectModel subjectModel = new SubjectModel();
				SubjectBean subject = subjectModel.findByPK(subjectId);
				if (subject == null || subject.getCourseId() != courseId) {
					request.setAttribute("subjectId", PropertyReader.getValue("error.subject.course.mismatch"));
					ServletUtility.setErrorMessage(PropertyReader.getValue("error.subject.course.mismatch"), request);
					pass = false;
				}
			} catch (Exception e) {
				pass = false;
			}
		}
		return pass;
	}

	@Override
	protected FacultyAllocationBean populateBean(HttpServletRequest request) {
		FacultyAllocationBean bean = new FacultyAllocationBean();
		bean.setId(DataUtility.getLong(request.getParameter("id")));
		bean.setFacultyId(DataUtility.getLong(request.getParameter("facultyId")));
		bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
		bean.setSubjectId(DataUtility.getLong(request.getParameter("subjectId")));
		populateDTO(bean, request);
		return bean;
	}

	@Override
	protected String getView() {
		return ORSView.FACULTY_ALLOCATION_VIEW;
	}

	@Override
	protected String getView(String op) {
		if (OP_CANCEL.equalsIgnoreCase(op)) {
			return ORSView.FACULTY_ALLOCATION_LIST_CTL;
		}
		return ORSView.FACULTY_ALLOCATION_VIEW;
	}

	@Override
	protected FacultyAllocationModel getModel() {
		return new FacultyAllocationModel();
	}
}
