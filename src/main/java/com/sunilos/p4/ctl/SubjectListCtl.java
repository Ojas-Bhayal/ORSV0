package com.sunilos.p4.ctl;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.model.SubjectModel;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.PropertyReader;
import com.sunilos.p4.util.ServletUtility;

@WebServlet("/ctl/SubjectListCtl")
public class SubjectListCtl extends BaseListCtl<SubjectBean, SubjectModel> {

    private static Logger log = Logger.getLogger(SubjectListCtl.class);

    @Override
    protected boolean validate(HttpServletRequest request) {
        boolean pass = true;
        String name = request.getParameter("name");
        if (DataValidator.isNotNull(name) && !DataValidator.isName(name)) {
            ServletUtility.setErrorMessage(PropertyReader.getValue("error.name", "Subject Name"), request);
            pass = false;
        }
        return pass;
    }

    @Override
    protected SubjectBean populateBean(HttpServletRequest request) {
        SubjectBean bean = new SubjectBean();
        bean.setName(DataUtility.getString(request.getParameter("name")));
        bean.setDescription(DataUtility.getString(request.getParameter("description")));
        bean.setCourseId(DataUtility.getLong(request.getParameter("courseId")));
        com.sunilos.p4.bean.UserBean user = getCurrentUser(request);
        if (user != null && (user.getRoleId() == com.sunilos.p4.bean.RoleBean.FACULTY
                || user.getRoleId() == com.sunilos.p4.bean.RoleBean.PROGRAM_CHAIR)) {
            com.sunilos.p4.util.ScopeAuthorizationUtil.DataScope scope = com.sunilos.p4.util.ScopeAuthorizationUtil.resolveScope(user);
            if (scope != null && scope.isValid()) {
                if (!scope.getCourseIds().isEmpty()) {
                    bean.setScopedCourseIds(scope.getCourseIds());
                }
                if (user.getRoleId() == com.sunilos.p4.bean.RoleBean.FACULTY && !scope.getSubjectIds().isEmpty()) {
                    bean.setScopedSubjectIds(scope.getSubjectIds());
                }
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
        return ORSView.SUBJECT_LIST_VIEW;
    }

    @Override
    protected SubjectModel getModel() {
        return new SubjectModel();
    }

}
