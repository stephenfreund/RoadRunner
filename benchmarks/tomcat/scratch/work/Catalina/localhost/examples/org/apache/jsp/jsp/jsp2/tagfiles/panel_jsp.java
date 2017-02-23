package org.apache.jsp.jsp.jsp2.tagfiles;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class panel_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(1);
    _jspx_dependants.add("/WEB-INF/tags/panel.tag");
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
      out.write("    <title>JSP 2.0 Examples - Panels using Tag Files</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Examples - Panels using Tag Files</h1>\n");
      out.write("    <hr>\n");
      out.write("    <p>This JSP page invokes a custom tag that draws a \n");
      out.write("    panel around the contents of the tag body.  Normally, such a tag \n");
      out.write("    implementation would require a Java class with many println() statements,\n");
      out.write("    outputting HTML.  Instead, we can use a .tag file as a template,\n");
      out.write("    and we don't need to write a single line of Java or even a TLD!</p>\n");
      out.write("    <hr>\n");
      out.write("    <table border=\"0\">\n");
      out.write("      <tr valign=\"top\">\n");
      out.write("        <td>\n");
      out.write("          ");
      if (_jspx_meth_tags_005fpanel_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("        </td>\n");
      out.write("        <td>\n");
      out.write("          ");
      if (_jspx_meth_tags_005fpanel_005f1(_jspx_page_context))
        return;
      out.write("\n");
      out.write("        </td>\n");
      out.write("        <td>\n");
      out.write("          ");
      if (_jspx_meth_tags_005fpanel_005f2(_jspx_page_context))
        return;
      out.write("\n");
      out.write("        </td>\n");
      out.write("      </tr>\n");
      out.write("    </table>\n");
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

  private boolean _jspx_meth_tags_005fpanel_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  tags:panel
    org.apache.jsp.tag.web.panel_tag _jspx_th_tags_005fpanel_005f0 = new org.apache.jsp.tag.web.panel_tag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f0);
    _jspx_th_tags_005fpanel_005f0.setJspContext(_jspx_page_context);
    // /jsp/jsp2/tagfiles/panel.jsp(34,10) name = color type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f0.setColor("#ff8080");
    // /jsp/jsp2/tagfiles/panel.jsp(34,10) name = bgcolor type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f0.setBgcolor("#ffc0c0");
    // /jsp/jsp2/tagfiles/panel.jsp(34,10) name = title type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f0.setTitle("Panel 1");
    _jspx_th_tags_005fpanel_005f0.setJspBody(new Helper( 0, _jspx_page_context, _jspx_th_tags_005fpanel_005f0, null));
    _jspx_th_tags_005fpanel_005f0.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f0);
    return false;
  }

  private boolean _jspx_meth_tags_005fpanel_005f1(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  tags:panel
    org.apache.jsp.tag.web.panel_tag _jspx_th_tags_005fpanel_005f1 = new org.apache.jsp.tag.web.panel_tag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f1);
    _jspx_th_tags_005fpanel_005f1.setJspContext(_jspx_page_context);
    // /jsp/jsp2/tagfiles/panel.jsp(39,10) name = color type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f1.setColor("#80ff80");
    // /jsp/jsp2/tagfiles/panel.jsp(39,10) name = bgcolor type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f1.setBgcolor("#c0ffc0");
    // /jsp/jsp2/tagfiles/panel.jsp(39,10) name = title type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f1.setTitle("Panel 2");
    _jspx_th_tags_005fpanel_005f1.setJspBody(new Helper( 1, _jspx_page_context, _jspx_th_tags_005fpanel_005f1, null));
    _jspx_th_tags_005fpanel_005f1.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f1);
    return false;
  }

  private boolean _jspx_meth_tags_005fpanel_005f2(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  tags:panel
    org.apache.jsp.tag.web.panel_tag _jspx_th_tags_005fpanel_005f2 = new org.apache.jsp.tag.web.panel_tag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f2);
    _jspx_th_tags_005fpanel_005f2.setJspContext(_jspx_page_context);
    // /jsp/jsp2/tagfiles/panel.jsp(47,10) name = color type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f2.setColor("#8080ff");
    // /jsp/jsp2/tagfiles/panel.jsp(47,10) name = bgcolor type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f2.setBgcolor("#c0c0ff");
    // /jsp/jsp2/tagfiles/panel.jsp(47,10) name = title type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f2.setTitle("Panel 3");
    _jspx_th_tags_005fpanel_005f2.setJspBody(new Helper( 2, _jspx_page_context, _jspx_th_tags_005fpanel_005f2, null));
    _jspx_th_tags_005fpanel_005f2.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f2);
    return false;
  }

  private boolean _jspx_meth_tags_005fpanel_005f3(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  tags:panel
    org.apache.jsp.tag.web.panel_tag _jspx_th_tags_005fpanel_005f3 = new org.apache.jsp.tag.web.panel_tag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f3);
    _jspx_th_tags_005fpanel_005f3.setJspContext(_jspx_page_context);
    _jspx_th_tags_005fpanel_005f3.setParent(_jspx_parent);
    // /jsp/jsp2/tagfiles/panel.jsp(49,12) name = color type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f3.setColor("#ff80ff");
    // /jsp/jsp2/tagfiles/panel.jsp(49,12) name = bgcolor type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f3.setBgcolor("#ffc0ff");
    // /jsp/jsp2/tagfiles/panel.jsp(49,12) name = title type = java.lang.String reqTime = true required = false fragment = false deferredValue = false expectedTypeName = java.lang.String deferredMethod = false methodSignature = null
    _jspx_th_tags_005fpanel_005f3.setTitle("Inner");
    _jspx_th_tags_005fpanel_005f3.setJspBody(new Helper( 3, _jspx_page_context, _jspx_th_tags_005fpanel_005f3, null));
    _jspx_th_tags_005fpanel_005f3.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fpanel_005f3);
    return false;
  }

  private class Helper
      extends org.apache.jasper.runtime.JspFragmentHelper
  {
    private javax.servlet.jsp.tagext.JspTag _jspx_parent;
    private int[] _jspx_push_body_count;

    public Helper( int discriminator, JspContext jspContext, javax.servlet.jsp.tagext.JspTag _jspx_parent, int[] _jspx_push_body_count ) {
      super( discriminator, jspContext, _jspx_parent );
      this._jspx_parent = _jspx_parent;
      this._jspx_push_body_count = _jspx_push_body_count;
    }
    public boolean invoke0( JspWriter out ) 
      throws Throwable
    {
      out.write("\n");
      out.write("\t    First panel.<br/>\n");
      out.write("\t  ");
      return false;
    }
    public boolean invoke1( JspWriter out ) 
      throws Throwable
    {
      out.write("\n");
      out.write("\t    Second panel.<br/>\n");
      out.write("\t    Second panel.<br/>\n");
      out.write("\t    Second panel.<br/>\n");
      out.write("\t    Second panel.<br/>\n");
      out.write("\t  ");
      return false;
    }
    public boolean invoke2( JspWriter out ) 
      throws Throwable
    {
      out.write("\n");
      out.write("\t    Third panel.<br/>\n");
      out.write("            ");
      if (_jspx_meth_tags_005fpanel_005f3(_jspx_parent, _jspx_page_context))
        return true;
      out.write("\n");
      out.write("\t    Third panel.<br/>\n");
      out.write("\t  ");
      return false;
    }
    public boolean invoke3( JspWriter out ) 
      throws Throwable
    {
      out.write("\n");
      out.write("\t      A panel in a panel.\n");
      out.write("\t    ");
      return false;
    }
    public void invoke( java.io.Writer writer )
      throws JspException
    {
      JspWriter out = null;
      if( writer != null ) {
        out = this.jspContext.pushBody(writer);
      } else {
        out = this.jspContext.getOut();
      }
      try {
        this.jspContext.getELContext().putContext(JspContext.class,this.jspContext);
        switch( this.discriminator ) {
          case 0:
            invoke0( out );
            break;
          case 1:
            invoke1( out );
            break;
          case 2:
            invoke2( out );
            break;
          case 3:
            invoke3( out );
            break;
        }
      }
      catch( Throwable e ) {
        if (e instanceof SkipPageException)
            throw (SkipPageException) e;
        throw new JspException( e );
      }
      finally {
        if( writer != null ) {
          this.jspContext.popBody();
        }
      }
    }
  }
}
