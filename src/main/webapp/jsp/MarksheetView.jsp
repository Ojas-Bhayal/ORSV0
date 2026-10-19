<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.MarksheetCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.bean.SubjectBean"%>
<%@page import="java.util.List"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.MarksheetBean" scope="request"></jsp:useBean>

<%
    List<CourseBean> courseList = (List<CourseBean>) request.getAttribute("courseList");
    List<SubjectBean> subjectList = (List<SubjectBean>) request.getAttribute("subjectList");
    long selectedCourseId = bean.getCourseId();
    long selectedSubjectId = bean.getSubjectId();
%>

<div class="container py-4" style="max-width:640px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold">
        <i class="bi bi-file-earmark-text-fill me-2"></i>
        <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 ? "Edit Course-Wise Marksheet" : "Add Course-Wise Marksheet")%>
      </h5>
    </div>

    <div class="card-body px-4 py-4">

      <ors:formMsg />

      <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MARKSHEET_CTL)%>" method="POST">
      <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
        <input type="hidden" name="id"              value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
        <input type="hidden" name="createdBy"        value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCreatedBy())%>">
        <input type="hidden" name="modifiedBy"       value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getModifiedBy())%>">
        <input type="hidden" name="createdDatetime"  value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getCreatedDatetime()))%>">
        <input type="hidden" name="modifiedDatetime" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getModifiedDatetime()))%>">

        <div class="mb-3">
          <label class="form-label fw-semibold"><ors:message key="marksheet.rollno"/> <span class="text-danger">*</span></label>
          <input type="text" name="rollNo" class="form-control"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getRollNo()))%>"
                 <%=com.sunilos.p4.util.HTMLUtility.escapeHtml((bean.getId() > 0) ? "readonly" : "")%>>
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("rollNo", request))%></div>
        </div>

        <div class="mb-3">
          <label class="form-label fw-semibold"><ors:message key="marksheet.name"/> <span class="text-danger">*</span></label>
          <ors:formSelect name="studentId" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.valueOf(bean.getStudentId()))%>" list="studentList" />
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("studentId", request))%></div>
        </div>

        <!-- Dynamic Course Selection (strictly from CourseModel) -->
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label fw-semibold">Course <span class="text-danger">*</span></label>
            <select name="courseId" id="courseSelect" class="form-select" onchange="filterSubjectsByCourse()">
              <option value="">-- Select Course --</option>
              <% if (courseList != null && !courseList.isEmpty()) {
                   for (CourseBean c : courseList) { %>
              <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml((c.getId() == selectedCourseId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></option>
              <%   }
                 } %>
            </select>
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("courseId", request))%></div>
          </div>

          <!-- Dynamic Subject Selection (strictly from SubjectModel) -->
          <div class="col-md-6">
            <label class="form-label fw-semibold">Subject <span class="text-danger">*</span></label>
            <select name="subjectId" id="subjectSelect" class="form-select">
              <option value="">-- Select Subject --</option>
              <% if (subjectList != null && !subjectList.isEmpty()) {
                   for (SubjectBean s : subjectList) { %>
              <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getId())%>" data-course-id="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getCourseId())%>"
                      <%=com.sunilos.p4.util.HTMLUtility.escapeHtml((s.getId() == selectedSubjectId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getName())%></option>
              <%   }
                 } %>
            </select>
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("subjectId", request))%></div>
          </div>
        </div>

        <!-- Dynamic Marks Input (0-100) -->
        <div class="mb-4">
          <label class="form-label fw-semibold">Subject Marks (Out of 100) <span class="text-danger">*</span></label>
          <input type="number" name="marks" min="0" max="100" class="form-control"
                 placeholder="Enter marks (0 - 100)"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 || bean.getMarks() != null ? DataUtility.getStringData(bean.getMarks()) : "")%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("marks", request))%></div>
        </div>

        <div class="d-flex gap-2 pt-2 border-top">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(MarksheetCtl.OP_SAVE)%>" class="btn btn-primary">
            <i class="bi bi-save me-1"></i> <ors:message key="button.save"/>
          </button>
          <% if(bean.getId() > 0) { %>
          <% if ("Admin".equalsIgnoreCase((String)session.getAttribute("role"))) { %>
<button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(MarksheetCtl.OP_DELETE)%>" class="btn btn-danger"
                  onclick="return confirm('Delete this marksheet?')">
            <i class="bi bi-trash me-1"></i> <ors:message key="button.delete"/>
          </button>
<% } %>
          <% } %>
          <a href="MarksheetListCtl?id=0" class="btn btn-secondary ms-auto">
            <i class="bi bi-x-circle me-1"></i> <ors:message key="button.cancel"/>
          </a>
        </div>
      </form>
    </div>
  </div>
</div>

<script>
function filterSubjectsByCourse() {
  var courseSelect = document.getElementById('courseSelect');
  var subjectSelect = document.getElementById('subjectSelect');
  if (!courseSelect || !subjectSelect) return;
  var selectedCourse = courseSelect.value;
  for (var i = 0; i < subjectSelect.options.length; i++) {
    var opt = subjectSelect.options[i];
    if (!opt.value) continue;
    var cId = opt.getAttribute('data-course-id');
    if (!selectedCourse || !cId || cId === selectedCourse) {
      opt.style.display = '';
    } else {
      opt.style.display = 'none';
    }
  }
}
document.addEventListener('DOMContentLoaded', filterSubjectsByCourse);
</script>
