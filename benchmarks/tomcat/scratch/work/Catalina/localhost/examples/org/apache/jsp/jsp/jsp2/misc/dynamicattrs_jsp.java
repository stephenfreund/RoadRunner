package org.apache.jsp.jsp.jsp2.misc;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class dynamicattrs_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

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
      out.write("<html>\n");
      out.write("  <head>\n");
      out.write("    <title>JSP 2.0 Examples - Dynamic Attributes</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Examples - Dynamic Attributes</h1>\n");
      out.write("    <hr>\n");
      out.write("    <p>This JSP page invokes a custom tag that accepts a dynamic set \n");
      out.write("    of attributes.  The tag echoes the name and value of all attributes\n");
      out.write("    passed to it.</p>\n");
      out.write("    <hr>\n");
      out.write("    <h2>Invocation 1 (six attributes)</h2>\n");
      out.write("    <ul>\n");
      out.write("      ");
      if (_jspx_meth_my_005fechoAttributes_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("    </ul>\n");
      out.write("    <h2>Invocation 2 (zero attributes)</h2>\n");
      out.write("    <ul>\n");
      out.write("      ");
      if (_jspx_meth_my_005fechoAttributes_005f1(_jspx_page_context))
        return;
      out.write("\n");
      out.write("    </ul>\n");
      out.write("    <h2>Invocation 3 (three attributes)</h2>\n");
      out.write("    <ul>\n");
      out.write("      ");
      if (_jspx_meth_my_005fechoAttributes_005f2(_jspx_page_context))
        return;
      out.write("\n");
      out.write("    </ul>\n");
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

  private boolean _jspx_meth_my_005fechoAttributes_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:echoAttributes
    jsp2.examples.simpletag.EchoAttributesTag _jspx_th_my_005fechoAttributes_005f0 = new jsp2.examples.simpletag.EchoAttributesTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f0);
    _jspx_th_my_005fechoAttributes_005f0.setJspContext(_jspx_page_context);
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "x", new String("1"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "y", new String("2"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "z", new String("3"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "r", new String("red"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "g", new String("green"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(31,6) null
    _jspx_th_my_005fechoAttributes_005f0.setDynamicAttribute(null, "b", new String("blue"));
    _jspx_th_my_005fechoAttributes_005f0.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f0);
    return false;
  }

  private boolean _jspx_meth_my_005fechoAttributes_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:echoAttributes
    jsp2.examples.simpletag.EchoAttributesTag _jspx_th_my_005fechoAttributes_005f1 = new jsp2.examples.simpletag.EchoAttributesTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f1);
    _jspx_th_my_005fechoAttributes_005f1.setJspContext(_jspx_page_context);
    _jspx_th_my_005fechoAttributes_005f1.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f1);
    return false;
  }

  private boolean _jspx_meth_my_005fechoAttributes_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:echoAttributes
    jsp2.examples.simpletag.EchoAttributesTag _jspx_th_my_005fechoAttributes_005f2 = new jsp2.examples.simpletag.EchoAttributesTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f2);
    _jspx_th_my_005fechoAttributes_005f2.setJspContext(_jspx_page_context);
    // /jsp/jsp2/misc/dynamicattrs.jsp(39,6) null
    _jspx_th_my_005fechoAttributes_005f2.setDynamicAttribute(null, "dogName", new String("Scruffy"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(39,6) null
    _jspx_th_my_005fechoAttributes_005f2.setDynamicAttribute(null, "catName", new String("Fluffy"));
    // /jsp/jsp2/misc/dynamicattrs.jsp(39,6) null
    _jspx_th_my_005fechoAttributes_005f2.setDynamicAttribute(null, "blowfishName", new String("Puffy"));
    _jspx_th_my_005fechoAttributes_005f2.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fechoAttributes_005f2);
    return false;
  }
}
