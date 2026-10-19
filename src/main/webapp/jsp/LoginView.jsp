<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.LoginCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.ctl.BaseCtl"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.util.MessageSource"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.UserBean" scope="request"></jsp:useBean>

<%
    MessageSource ms = MessageSource.getInstance();
%>

<div class="container-fluid min-vh-100 d-flex align-items-center justify-content-center py-5"
     style="background: linear-gradient(135deg, #e8f0fe 0%, #f1f5f9 100%);">

  <div class="col-12 col-sm-9 col-md-6 col-lg-4">

    <div class="card border-0 shadow-lg rounded-4 overflow-hidden">

      <div class="card-header text-white text-center py-4 border-0"
           style="background: linear-gradient(135deg, #0d2137 0%, #1565c0 100%);">
        <div class="d-flex justify-content-center mb-3">
          <div class="rounded-circle d-flex align-items-center justify-content-center"
               style="width:68px; height:68px; background:rgba(255,255,255,0.18); border:2px solid rgba(255,255,255,0.3); font-size:1.9rem;">
            <i class="fas fa-graduation-cap"></i>
          </div>
        </div>
        <h4 class="fw-bold mb-1"><ors:message key="login.title"/></h4>
        <p class="mb-0 opacity-75 small">Online Result System</p>
      </div>

      <div class="card-body px-4 py-4">

        <ors:formMsg />

        <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.LOGIN_CTL)%>" method="POST">
        <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">

          <!-- Login ID (Email Validation - Requirement 5) -->
          <div class="mb-3">
            <label class="form-label fw-semibold text-secondary small text-uppercase tracking-wide">
              <i class="bi bi-envelope-fill me-1"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ms.get("login.userid"))%>
              <span class="text-danger">*</span>
            </label>
            <input type="email"
                   name="login"
                   class="form-control form-control-lg"
                   placeholder="Enter your email ID"
                   value="<%=HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getLogin()))%>">
            <% String loginErr = ServletUtility.getErrorMessage("login", request);
               if (loginErr != null && !loginErr.isEmpty()) { %>
            <div class="text-danger small mt-1"><i class="bi bi-exclamation-circle"></i> <%=HTMLUtility.escapeHtml(loginErr)%></div>
            <% } %>
          </div>

          <!-- Password (8 characters - Requirement 2) -->
          <div class="mb-4">
            <label class="form-label fw-semibold text-secondary small text-uppercase">
              <i class="bi bi-lock-fill me-1"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ms.get("login.password"))%> (8 Characters)
              <span class="text-danger">*</span>
            </label>
            <input type="password"
                   name="password"
                   minlength="8"
                   maxlength="8"
                   class="form-control form-control-lg"
                   placeholder="Enter 8-character password"
                   value="">
            <% String passErr = ServletUtility.getErrorMessage("password", request);
               if (passErr != null && !passErr.isEmpty()) { %>
            <div class="text-danger small mt-1"><i class="bi bi-exclamation-circle"></i> <%=HTMLUtility.escapeHtml(passErr)%></div>
            <% } %>
          </div>

          <button type="submit" name="operation"
                  class="btn btn-primary btn-lg w-100 fw-semibold"
                  style="background:linear-gradient(135deg,#1565c0,#0d47a1); border:none;">
            <i class="bi bi-box-arrow-in-right me-2"></i><ors:message key="login.signin"/>
          </button>

          <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
            <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.USER_REGISTRATION_CTL)%>"
               class="text-success fw-semibold text-decoration-none small">
              <i class="bi bi-person-plus-fill me-1"></i><ors:message key="login.signup"/>
            </a>
            <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.FORGET_PASSWORD_CTL)%>"
               class="text-secondary text-decoration-none small">
              <i class="bi bi-question-circle me-1"></i><ors:message key="login.forgotpassword"/>
            </a>
          </div>

        </form>
      </div>
    </div>

    <p class="text-center text-muted small mt-3">
      &copy; 2024 Rays Technologies &mdash; Online Result System
    </p>

  </div>
</div>
