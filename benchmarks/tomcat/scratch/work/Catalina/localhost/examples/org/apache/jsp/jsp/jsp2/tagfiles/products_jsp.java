package org.apache.jsp.jsp.jsp2.tagfiles;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class products_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

  private static final JspFactory _jspxFactory = JspFactory.getDefaultFactory();

  private static java.util.List _jspx_dependants;

  static {
    _jspx_dependants = new java.util.ArrayList(1);
    _jspx_dependants.add("/WEB-INF/tags/displayProducts.tag");
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
      out.write("    <title>JSP 2.0 Examples - Display Products Tag File</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Examples - Display Products Tag File</h1>\n");
      out.write("    <hr>\n");
      out.write("    <p>This JSP page invokes a tag file that displays a listing of \n");
      out.write("    products.  The custom tag accepts two fragments that enable\n");
      out.write("    customization of appearance.  One for when the product is on sale\n");
      out.write("    and one for normal price.</p>\n");
      out.write("    <p>The tag is invoked twice, using different styles</p>\n");
      out.write("    <hr>\n");
      out.write("    <h2>Products</h2>\n");
      out.write("    ");
      //  tags:displayProducts
      org.apache.jsp.tag.web.displayProducts_tag _jspx_th_tags_005fdisplayProducts_005f0 = new org.apache.jsp.tag.web.displayProducts_tag();
      org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fdisplayProducts_005f0);
      _jspx_th_tags_005fdisplayProducts_005f0.setJspContext(_jspx_page_context);
      javax.servlet.jsp.tagext.JspFragment _jspx_temp0 = new Helper( 0, _jspx_page_context, _jspx_th_tags_005fdisplayProducts_005f0, null);
      // /jsp/jsp2/tagfiles/products.jsp(32,4) null
      _jspx_th_tags_005fdisplayProducts_005f0.setNormalPrice(_jspx_temp0);
      javax.servlet.jsp.tagext.JspFragment _jspx_temp1 = new Helper( 1, _jspx_page_context, _jspx_th_tags_005fdisplayProducts_005f0, null);
      // /jsp/jsp2/tagfiles/products.jsp(32,4) null
      _jspx_th_tags_005fdisplayProducts_005f0.setOnSale(_jspx_temp1);
      _jspx_th_tags_005fdisplayProducts_005f0.doTag();
      org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fdisplayProducts_005f0);
      out.write("\n");
      out.write("    <hr>\n");
      out.write("    <h2>Products (Same tag, alternate style)</h2>\n");
      out.write("    ");
      //  tags:displayProducts
      org.apache.jsp.tag.web.displayProducts_tag _jspx_th_tags_005fdisplayProducts_005f1 = new org.apache.jsp.tag.web.displayProducts_tag();
      org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_tags_005fdisplayProducts_005f1);
      _jspx_th_tags_005fdisplayProducts_005f1.setJspContext(_jspx_page_context);
      javax.servlet.jsp.tagext.JspFragment _jspx_temp2 = new Helper( 2, _jspx_page_context, _jspx_th_tags_005fdisplayProducts_005f1, null);
      // /jsp/jsp2/tagfiles/products.jsp(45,4) null
      _jspx_th_tags_005fdisplayProducts_005f1.setNormalPrice(_jspx_temp2);
      javax.servlet.jsp.tagext.JspFragment _jspx_temp3 = new Helper( 3, _jspx_page_context, _jspx_th_tags_005fdisplayProducts_005f1, null);
      // /jsp/jsp2/tagfiles/products.jsp(45,4) null
      _jspx_th_tags_005fdisplayProducts_005f1.setOnSale(_jspx_temp3);
      _jspx_th_tags_005fdisplayProducts_005f1.doTag();
      org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_tags_005fdisplayProducts_005f1);
      out.write("\n");
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
    public void invoke0( JspWriter out ) 
      throws Throwable
    {
      out.write("Item: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${name}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("<br/>\n");
      out.write("\tPrice: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${price}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      return;
    }
    public void invoke1( JspWriter out ) 
      throws Throwable
    {
      out.write("Item: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${name}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("<br/>\n");
      out.write("\t<font color=\"red\"><strike>Was: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${origPrice}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</strike></font><br/>\n");
      out.write("\t<b>Now: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${salePrice}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</b>");
      return;
    }
    public void invoke2( JspWriter out ) 
      throws Throwable
    {
      out.write('<');
      out.write('b');
      out.write('>');
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${name}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</b> @ ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${price}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write(" ea.");
      return;
    }
    public void invoke3( JspWriter out ) 
      throws Throwable
    {
      out.write('<');
      out.write('b');
      out.write('>');
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${name}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write("</b> @ ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${salePrice}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write(" ea. (was: ");
      out.write((java.lang.String) org.apache.jasper.runtime.PageContextImpl.proprietaryEvaluate("${origPrice}", java.lang.String.class, (PageContext)_jspx_page_context, null, false));
      out.write(')');
      return;
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
