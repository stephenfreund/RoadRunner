package org.apache.jsp.jsp.jsp2.el;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class functions_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

static private org.apache.jasper.runtime.ProtectedFunctionMapper _jspx_fnmap_0;
static private org.apache.jasper.runtime.ProtectedFunctionMapper _jspx_fnmap_1;
static private org.apache.jasper.runtime.ProtectedFunctionMapper _jspx_fnmap_2;

static {
  _jspx_fnmap_0= org.apache.jasper.runtime.ProtectedFunctionMapper.getMapForFunction("fn:escapeXml", org.apache.taglibs.standard.functions.Functions.class, "escapeXml", new Class[] {java.lang.String.class});
  _jspx_fnmap_1= org.apache.jasper.runtime.ProtectedFunctionMapper.getInstance();
  _jspx_fnmap_1.mapFunction("my:reverse", jsp2.examples.el.Functions.class, "reverse", new Class[] {java.lang.String.class});
  _jspx_fnmap_1.mapFunction("fn:escapeXml", org.apache.taglibs.standard.functions.Functions.class, "escapeXml", new Class[] {java.lang.String.class});
  _jspx_fnmap_2= org.apache.jasper.runtime.ProtectedFunctionMapper.getInstance();
  _jspx_fnmap_2.mapFunction("my:countVowels", jsp2.examples.el.Functions.class, "numVowels", new Class[] {java.lang.String.class});
  _jspx_fnmap_2.mapFunction("fn:escapeXml", org.apache.taglibs.standard.functions.Functions.class, "escapeXml", new Class[] {java.lang.String.class});
}

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(1);
    _jspx_dependants.add("/WEB-INF/jsp2/jsp2-example-taglib.tld");
  }

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
      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<html>\n");
      out.write("  <head>\n");
      out.write("    <title>JSP 2.0 Expression Language - Functions</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Expression Language - Functions</h1>\n");
      out.write("    <hr>\n");
      out.write("    An upgrade from the JSTL expression language, the JSP 2.0 EL also\n");
      out.write("    allows for simple function invocation.  Functions are defined\n");
      out.write("    by tag libraries and are implemented by a Java programmer as \n");
      out.write("    static methods.\n");
      out.write("\n");
      out.write("    <blockquote>\n");
      out.write("      <u><b>Change Parameter</b></u>\n");
      out.write("      <form action=\"functions.jsp\" method=\"GET\">\n");
      out.write("\t  foo = <input type=\"text\" name=\"foo\" value=\"");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${fn:escapeXml(param[\"foo\"])}", java.lang.String.class, (PageContext)_jspx_page_context, _jspx_fnmap_0, false));
      out.write("\">\n");
      out.write("          <input type=\"submit\">\n");
      out.write("      </form>\n");
      out.write("      <br>\n");
      out.write("      <code>\n");
      out.write("        <table border=\"1\">\n");
      out.write("          <thead>\n");
      out.write("\t    <td><b>EL Expression</b></td>\n");
      out.write("\t    <td><b>Result</b></td>\n");
      out.write("\t  </thead>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${param[\"foo\"]}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${fn:escapeXml(param[\"foo\"])}", java.lang.String.class, (PageContext)_jspx_page_context, _jspx_fnmap_0, false));
      out.write("&nbsp;</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${my:reverse(param[\"foo\"])}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${my:reverse(fn:escapeXml(param[\"foo\"]))}", java.lang.String.class, (PageContext)_jspx_page_context, _jspx_fnmap_1, false));
      out.write("&nbsp;</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${my:reverse(my:reverse(param[\"foo\"]))}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${my:reverse(my:reverse(fn:escapeXml(param[\"foo\"])))}", java.lang.String.class, (PageContext)_jspx_page_context, _jspx_fnmap_1, false));
      out.write("&nbsp;</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t  <tr>\n");
      out.write("\t    <td>${my:countVowels(param[\"foo\"])}</td>\n");
      out.write("\t    <td>");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${my:countVowels(fn:escapeXml(param[\"foo\"]))}", java.lang.String.class, (PageContext)_jspx_page_context, _jspx_fnmap_2, false));
      out.write("&nbsp;</td>\n");
      out.write("\t  </tr>\n");
      out.write("\t</table>\n");
      out.write("      </code>\n");
      out.write("    </blockquote>\n");
      out.write("  </body>\n");
      out.write("</html>\n");
      out.write("\n");
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
