<%@page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.ForgetPasswordCtl" %>
<%@page import="com.sunilos.p4.ctl.ORSView" %>
<%@page import="com.sunilos.p4.util.ServletUtility" %>
<%@page import="com.sunilos.p4.util.HTMLUtility" %>
<jsp:useBean id="bean" class="com.sunilos.p4.bean.UserBean" scope="request" />
<%
    String resetLogin = ServletUtility.getParameter("login", request);
    String successMessage = ServletUtility.getSuccessMessage(request);
    String errorMessage = ServletUtility.getErrorMessage(request);
    String csrfToken = session.getAttribute("csrfToken") == null ? "" : String.valueOf(session.getAttribute("csrfToken"));
%>

<section class="ors-reset-page container py-4 py-lg-5">
  <div class="ors-reset-heading mb-4">
    <span class="ors-reset-icon" aria-hidden="true"><i class="bi bi-shield-lock-fill"></i></span>
    <div>
      <h1 class="h2 mb-2"><ors:message key="reset.title" /></h1>
      <p class="text-muted mb-0"><ors:message key="reset.intro" /></p>
    </div>
  </div>

  <% if (successMessage != null && !successMessage.trim().isEmpty()) { %>
    <div class="alert alert-success" role="status"><i class="bi bi-check-circle-fill me-2"></i><%=HTMLUtility.escapeHtml(successMessage)%></div>
  <% } %>
  <% if (errorMessage != null && !errorMessage.trim().isEmpty()) { %>
    <div class="alert alert-danger" role="alert"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=HTMLUtility.escapeHtml(errorMessage)%></div>
  <% } %>

  <div class="ors-reset-grid">
    <section class="card ors-reset-card border-0 shadow-sm">
      <div class="card-body p-4 p-lg-4">
        <div class="d-flex align-items-center gap-3 mb-3">
          <span class="ors-reset-step">1</span>
          <div>
            <h2 class="h5 mb-1"><ors:message key="reset.request.heading" /></h2>
            <p class="small text-muted mb-0"><ors:message key="reset.request.help" /></p>
          </div>
        </div>
        <form action="<%=HTMLUtility.escapeHtml(ORSView.FORGET_PASSWORD_CTL)%>" method="post" class="ors-reset-form">
          <input type="hidden" name="csrfToken" value="<%=HTMLUtility.escapeHtml(csrfToken)%>">
          <div class="mb-3">
            <label class="form-label" for="requestLogin"><ors:message key="reset.email" /> <span class="text-danger">*</span></label>
            <input id="requestLogin" type="email" name="login" class="form-control" autocomplete="email" required
                   placeholder="<ors:message key='reset.email.placeholder' />"
                   value="<%=HTMLUtility.escapeHtml(resetLogin)%>">
            <% String loginError = ServletUtility.getErrorMessage("login", request); if (loginError != null && !loginError.isEmpty()) { %>
              <div class="invalid-feedback d-block"><%=HTMLUtility.escapeHtml(loginError)%></div>
            <% } %>
          </div>
          <button type="submit" name="operation" value="<%=ForgetPasswordCtl.OP_GO%>" class="btn btn-primary w-100 ors-reset-primary">
            <i class="bi bi-envelope-arrow-up me-2"></i><ors:message key="reset.request.button" />
          </button>
          <p class="form-text mt-3 mb-0"><ors:message key="reset.privacy.note" /></p>
        </form>
      </div>
    </section>

    <section class="card ors-reset-card border-0 shadow-sm">
      <div class="card-body p-4 p-lg-4">
        <div class="d-flex align-items-center gap-3 mb-3">
          <span class="ors-reset-step">2</span>
          <div>
            <h2 class="h5 mb-1"><ors:message key="reset.redeem.heading" /></h2>
            <p class="small text-muted mb-0"><ors:message key="reset.redeem.help" /></p>
          </div>
        </div>
        <form action="<%=HTMLUtility.escapeHtml(ORSView.FORGET_PASSWORD_CTL)%>" method="post" class="ors-reset-form">
          <input type="hidden" name="csrfToken" value="<%=HTMLUtility.escapeHtml(csrfToken)%>">
          <div class="mb-3">
            <label class="form-label" for="redeemLogin"><ors:message key="reset.email" /> <span class="text-danger">*</span></label>
            <input id="redeemLogin" type="email" name="login" class="form-control" autocomplete="email" required
                   placeholder="<ors:message key='reset.email.placeholder' />"
                   value="<%=HTMLUtility.escapeHtml(resetLogin)%>">
          </div>
          <div class="mb-3">
            <label class="form-label" for="resetToken"><ors:message key="reset.token" /> <span class="text-danger">*</span></label>
            <input id="resetToken" type="text" name="resetToken" class="form-control" minlength="64" maxlength="64"
                   autocomplete="off" autocapitalize="off" spellcheck="false" required
                   placeholder="<ors:message key='reset.token.placeholder' />">
            <% String tokenError = ServletUtility.getErrorMessage("resetToken", request); if (tokenError != null && !tokenError.isEmpty()) { %>
              <div class="invalid-feedback d-block"><%=HTMLUtility.escapeHtml(tokenError)%></div>
            <% } %>
          </div>
          <div class="mb-3">
            <label class="form-label" for="newPassword"><ors:message key="reset.new.password" /> <span class="text-danger">*</span></label>
            <input id="newPassword" type="password" name="newPassword" class="form-control" minlength="8" maxlength="8" pattern=".{8}"
                   autocomplete="new-password" required placeholder="<ors:message key='reset.password.placeholder' />">
            <% String newPasswordError = ServletUtility.getErrorMessage("newPassword", request); if (newPasswordError != null && !newPasswordError.isEmpty()) { %>
              <div class="invalid-feedback d-block"><%=HTMLUtility.escapeHtml(newPasswordError)%></div>
            <% } %>
          </div>
          <div class="mb-4">
            <label class="form-label" for="confirmPassword"><ors:message key="reset.confirm.password" /> <span class="text-danger">*</span></label>
            <input id="confirmPassword" type="password" name="confirmPassword" class="form-control" minlength="8" maxlength="8" pattern=".{8}"
                   autocomplete="new-password" required placeholder="<ors:message key='reset.confirm.placeholder' />">
            <% String confirmError = ServletUtility.getErrorMessage("confirmPassword", request); if (confirmError != null && !confirmError.isEmpty()) { %>
              <div class="invalid-feedback d-block"><%=HTMLUtility.escapeHtml(confirmError)%></div>
            <% } %>
          </div>
          <button type="submit" name="operation" value="<%=ForgetPasswordCtl.OP_RESET_PASSWORD%>" class="btn btn-primary w-100 ors-reset-primary">
            <i class="bi bi-key-fill me-2"></i><ors:message key="reset.submit.button" />
          </button>
          <p class="form-text mt-3 mb-0"><ors:message key="reset.password.rule" /></p>
        </form>
      </div>
    </section>
  </div>
</section>
