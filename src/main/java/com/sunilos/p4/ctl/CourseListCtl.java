package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.model.CourseModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/CourseListCtl")
public class CourseListCtl extends BaseListCtl<CourseBean, CourseModel> {

    private static Logger log = Logger.getLogger(CourseListCtl.class);

    @Override
    protected boolean validate(HttpServletRequest request) {
        boolean pass = true;
        String name = request.getParameter("name");
        if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
            ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "Course Name"), request);
            pass = false;
        }
        return pass;
    }

    @Override
    protected CourseBean populateBean(HttpServletRequest request) {
        CourseBean bean = new CourseBean();
        bean.setName(DataUtility.getString(request.getParameter("name")));
        bean.setDescription(DataUtility.getString(request.getParameter("description")));
        bean.setDuration(DataUtility.getString(request.getParameter("duration")));
        com.sunilos.p4.bean.UserBean user = getCurrentUser(request);
        if (user != null && (user.getRoleId() == com.sunilos.p4.bean.RoleBean.FACULTY
                || user.getRoleId() == com.sunilos.p4.bean.RoleBean.PROGRAM_CHAIR)) {
            com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope scope = com.sunilos.p4.util.ScopeAuthorizationUtil.resolveScope(user);
            if (scope != null && scope.isValid() && !scope.getCourseIds().isEmpty()) {
                bean.setScopedCourseIds(scope.getCourseIds());
            }
        }
        return bean;
    }

    @Override
    protected String getView() {
        return getView(null);
    }

    @Override
    protected String getView(String op) {
        return ORSView.COURSE_LIST_VIEW;
    }

    @Override
    protected CourseModel getModel() {
        return new CourseModel();
    }

}
