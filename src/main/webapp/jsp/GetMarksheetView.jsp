<%@page import="com.sunilos.p4.ctl.GetMarksheetCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.bean.MarksheetBean"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.ArrayList"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.MarksheetBean" scope="request"></jsp:useBean>

<%
    String _suc = ServletUtility.getSuccessMessage(request);
    String _err = ServletUtility.getErrorMessage(request);
    List<MarksheetBean> courseMarksheetList = (List<MarksheetBean>) request.getAttribute("courseMarksheetList");
    if ((courseMarksheetList == null || courseMarksheetList.isEmpty())
            && bean.getRollNo() != null && !bean.getRollNo().trim().isEmpty()) {
        courseMarksheetList = new ArrayList<MarksheetBean>();
        courseMarksheetList.add(bean);
    }
    boolean hasResult = courseMarksheetList != null && !courseMarksheetList.isEmpty();
%>

<div class="container py-4" style="max-width:740px;">

  <!-- Search Card -->
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold"><i class="bi bi-search me-2"></i> Get Course-Wise Marksheet</h5>
    </div>
    <div class="card-body px-4 py-4">

      <% if(_suc != null && !_suc.isEmpty()) { %>
      <div class="alert alert-success py-2"><i class="bi bi-check-circle-fill me-2"></i><%=HTMLUtility.escapeHtml(_suc)%></div>
      <% } %>
      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.GET_MARKSHEET_CTL)%>" method="POST">
      <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
        <label class="form-label fw-semibold">Roll No <span class="text-danger">*</span></label>
        <div class="input-group">
          <input type="text" name="rollNo" class="form-control form-control-lg"
                 placeholder="Enter Student Roll Number"
                 value="<%=HTMLUtility.escapeHtml(ServletUtility.getParameter("rollNo", request))%>">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(GetMarksheetCtl.OP_GO)%>"
                  class="btn btn-primary px-4">
            <i class="bi bi-search me-1"></i> Search
          </button>
        </div>
        <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("rollNo", request))%></div>
      </form>
    </div>
  </div>

  <!-- Dynamic Course-Wise Result Cards -->
  <% if (hasResult) {
       MarksheetBean studentHeader = courseMarksheetList.get(0);
  %>
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
    <div class="card-header border-0 py-3 px-4 bg-dark text-white d-flex justify-content-between align-items-center">
      <div>
        <h6 class="mb-0 fw-bold"><i class="bi bi-person-vcard-fill me-2"></i> Student Academic Record</h6>
      </div>
      <span class="badge bg-info text-dark">Roll No: <%=HTMLUtility.escapeHtml(DataUtility.getStringData(studentHeader.getRollNo()))%></span>
    </div>
    <div class="card-body py-3 px-4 bg-light">
      <div class="row">
        <div class="col-sm-6">
          <span class="text-muted small">Student Name:</span>
          <div class="fw-bold fs-5"><%=HTMLUtility.escapeHtml(DataUtility.getStringData(studentHeader.getName()))%></div>
        </div>
        <div class="col-sm-6 text-sm-end mt-2 mt-sm-0">
          <span class="text-muted small">Enrolled Courses Found:</span>
          <div class="fw-bold fs-5 text-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(courseMarksheetList.size())%> Course(s)</div>
        </div>
      </div>
    </div>
  </div>

  <% for (MarksheetBean courseMs : courseMarksheetList) {
       String courseName = HTMLUtility.escapeHtml(DataUtility.getStringData(courseMs.getCourseName()));
       Map<String, Integer> subjectMarks = courseMs.getSubjectMarks();
       int totalMarks = courseMs.getTotalCourseMarks();
       int maxMarks   = courseMs.getMaxCourseMarks();
       double percentage = courseMs.getPercentage();
       boolean coursePassed = true;
       for (Integer subjectScore : subjectMarks.values()) {
         if (subjectScore == null || subjectScore < 35) {
           coursePassed = false;
           break;
         }
       }
       String status = coursePassed ? "PASS" : "FAIL";
       double classAvg = courseMs.getClassAverage();
  %>
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
    <div class="card-header border-0 py-3 px-4 bg-success text-white d-flex justify-content-between align-items-center">
      <h6 class="mb-0 fw-bold">
        <i class="bi bi-journal-code me-2"></i> Course: <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(courseName)%>
      </h6>
      <span class="badge <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("PASS".equals(status) ? "bg-light text-success" : "bg-danger text-white")%> fw-bold px-3 py-1">
        <%=HTMLUtility.escapeHtml(status)%>
      </span>
    </div>
    <div class="card-body p-0">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th class="ps-4">Subject Name (<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(courseName)%>)</th>
            <th class="text-center">Max Marks</th>
            <th class="text-center">Marks Obtained</th>
            <th class="text-center">Subject Status</th>
          </tr>
        </thead>
        <tbody>
          <% for (Map.Entry<String, Integer> entry : subjectMarks.entrySet()) {
               String subName = HTMLUtility.escapeHtml(entry.getKey());
               int subScore = entry.getValue() != null ? entry.getValue() : 0;
               boolean subPass = subScore >= 35;
          %>
          <tr>
            <td class="ps-4 fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(subName)%></td>
            <td class="text-center text-muted">100</td>
            <td class="text-center">
              <span class="badge bg-primary rounded-pill fs-6 px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(subScore)%></span>
            </td>
            <td class="text-center">
              <span class="badge <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(subPass ? "bg-success" : "bg-danger")%>"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(subPass ? "PASS" : "FAIL")%></span>
            </td>
          </tr>
          <% } %>
          <tr class="table-light border-top">
            <th class="ps-4 py-3">Course Total (<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(courseName)%>)</th>
            <th class="text-center py-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(maxMarks)%></th>
            <th class="text-center py-3">
              <span class="badge bg-dark rounded-pill fs-6 px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(totalMarks)%> / <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(maxMarks)%></span>
            </th>
            <th class="text-center py-3">
              <span class="badge bg-info text-dark rounded-pill fs-6 px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", percentage))%></span>
            </th>
          </tr>
          <tr class="table-light">
            <th class="ps-4 py-3 text-secondary" colspan="2">
              <i class="bi bi-bar-chart-fill me-1"></i> Class Benchmark Average (<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(courseName)%>)
            </th>
            <td class="text-center py-3" colspan="2">
              <span class="badge bg-secondary rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", classAvg))%></span>
              <% if (percentage >= classAvg) { %>
                <span class="text-success small fw-semibold ms-2"><i class="bi bi-arrow-up-circle-fill"></i> Above Class Avg</span>
              <% } else { %>
                <span class="text-warning small fw-semibold ms-2"><i class="bi bi-info-circle-fill"></i> Below Class Avg</span>
              <% } %>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <% } %>
  <% } %>

</div>
