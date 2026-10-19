<%@page import="com.sunilos.p4.ctl.MyProfileCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.UserBean" scope="request"></jsp:useBean>

<%
    String _suc = ServletUtility.getSuccessMessage(request);
    String _err = ServletUtility.getErrorMessage(request);
    HashMap genderMap = new HashMap();
    genderMap.put("M", "Male");
    genderMap.put("F", "Female");
%>

<div class="container py-4" style="max-width:660px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold">
        <i class="bi bi-person-circle me-2"></i> My Profile
      </h5>
    </div>

    <div class="card-body px-4 py-4">

      <% if(_suc != null && !_suc.isEmpty()) { %>
      <div class="alert alert-success py-2"><i class="bi bi-check-circle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_suc)%></div>
      <% } %>
      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MY_PROFILE_CTL)%>" method="POST">
      <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
        <input type="hidden" name="id"              value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
        <input type="hidden" name="createdBy"        value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCreatedBy())%>">
        <input type="hidden" name="modifiedBy"       value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getModifiedBy())%>">
        <input type="hidden" name="createdDatetime"  value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getCreatedDatetime()))%>">
        <input type="hidden" name="modifiedDatetime" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getModifiedDatetime()))%>">

        <div class="mb-3">
          <label class="form-label fw-semibold">Login ID (Email)</label>
          <input type="email" name="login" class="form-control bg-light" readonly
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getLogin()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("login", request))%></div>
        </div>

        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label fw-semibold">First Name <span class="text-danger">*</span></label>
            <input type="text" name="firstName" class="form-control"
                   pattern="[A-Za-z]+"
                   title="Only letters are allowed"
                   oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getFirstName()))%>">
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("firstName", request))%></div>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">Last Name <span class="text-danger">*</span></label>
            <input type="text" name="lastName" class="form-control"
                   pattern="[A-Za-z]+"
                   title="Only letters are allowed"
                   oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getLastName()))%>">
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("lastName", request))%></div>
          </div>
        </div>

        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label fw-semibold">Gender</label>
            <%=HTMLUtility.getList("gender", bean.getGender(), genderMap)%>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">Mobile No <span class="text-danger">*</span></label>
            <input type="text" name="mobileNo" maxlength="10" pattern="[6-9][0-9]{9}"
                   title="10 digits starting with 6, 7, 8, or 9"
                   oninput="this.value=this.value.replace(/[^0-9]/g,'').slice(0,10)"
                   class="form-control"
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getMobileNo()))%>">
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("mobileNo", request))%></div>
          </div>
        </div>

        <div class="mb-4">
          <label class="form-label fw-semibold">Date of Birth (mm/dd/yyyy)</label>
          <div class="input-group">
            <input type="text" name="dob" class="form-control" readonly
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getDateString(bean.getDob()))%>">
            <a class="btn btn-outline-secondary" href="javascript:void(0)" onclick="getCalendar(this.parentNode.querySelector('[name=dob]'));">
              <img src="../img/cal.jpg" width="16" height="15" alt="Calendar">
            </a>
          </div>
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("dob", request))%></div>
        </div>

        <div class="d-flex gap-2 pt-2 border-top">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(MyProfileCtl.OP_SAVE)%>" class="btn btn-primary">
            <i class="bi bi-save me-1"></i> Save
          </button>
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(MyProfileCtl.OP_CHANGE_MY_PASSWORD)%>"
                  class="btn btn-outline-warning">
            <i class="bi bi-key me-1"></i> Change Password
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
