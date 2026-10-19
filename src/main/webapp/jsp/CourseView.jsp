<%@page import="com.sunilos.p4.ctl.CourseCtl"%>
<%@page import="com.sunilos.p4.ctl.BaseCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.CourseBean" scope="request"></jsp:useBean>

<%
    String _suc = ServletUtility.getSuccessMessage(request);
    String _err = ServletUtility.getErrorMessage(request);
%>

<div class="container py-4" style="max-width:580px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold">
        <i class="bi bi-journal-bookmark-fill me-2"></i>
        <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 ? "Edit Course" : "Add Course")%>
      </h5>
    </div>

    <div class="card-body px-4 py-4">

      <% if(_suc != null && !_suc.isEmpty()) { %>
      <div class="alert alert-success py-2"><i class="bi bi-check-circle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_suc)%></div>
      <% } %>
      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.COURSE_CTL)%>" method="POST">
      <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
        <input type="hidden" name="id"              value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
        <input type="hidden" name="createdBy"        value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCreatedBy())%>">
        <input type="hidden" name="modifiedBy"       value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getModifiedBy())%>">
        <input type="hidden" name="createdDatetime"  value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getCreatedDatetime()))%>">
        <input type="hidden" name="modifiedDatetime" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getModifiedDatetime()))%>">

        <div class="mb-3">
          <label class="form-label fw-semibold">Name <span class="text-danger">*</span></label>
          <input type="text" name="name" class="form-control" maxlength="50"
                 placeholder="Letters only"
                 pattern="[A-Za-z]+"
                 title="Only letters are allowed"
                 oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getName()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("name", request))%></div>
        </div>

        <div class="mb-3">
          <label class="form-label fw-semibold">Description <span class="text-danger">*</span></label>
          <input type="text" name="description" class="form-control" maxlength="100"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getDescription()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("description", request))%></div>
        </div>

        <div class="mb-4">
          <label class="form-label fw-semibold">Duration <span class="text-danger">*</span></label>
          <input type="text" name="duration" class="form-control" maxlength="100"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getDuration()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("duration", request))%></div>
        </div>

        <div class="d-flex gap-2 pt-2 border-top">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_SAVE)%>" class="btn btn-primary">
            <i class="bi bi-save me-1"></i> Save
          </button>
          <% if(bean.getId() > 0) { %>
          <% if ("Admin".equalsIgnoreCase((String)session.getAttribute("role"))) { %>
<button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_DELETE)%>" class="btn btn-danger"
                  onclick="return confirm('Delete this course?')">
            <i class="bi bi-trash me-1"></i> Delete
          </button>
<% } %>
          <% } %>
          <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.COURSE_LIST_CTL)%>" class="btn btn-secondary ms-auto">
            <i class="bi bi-x-circle me-1"></i> Cancel
          </a>
        </div>
      </form>
    </div>
  </div>
</div>
