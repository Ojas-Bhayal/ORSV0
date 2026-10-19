package com.sunilos.p4.ctl;

import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.model.CollegeModel;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.model.FacultyModel;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;

@WebServlet("/ctl/FacultyCtl")
public class FacultyCtl extends BaseCtl<FacultyBean, FacultyModel> {

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(FacultyCtl.class);

    @Override
    protected void preload(HttpServletRequest request) {
        CollegeModel model = new CollegeModel();
        CourseModel courseModel = new CourseModel();
        SubjectModel subjectModel = new SubjectModel();
        try {
            List l = model.list();
            request.setAttribute("collegeList", l);
            List courses = courseModel.list();
            request.setAttribute("courseList", courses);
            List subjects = subjectModel.list();
            request.setAttribute("subjectList", subjects);
        } catch (ApplicationException e) {
            log.error(e);
        }
    }

    @Override
    protected boolean validate(HttpServletRequest request) {
        boolean pass = true;

        String firstName = request.getParameter("firstName");
        String lastName = request.getParameter("lastName");
        String email = request.getParameter("email");
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

        if (DataValidator.isNull(email)) {
            request.setAttribute("email", PropertyReader.getValue("error.require", "Email"));
            pass = false;
        } else if (!DataValidator.isEmail(email)) {
            request.setAttribute("email", PropertyReader.getValue("error.email", "Email"));
            pass = false;
        }

        if (DataValidator.isNull(mobileNo)) {
            request.setAttribute("mobileNo", PropertyReader.getValue("error.require", "Mobile No"));
            pass = false;
        } else if (!DataValidator.isPhoneNo(mobileNo)) {
            request.setAttribute("mobileNo", PropertyReader.getValue("error.phone", "Mobile No"));
            pass = false;
        }

        if (DataValidator.isNull(request.getParameter("address"))) {
            request.setAttribute("address", PropertyReader.getValue("error.require", "Address"));
            pass = false;
        }
        if (DataValidator.isNull(request.getParameter("collegeId"))) {
            request.setAttribute("collegeId", PropertyReader.getValue("error.require", "College"));
            pass = false;
        }
        if (DataValidator.isNull(request.getParameter("courseId"))) {
            request.setAttribute("courseId", PropertyReader.getValue("error.require", "Course"));
            pass = false;
        }
        if (DataValidator.isNull(request.getParameter("subjectId"))) {
            request.setAttribute("subjectId", PropertyReader.getValue("error.require", "Subject"));
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
    protected FacultyBean populateBean(HttpServletRequest request) {
        FacultyBean bean = new FacultyBean();
        bean.setId(DataUtility.getLong(request.getParameter("id")));
        bean.setCollegeId(DataUtility.getLong(request.getParameter("collegeId")));
        bean.setFirstName(DataUtility.getString(request.getParameter("firstName")));
        bean.setLastName(DataUtility.getString(request.getParameter("lastName")));
        bean.setEmail(DataUtility.getString(request.getParameter("email")));
        bean.setMobileNo(DataUtility.getString(request.getParameter("mobileNo")));
        bean.setAddress(DataUtility.getString(request.getParameter("address")));
        bean.setGender(DataUtility.getString(request.getParameter("gender")));
        bean.setDob(DataUtility.getDate(request.getParameter("dob")));
        bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
        bean.setSubjectId(DataUtility.getLong(request.getParameter("subjectId")));
        populateDTO(bean, request);
        return bean;
    }

    @Override
    protected String getView() { return getView(null); }

    @Override
    protected String getView(String op) {
        if (OP_CANCEL.equalsIgnoreCase(op) || OP_DELETE.equalsIgnoreCase(op)) {
            return ORSView.FACULTY_LIST_CTL;
        }
        return ORSView.FACULTY_VIEW;
    }

    @Override
    protected FacultyModel getModel() { return new FacultyModel(); }
}
