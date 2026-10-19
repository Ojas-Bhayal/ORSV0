<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.bean.SubjectBean"%>
<%@page import="com.sunilos.p4.bean.FacultyBean"%>
<%@page import="com.sunilos.p4.bean.FacultyAllocationBean"%>
<%@page import="com.sunilos.p4.bean.StudentBean"%>
<%@page import="com.sunilos.p4.bean.EnrollmentBean"%>
<%@page import="com.sunilos.p4.bean.MarksheetBean"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>

<%
    String _err = ServletUtility.getErrorMessage(request);
    List<CourseBean> courseList = (List<CourseBean>) request.getAttribute("courseList");
    List<SubjectBean> subjectList = (List<SubjectBean>) request.getAttribute("subjectList");
    List<FacultyBean> facultyList = (List<FacultyBean>) request.getAttribute("facultyList");
    List<FacultyAllocationBean> allocationList = (List<FacultyAllocationBean>) request.getAttribute("allocationList");
    List<StudentBean> studentList = (List<StudentBean>) request.getAttribute("studentList");
    List<EnrollmentBean> enrollmentList = (List<EnrollmentBean>) request.getAttribute("enrollmentList");
    Map<String, Integer> courseEnrollmentMap = (Map<String, Integer>) request.getAttribute("courseEnrollmentMap");
    Map<String, Double> courseAverageMap = (Map<String, Double>) request.getAttribute("courseAverageMap");
    Map<Long, Double> subjectAverageMap = (Map<Long, Double>) request.getAttribute("subjectAverageMap");

    int totalCourses     = courseList     != null ? courseList.size()     : 0;
    int totalSubjects    = subjectList    != null ? subjectList.size()    : 0;
    java.util.Set<Long> allocatedFacultyIds = new java.util.HashSet<>();
    if (allocationList != null) {
        for (FacultyAllocationBean a : allocationList) {
            if (a != null && a.getFacultyId() > 0) allocatedFacultyIds.add(a.getFacultyId());
        }
    }
    int totalFaculty = allocationList != null ? allocatedFacultyIds.size() : (facultyList != null ? facultyList.size() : 0);
    int totalEnrollments = 0;
    if (enrollmentList != null) {
        for (EnrollmentBean e : enrollmentList) {
            if (e != null && "ACTIVE".equalsIgnoreCase(e.getStatus())) totalEnrollments++;
        }
    } else if (studentList != null) {
        totalEnrollments = studentList.size();
    }
%>

