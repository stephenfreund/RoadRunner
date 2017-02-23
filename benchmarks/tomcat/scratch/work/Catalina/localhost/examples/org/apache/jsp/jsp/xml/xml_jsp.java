package org.apache.jsp.jsp.xml;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.util.Date;
import java.util.Locale;
import java.text.*;

public final class xml_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {


  String getDateTimeStr(Locale l) {
    DateFormat df = SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, l);
    return df.format(new Date());
  }

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  private javax.el.ExpressionFactory _el_expressionfactory;
  private org.apache.AnnotationProcessor _jsp_annotationprocessor;

  public Object getDependants() {
    return _jspx_dependants;
  }

  public void _jspInit() {
    _el_expressionfactory = _jspxFactory.getJspApplicationContext(getServletConfig().getServletContext()).getExpressionFactory();
    _jsp_annotationprocessor = (org.apache.AnnotationProcessor) getServletConfig().getServletContext().getAttribute(org.apache.AnnotationProcessor.class.getName());
  }

  public void _jspDestroy() {
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;
    PageContext _jspx_page_context = null;


    try {
      response.setContentType("text/html;charset=UTF-8");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("<html>");
      out.write("<head>");
      out.write("<title>");
      out.write("Example JSP in XML format");
      out.write("</title>");
      out.write("</head>");
      out.write("<body>");
      out.write("\n");
      out.write("This is the output of a simple JSP using XML format. \n");
      out.write("<br/>");
      out.write("<div>");
      out.write("Use a jsp:scriptlet to loop from 1 to 10: ");
      out.write("</div>");

// Note we need to declare CDATA because we don't escape the less than symbol

  for (int i = 1; i<=10; i++) {
    out.println(i);
    if (i < 10) {
      out.println(", ");
    }
  }

      out.write("\n");
      out.write("  <br><br>\n");
      out.write("<div align=\"left\">");
      out.write("\n");
      out.write("  Use a jsp:expression to write the date and time in the browser's locale: \n");
      out.write("  ");
      out.print(getDateTimeStr(request.getLocale()));
      out.write("</div>");
      out.write("\n");
      out.write("  <p>This sentence is enclosed in a jsp:text element.</p>\n");
      out.write("</body>");
      out.write("</html>");
    } catch (Throwable t) {
      if (!(t instanceof SkipPageException)){
        out = _jspx_out;
        if (out != null && out.getBufferSize() != 0)
          try { out.clearBuffer(); } catch (java.io.IOException e) {}
        if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
      }
    } finally {
      _jspxFactory.releasePageContext(_jspx_page_context);
    }
  }
}
