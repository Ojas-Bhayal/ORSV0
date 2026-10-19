<%@page import="com.sunilos.p4.ctl.BaseCtl" %>
  <%@page import="com.sunilos.p4.util.ServletUtility" %>
    <%@page import="java.util.List" %>

      <% int pageNo1=ServletUtility.getPageNo(request); List nextList=(List) request.getAttribute("nextList"); %>

        <div class="d-flex justify-content-center align-items-center gap-2">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_PREVIOUS)%>" class="btn btn-outline-primary btn-sm"
            <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageNo1==1 ? "disabled" : "" )%>>
            <i class="bi bi-chevron-left"></i> Previous
          </button>
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_NEXT)%>" class="btn btn-outline-primary btn-sm"
            <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(nextList.size()==0 ? "disabled" : "" )%>>
            Next <i class="bi bi-chevron-right"></i>
          </button>
        </div>