<div class="container-fluid px-4 py-4" style="max-width:1200px;">

  <!-- Header Banner -->
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
    <div class="card-header text-white border-0 py-3 px-4 d-flex justify-content-between align-items-center"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <div>
        <h5 class="mb-0 fw-bold"><i class="bi bi-bar-chart-line-fill me-2"></i> Program Chair Departmental Audit &amp; Enrollment Analysis</h5>
        <small class="opacity-75">Concurrent Departmental Audit across Courses, Subject Modules, Faculty Allocations &amp; Student Enrollments</small>
      </div>
      <span class="badge bg-light text-primary px-3 py-2 fw-semibold">Department Overview</span>
    </div>
  </div>

  <% if(_err != null && !_err.isEmpty()) { %>
  <div class="alert alert-danger py-2"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
  <% } %>

  <!-- KPI Summary Row -->
  <div class="row g-3 mb-4">
    <div class="col-6 col-md-3">
      <div class="card border-0 shadow-sm rounded-4 p-3 text-center border-top border-4 border-primary">
        <div class="text-muted small text-uppercase fw-semibold">Active Programs</div>
        <div class="fs-2 fw-bold text-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(totalCourses)%></div>
        <div class="small text-muted">Courses Configured</div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border-0 shadow-sm rounded-4 p-3 text-center border-top border-4 border-info">
        <div class="text-muted small text-uppercase fw-semibold">Curriculum Modules</div>
        <div class="fs-2 fw-bold text-info"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(totalSubjects)%></div>
        <div class="small text-muted">Active Subjects</div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border-0 shadow-sm rounded-4 p-3 text-center border-top border-4 border-success">
        <div class="text-muted small text-uppercase fw-semibold">Academic Staff</div>
        <div class="fs-2 fw-bold text-success"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(totalFaculty)%></div>
        <div class="small text-muted">Allocated Educators</div>
      </div>
    </div>
    <div class="col-6 col-md-3">
      <div class="card border-0 shadow-sm rounded-4 p-3 text-center border-top border-4 border-warning">
        <div class="text-muted small text-uppercase fw-semibold">Course Registrations</div>
        <div class="fs-2 fw-bold text-warning"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(totalEnrollments)%></div>
        <div class="small text-muted">Active Enrollments</div>
      </div>
    </div>
  </div>

  <!-- Course benchmark analysis -->
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
    <div class="card-header bg-light py-3 px-4 border-bottom">
      <h6 class="mb-0 fw-bold text-dark"><i class="bi bi-pie-chart-fill me-2 text-primary"></i> 1. Course-Wise Enrollment &amp; Performance Benchmarks</h6>
    </div>
    <div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th>Course ID</th>
            <th>Course Name</th>
            <th>Duration / Term</th>
            <th>Linked Subjects</th>
            <th class="text-center">Enrolled Headcount</th>
            <th class="text-center">Class Average Benchmark</th>
          </tr>
        </thead>
        <tbody>
          <% if (courseList != null && !courseList.isEmpty()) {
               for (CourseBean c : courseList) {
                 int enrolled = (courseEnrollmentMap != null && courseEnrollmentMap.get(c.getName()) != null) ? courseEnrollmentMap.get(c.getName()) : 0;
                 double avg = (courseAverageMap != null && courseAverageMap.get(c.getName()) != null) ? courseAverageMap.get(c.getName()) : Double.NaN;
                 StringBuilder subs = new StringBuilder();
                 if (subjectList != null) {
                   for (SubjectBean s : subjectList) {
                     if (s.getCourseId() == c.getId()) {
                       if (subs.length() > 0) subs.append(", ");
                       subs.append(s.getName());
                     }
                   }
                 }
          %>
          <tr>
            <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%></td>
            <td class="fw-bold text-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></td>
            <td><span class="badge bg-secondary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getDuration())%></span></td>
            <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(subs.length() > 0 ? subs.toString() : "No subjects linked")%></td>
            <td class="text-center"><span class="badge bg-primary rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(enrolled)%> Students</span></td>
            <td class="text-center">
              <% if (Double.isFinite(avg)) { %>
                <span class="badge bg-success rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", avg))%></span>
              <% } else { %>
                <span class="badge bg-secondary rounded-pill px-3">Unavailable</span>
              <% } %>
            </td>
          </tr>
          <%   }
             } else { %>
          <tr><td colspan="6" class="text-center text-muted py-3">No courses found.</td></tr>
          <% } %>
        </tbody>
      </table>
    </div>
  </div>

  <div class="row g-4">
    <!-- Academic staff teaching allocations -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 overflow-hidden h-100">
        <div class="card-header bg-light py-3 px-4 border-bottom">
          <h6 class="mb-0 fw-bold text-dark"><i class="bi bi-person-badge-fill me-2 text-success"></i> 2. Academic Staff Teaching Allocations</h6>
        </div>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Educator</th>
                <th>Email</th>
                <th>Allocated Course</th>
                <th>Allocated Subject</th>
              </tr>
            </thead>
            <tbody>
              <% if (allocationList != null && !allocationList.isEmpty()) {
                   for (FacultyAllocationBean a : allocationList) { %>
              <tr>
                <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(a.getFacultyName())%></td>
                <td class="small text-muted"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(a.getFacultyEmail())%></td>
                <td><span class="badge bg-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(a.getCourseName())%></span></td>
                <td><span class="badge bg-info text-dark"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(a.getSubjectName())%></span></td>
              </tr>
              <%   }
                 } else if (facultyList != null && !facultyList.isEmpty()) {
                   for (FacultyBean f : facultyList) { %>
              <tr>
                <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(f.getFirstName())%> <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(f.getLastName())%></td>
                <td class="small text-muted"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(f.getEmail())%></td>
                <td><span class="badge bg-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(f.getCourseName() != null ? f.getCourseName() : "Unassigned")%></span></td>
                <td><span class="badge bg-info text-dark"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(f.getSubjectName() != null ? f.getSubjectName() : "Unassigned")%></span></td>
              </tr>
              <%   }
                 } else { %>
              <tr><td colspan="4" class="text-center text-muted py-3">No faculty allocations found.</td></tr>
              <% } %>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Curriculum and subject hierarchy -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 overflow-hidden h-100">
        <div class="card-header bg-light py-3 px-4 border-bottom">
          <h6 class="mb-0 fw-bold text-dark"><i class="bi bi-journal-bookmark-fill me-2 text-info"></i> 3. Curriculum &amp; Subject Hierarchy</h6>
        </div>
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>Subject ID</th>
                <th>Subject Module</th>
                <th>Course ID</th>
                <th>Description</th>
                <th class="text-center">Subject Class Average</th>
              </tr>
            </thead>
            <tbody>
              <% if (subjectList != null && !subjectList.isEmpty()) {
                   for (SubjectBean s : subjectList) { %>
              <tr>
                <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getId())%></td>
                <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getName())%></td>
                <td><span class="badge bg-secondary">Course #<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getCourseId())%></span></td>
                <td class="small text-muted"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getDescription())%></td>
                <td class="text-center">
                  <% Double subjectAvg = subjectAverageMap != null ? subjectAverageMap.get(s.getId()) : null;
                     if (subjectAvg != null && Double.isFinite(subjectAvg.doubleValue())) { %>
                    <span class="badge bg-success rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", subjectAvg.doubleValue()))%></span>
                  <% } else { %>
                    <span class="badge bg-secondary rounded-pill px-3">Unavailable</span>
                  <% } %>
                </td>
              </tr>
              <%   }
                 } else { %>
              <tr><td colspan="5" class="text-center text-muted py-3">No subject modules found.</td></tr>
              <% } %>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

</div>
