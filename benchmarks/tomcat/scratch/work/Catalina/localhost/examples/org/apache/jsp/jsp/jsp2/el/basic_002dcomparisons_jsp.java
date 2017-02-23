package org.apache.jsp.jsp.jsp2.el;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class basic_002dcomparisons_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

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
      response.setContentType("text/html");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			null, true, 8192, true);
      _jspx_page_context = pageContext;
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("<!--\n");
      out.write(" Licensed to the Apache Software Foundation (ASF) under one or more\n");
      out.write("  contributor license agreements.  See the NOTICE file distributed with\n");
      out.write("  this work for additional information regarding copyright ownership.\n");
      out.write("  The ASF licenses this file to You under the Apache License, Version 2.0\n");
      out.write("  (the \"License\"); you may not use this file except in compliance with\n");
      out.write("  the License.  You may obtain a copy of the License at\n");
      out.write("\n");
      out.write("      http://www.apache.org/licenses/LICENSE-2.0\n");
      out.write("\n");
      out.write("  Unless required by applicable law or agreed to in writing, software\n");
      out.write("  distributed under the License is distributed on an \"AS IS\" BASIS,\n");
      out.write("  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
      out.write("  See the License for the specific language governing permissions and\n");
      out.write("  limitations under the License.\n");
      out.write("-->\n");
      out.write("<html>\n");
      out.write("  <head>\n");
      out.write("    <title>JSP 2.0 Expression Language - Basic Comparisons</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Expression Language - Basic Comparisons</h1>\n");
      out.write("    <hr>\n");
      out.write("    This example illustrates basic Expression Language comparisons.\n");
      out.write("    The following comparison operators are supported:\n");
      out.write("    <ul>\n");
      out.write("      <li>Less-than (&lt; or lt)</li>\n");
      out.write("      <li>Greater-than (&gt; or gt)</li>\n");
      out.write("      <li>Less-than-or-equal (&lt;= or le)</li>\n");
      out.write("      <li>Greater-than-or-equal (&gt;= or ge)</li>\n");
      out.write("      <li>Equal (== or eq)</li>\n");
      out.write("      <li>Not Equal (!= or ne)</li>\n");
      out.write("    </ul>\n");
      out.write("    <blockquote>\n");
      out.write("      <u><b>Numeric</b></u>\n");
      out.write("      <code>\n");
      out.write("        <table border=\"1\">\n");
      out.write("          <thead>\n");
      out.write("\t    <td><b>EL Expression</b></td>\n");
      out.write("\t    <td><b>Result</b></td>\n");
      out.write("\t  </thead>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${1 &lt; 2}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${1 < 2}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${1 lt 2}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${1 lt 2}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${1 &gt; (4/2)}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${1 > (4/2)}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${1 gt (4/2)}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${1 gt (4/2)}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${4.0 &gt;= 3}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${4.0 >= 3}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${4.0 ge 3}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${4.0 ge 3}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${4 &lt;= 3}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${4 <= 3}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${4 le 3}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${4 le 3}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${100.0 == 100}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${100.0 == 100}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${100.0 eq 100}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${100.0 eq 100}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${(10*10) != 100}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${(10*10) != 100}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${(10*10) ne 100}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${(10*10) ne 100}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t</table>\n");
      out.write("      </code>\n");
      out.write("      <br>\n");
      out.write("      <u><b>Alphabetic</b></u>\n");
      out.write("      <code>\n");
      out.write("        <table border=\"1\">\n");
      out.write("          <thead>\n");
      out.write("\t    <td><b>EL Expression</b></td>\n");
      out.write("\t    <td><b>Result</b></td>\n");
      out.write("\t  </thead>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${'a' &lt; 'b'}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${'a' < 'b'}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${'hip' &gt; 'hit'}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${'hip' > 'hit'}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${'4' &gt; 3}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${'4' > 3}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t</table>\n");
      out.write("      </code>\n");
      out.write("    </blockquote>\n");
      out.write("  </body>\n");
      out.write("</html>\n");
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
