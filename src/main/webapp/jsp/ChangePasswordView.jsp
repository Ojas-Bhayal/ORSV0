<%@page import="com.sunilos.p4.ctl.ChangePasswordCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.UserBean" scope="request"></jsp:useBean>

<%
    String _suc = ServletUtility.getSuccessMessage(request);
    String _err = ServletUtility.getErrorMessage(request);
%>

<div class="container py-4" style="max-width:560px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold">
        <i class="bi bi-key-fill me-2"></i> Change Password
      </h5>
    </div>

    <div class="card-body px-4 py-4">

      <% if(_suc != null && !_suc.isEmpty()) { %>
      <div class="alert alert-success py-2"><i class="bi bi-check-circle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_suc)%></div>
      <% } %>
      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.CHANGE_PASSWORD_CTL)%>" method="POST">
      <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">

        <div class="mb-3">
          <label class="form-label fw-semibold">Old Password (8 Characters) <span class="text-danger">*</span></label>
          <input type="password" name="oldPassword" minlength="8" maxlength="8" class="form-control"
                 placeholder="Enter 8-character old password"
                 value="">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("oldPassword", request))%></div>
        </div>

        <div class="mb-3">
          <label class="form-label fw-semibold">New Password (8 Characters) <span class="text-danger">*</span></label>
          <input type="password" name="newPassword" minlength="8" maxlength="8" class="form-control"
                 placeholder="Enter 8-character new password"
                 value="">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("newPassword", request))%></div>
        </div>

        <div class="mb-4">
          <label class="form-label fw-semibold">Confirm Password (8 Characters) <span class="text-danger">*</span></label>
          <input type="password" name="confirmPassword" minlength="8" maxlength="8" class="form-control"
                 placeholder="Confirm 8-character password"
                 value="">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("confirmPassword", request))%></div>
        </div>

        <div class="pt-2 border-top">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ChangePasswordCtl.OP_SAVE)%>" class="btn btn-primary">
            <i class="bi bi-shield-lock me-1"></i> Update Password
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
