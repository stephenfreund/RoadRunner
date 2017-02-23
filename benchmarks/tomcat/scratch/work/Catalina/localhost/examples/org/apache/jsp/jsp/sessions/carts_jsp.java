package org.apache.jsp.jsp.sessions;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class carts_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(1);
    _jspx_dependants.add("/jsp/sessions/carts.html");
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

      out.write("<html>\n");
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
      sessions.DummyCart cart = null;
      synchronized (session) {
        cart = (sessions.DummyCart) _jspx_page_context.getAttribute("cart", PageContext.SESSION_SCOPE);
        if (cart == null){
          cart = new sessions.DummyCart();
          _jspx_page_context.setAttribute("cart", cart, PageContext.SESSION_SCOPE);
        }
      }
      out.write('\n');
      out.write('\n');
      org.apache.jasper.runtime.JspRuntimeLibrary.introspect(_jspx_page_context.findAttribute("cart"), request);
      out.write('\n');

	cart.processRequest();

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("<FONT size = 5 COLOR=\"#CC0000\">\n");
      out.write("<br> You have the following items in your cart:\n");
      out.write("<ol>\n");
 
	String[] items = cart.getItems();
	for (int i=0; i<items.length; i++) {

      out.write("\n");
      out.write("<li> ");
 out.print(util.HTMLFilter.filter(items[i])); 
      out.write(' ');
      out.write('\n');

	}

      out.write("\n");
      out.write("</ol>\n");
      out.write("\n");
      out.write("</FONT>\n");
      out.write("\n");
      out.write("<hr>\n");
      out.write("<html>\n");
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
      out.write("<head>\n");
      out.write("    <title>carts</title>\n");
      out.write("</head>\n");
      out.write("\n");
      out.write(" <body bgcolor=\"white\">\n");
      out.write("<font size = 5 color=\"#CC0000\">\n");
      out.write("\n");
      out.write("<form type=POST action=carts.jsp>\n");
      out.write("<BR>\n");
      out.write("Please enter item to add or remove:\n");
      out.write("<br>\n");
      out.write("Add Item:\n");
      out.write("\n");
      out.write("<SELECT NAME=\"item\">\n");
      out.write("<OPTION>Beavis & Butt-head Video collection\n");
      out.write("<OPTION>X-files movie\n");
      out.write("<OPTION>Twin peaks tapes\n");
      out.write("<OPTION>NIN CD\n");
      out.write("<OPTION>JSP Book\n");
      out.write("<OPTION>Concert tickets\n");
      out.write("<OPTION>Love life\n");
      out.write("<OPTION>Switch blade\n");
      out.write("<OPTION>Rex, Rugs & Rock n' Roll\n");
      out.write("</SELECT>\n");
      out.write("\n");
      out.write("\n");
      out.write("<br> <br>\n");
      out.write("<INPUT TYPE=submit name=\"submit\" value=\"add\">\n");
      out.write("<INPUT TYPE=submit name=\"submit\" value=\"remove\">\n");
      out.write("\n");
      out.write("</form>\n");
      out.write("       \n");
      out.write("</FONT>\n");
      out.write("</body>\n");
      out.write("</html>\n");
      out.write("\n");
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
