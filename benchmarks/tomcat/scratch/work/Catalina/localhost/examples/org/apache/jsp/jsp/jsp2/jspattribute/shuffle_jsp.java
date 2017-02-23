package org.apache.jsp.jsp.jsp2.jspattribute;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;

public final class shuffle_jsp extends org.apache.jasper.runtime.HttpJspBase
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
      out.write("\n");
      out.write("<html>\n");
      out.write("  <head>\n");
      out.write("    <title>JSP 2.0 Examples - Shuffle Example</title>\n");
      out.write("  </head>\n");
      out.write("  <body>\n");
      out.write("    <h1>JSP 2.0 Examples - Shuffle Example</h1>\n");
      out.write("    <hr>\n");
      out.write("    <p>Try reloading the page a few times.  Both the rows and the columns\n");
      out.write("    are shuffled and appear different each time.</p>\n");
      out.write("    <p>Here's how the code works.  The SimpleTag handler called \n");
      out.write("    &lt;my:shuffle&gt; accepts three attributes.  Each attribute is a \n");
      out.write("    JSP Fragment, meaning it is a fragment of JSP code that can be\n");
      out.write("    dynamically executed by the shuffle tag handler on demand.  The \n");
      out.write("    shuffle tag handler executes the three fragments in a random order.\n");
      out.write("    To shuffle both the rows and the columns, the shuffle tag is used\n");
      out.write("    with itself as a parameter.</p>\n");
      out.write("    <hr>\n");
      out.write("    <blockquote>\n");
      out.write("     <font color=\"#ffffff\">\n");
      out.write("      <table>\n");
      out.write("        ");
      if (_jspx_meth_my_005fshuffle_005f0(_jspx_page_context))
        return;
      out.write("\n");
      out.write("      </table>\n");
      out.write("     </font>\n");
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

  private boolean _jspx_meth_my_005fshuffle_005f0(PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:shuffle
    jsp2.examples.simpletag.ShuffleSimpleTag _jspx_th_my_005fshuffle_005f0 = new jsp2.examples.simpletag.ShuffleSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f0);
    _jspx_th_my_005fshuffle_005f0.setJspContext(_jspx_page_context);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp0 = new Helper( 0, _jspx_page_context, _jspx_th_my_005fshuffle_005f0, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(39,8) null
    _jspx_th_my_005fshuffle_005f0.setFragment1(_jspx_temp0);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp4 = new Helper( 4, _jspx_page_context, _jspx_th_my_005fshuffle_005f0, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(39,8) null
    _jspx_th_my_005fshuffle_005f0.setFragment2(_jspx_temp4);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp8 = new Helper( 8, _jspx_page_context, _jspx_th_my_005fshuffle_005f0, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(39,8) null
    _jspx_th_my_005fshuffle_005f0.setFragment3(_jspx_temp8);
    _jspx_th_my_005fshuffle_005f0.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f0);
    return false;
  }

  private boolean _jspx_meth_my_005fshuffle_005f1(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:shuffle
    jsp2.examples.simpletag.ShuffleSimpleTag _jspx_th_my_005fshuffle_005f1 = new jsp2.examples.simpletag.ShuffleSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f1);
    _jspx_th_my_005fshuffle_005f1.setJspContext(_jspx_page_context);
    _jspx_th_my_005fshuffle_005f1.setParent(_jspx_parent);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp1 = new Helper( 1, _jspx_page_context, _jspx_th_my_005fshuffle_005f1, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(42,14) null
    _jspx_th_my_005fshuffle_005f1.setFragment1(_jspx_temp1);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp2 = new Helper( 2, _jspx_page_context, _jspx_th_my_005fshuffle_005f1, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(42,14) null
    _jspx_th_my_005fshuffle_005f1.setFragment2(_jspx_temp2);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp3 = new Helper( 3, _jspx_page_context, _jspx_th_my_005fshuffle_005f1, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(42,14) null
    _jspx_th_my_005fshuffle_005f1.setFragment3(_jspx_temp3);
    _jspx_th_my_005fshuffle_005f1.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f1);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f0(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f0 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f0);
    _jspx_th_my_005ftile_005f0.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f0.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(44,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f0.setColor("#ff0000");
    // /jsp/jsp2/jspattribute/shuffle.jsp(44,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f0.setLabel("A");
    _jspx_th_my_005ftile_005f0.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f0);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f1(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f1 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f1);
    _jspx_th_my_005ftile_005f1.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f1.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(47,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f1.setColor("#00ff00");
    // /jsp/jsp2/jspattribute/shuffle.jsp(47,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f1.setLabel("B");
    _jspx_th_my_005ftile_005f1.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f1);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f2(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f2 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f2);
    _jspx_th_my_005ftile_005f2.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f2.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(50,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f2.setColor("#0000ff");
    // /jsp/jsp2/jspattribute/shuffle.jsp(50,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f2.setLabel("C");
    _jspx_th_my_005ftile_005f2.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f2);
    return false;
  }

  private boolean _jspx_meth_my_005fshuffle_005f2(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:shuffle
    jsp2.examples.simpletag.ShuffleSimpleTag _jspx_th_my_005fshuffle_005f2 = new jsp2.examples.simpletag.ShuffleSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f2);
    _jspx_th_my_005fshuffle_005f2.setJspContext(_jspx_page_context);
    _jspx_th_my_005fshuffle_005f2.setParent(_jspx_parent);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp5 = new Helper( 5, _jspx_page_context, _jspx_th_my_005fshuffle_005f2, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(57,14) null
    _jspx_th_my_005fshuffle_005f2.setFragment1(_jspx_temp5);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp6 = new Helper( 6, _jspx_page_context, _jspx_th_my_005fshuffle_005f2, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(57,14) null
    _jspx_th_my_005fshuffle_005f2.setFragment2(_jspx_temp6);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp7 = new Helper( 7, _jspx_page_context, _jspx_th_my_005fshuffle_005f2, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(57,14) null
    _jspx_th_my_005fshuffle_005f2.setFragment3(_jspx_temp7);
    _jspx_th_my_005fshuffle_005f2.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f2);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f3(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f3 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f3);
    _jspx_th_my_005ftile_005f3.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f3.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(59,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f3.setColor("#ff0000");
    // /jsp/jsp2/jspattribute/shuffle.jsp(59,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f3.setLabel("1");
    _jspx_th_my_005ftile_005f3.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f3);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f4(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f4 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f4);
    _jspx_th_my_005ftile_005f4.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f4.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(62,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f4.setColor("#00ff00");
    // /jsp/jsp2/jspattribute/shuffle.jsp(62,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f4.setLabel("2");
    _jspx_th_my_005ftile_005f4.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f4);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f5(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f5 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f5);
    _jspx_th_my_005ftile_005f5.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f5.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(65,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f5.setColor("#0000ff");
    // /jsp/jsp2/jspattribute/shuffle.jsp(65,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f5.setLabel("3");
    _jspx_th_my_005ftile_005f5.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f5);
    return false;
  }

  private boolean _jspx_meth_my_005fshuffle_005f3(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:shuffle
    jsp2.examples.simpletag.ShuffleSimpleTag _jspx_th_my_005fshuffle_005f3 = new jsp2.examples.simpletag.ShuffleSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f3);
    _jspx_th_my_005fshuffle_005f3.setJspContext(_jspx_page_context);
    _jspx_th_my_005fshuffle_005f3.setParent(_jspx_parent);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp9 = new Helper( 9, _jspx_page_context, _jspx_th_my_005fshuffle_005f3, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(72,14) null
    _jspx_th_my_005fshuffle_005f3.setFragment1(_jspx_temp9);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp10 = new Helper( 10, _jspx_page_context, _jspx_th_my_005fshuffle_005f3, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(72,14) null
    _jspx_th_my_005fshuffle_005f3.setFragment2(_jspx_temp10);
    javax.servlet.jsp.tagext.JspFragment _jspx_temp11 = new Helper( 11, _jspx_page_context, _jspx_th_my_005fshuffle_005f3, null);
    // /jsp/jsp2/jspattribute/shuffle.jsp(72,14) null
    _jspx_th_my_005fshuffle_005f3.setFragment3(_jspx_temp11);
    _jspx_th_my_005fshuffle_005f3.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005fshuffle_005f3);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f6(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f6 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f6);
    _jspx_th_my_005ftile_005f6.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f6.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(74,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f6.setColor("#ff0000");
    // /jsp/jsp2/jspattribute/shuffle.jsp(74,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f6.setLabel("!");
    _jspx_th_my_005ftile_005f6.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f6);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f7(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f7 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f7);
    _jspx_th_my_005ftile_005f7.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f7.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(77,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f7.setColor("#00ff00");
    // /jsp/jsp2/jspattribute/shuffle.jsp(77,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f7.setLabel("@");
    _jspx_th_my_005ftile_005f7.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f7);
    return false;
  }

  private boolean _jspx_meth_my_005ftile_005f8(javax.servlet.jsp.tagext.JspTag _jspx_parent, PageContext _jspx_page_context)
          throws Throwable {
    PageContext pageContext = _jspx_page_context;
    JspWriter out = _jspx_page_context.getOut();
    //  my:tile
    jsp2.examples.simpletag.TileSimpleTag _jspx_th_my_005ftile_005f8 = new jsp2.examples.simpletag.TileSimpleTag();
    org.apache.jasper.runtime.AnnotationHelper.postConstruct(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f8);
    _jspx_th_my_005ftile_005f8.setJspContext(_jspx_page_context);
    _jspx_th_my_005ftile_005f8.setParent(_jspx_parent);
    // /jsp/jsp2/jspattribute/shuffle.jsp(80,18) name = color type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f8.setColor("#0000ff");
    // /jsp/jsp2/jspattribute/shuffle.jsp(80,18) name = label type = java.lang.String reqTime = false required = true fragment = false deferredValue = false expectedTypeName = null deferredMethod = false methodSignature = null
    _jspx_th_my_005ftile_005f8.setLabel("#");
    _jspx_th_my_005ftile_005f8.doTag();
    org.apache.jasper.runtime.AnnotationHelper.preDestroy(_jsp_annotationprocessor, _jspx_th_my_005ftile_005f8);
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
      out.write("<tr>\n");
      out.write("              ");
      if (_jspx_meth_my_005fshuffle_005f1(_jspx_parent, _jspx_page_context))
        return true;
      out.write("\n");
      out.write("            </tr>");
      return false;
    }
    public boolean invoke1( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f0(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke2( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f1(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke3( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f2(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke4( JspWriter out ) 
      throws Throwable
    {
      out.write("<tr>\n");
      out.write("              ");
      if (_jspx_meth_my_005fshuffle_005f2(_jspx_parent, _jspx_page_context))
        return true;
      out.write("\n");
      out.write("            </tr>");
      return false;
    }
    public boolean invoke5( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f3(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke6( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f4(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke7( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f5(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke8( JspWriter out ) 
      throws Throwable
    {
      out.write("<tr>\n");
      out.write("              ");
      if (_jspx_meth_my_005fshuffle_005f3(_jspx_parent, _jspx_page_context))
        return true;
      out.write("\n");
      out.write("            </tr>");
      return false;
    }
    public boolean invoke9( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f6(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke10( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f7(_jspx_parent, _jspx_page_context))
        return true;
      return false;
    }
    public boolean invoke11( JspWriter out ) 
      throws Throwable
    {
      if (_jspx_meth_my_005ftile_005f8(_jspx_parent, _jspx_page_context))
        return true;
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
          case 4:
            invoke4( out );
            break;
          case 5:
            invoke5( out );
            break;
          case 6:
            invoke6( out );
            break;
          case 7:
            invoke7( out );
            break;
          case 8:
            invoke8( out );
            break;
          case 9:
            invoke9( out );
            break;
          case 10:
            invoke10( out );
            break;
          case 11:
            invoke11( out );
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
