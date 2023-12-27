package org.exoplatform.ecm.webui.form;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.utils.TimeConvertUtils;


/**
 * Created by The eXo Platform SAS
 * Author : Ha Quang Tan
 *          tanhq@exoplatform.com
 * July 15, 2013
 */
public class UIFormRichtextInput extends UIFormInputBase<String> {
  
  public static final String FULL_TOOLBAR = "CompleteWCM";
  public static final String BASIC_TOOLBAR = "Basic";
  public static final String SUPER_BASIC_TOOLBAR = "SuperBasicWCM";
  public static final String INLINE_TOOLBAR = "InlineEdit";
  public static final String COMMENT_TOOLBAR = "Comment";
  public static final String FORUM_TOOLBAR = "Forum";
  public static final String FAQ_TOOLBAR = "FAQ";
  
  public static final String ENTER_P = "1";
  public static final String ENTER_BR = "2";
  public static final String ENTER_DIV = "3";

  private static final String CKEDITOR_ENTER_P = "CKEDITOR.ENTER_P";
  private static final String CKEDITOR_ENTER_BR = "CKEDITOR.ENTER_BR";
  private static final String CKEDITOR_ENTER_DIV = "CKEDITOR.ENTER_DIV";
  
  private String width;

  private String height;

  private String toolbar;
  
  private String enterMode;
  
  private String shiftEnterMode;

  private boolean forceEnterMode = false;

  private String css;
  
  private boolean isPasteAsPlainText = false;

  private boolean isIgnoreParserHTML = false;

  public UIFormRichtextInput(String name, String bindingField, String value) {
    super(name, bindingField, String.class);
    this.value_ = value;
  }

  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode) {
    super(name, bindingField, String.class);
    this.value_ = value;
    this.enterMode = enterMode;
  }

  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode, String toolbar) {
    super(name, bindingField, String.class);
    this.value_ = value;
    this.enterMode = enterMode;
    this.toolbar = toolbar;
  }

  public UIFormRichtextInput(String name, String bindingField, String value, String enterMode, String toolbar, String css) {
    super(name, bindingField, String.class);
    this.value_ = value;
    this.enterMode = enterMode;
    this.toolbar = toolbar;
    this.css = css;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getToolbar() {
    return toolbar;
  }

  public String getEnterMode() {
    return enterMode;
  }

  public String getShiftEnterMode() {
    return shiftEnterMode;
  }

  public void setToolbar(String toolbar) {
    this.toolbar = toolbar;
  }

  public void setEnterMode(String enterMode) {
    this.enterMode = enterMode;
  }

  public void setShiftEnterMode(String shiftEnterMode) {
    this.shiftEnterMode = shiftEnterMode;
  }

  public UIFormRichtextInput setIsPasteAsPlainText(boolean isPasteAsPlainText) {
    this.isPasteAsPlainText = isPasteAsPlainText;
    return this;
  }

  public boolean getIsPasteAsPlainText() {
    return this.isPasteAsPlainText;
  }

  public boolean isIgnoreParserHTML() {
    return isIgnoreParserHTML;
  }

  public UIFormRichtextInput setIgnoreParserHTML(boolean isIgnoreParserHTML) {
    this.isIgnoreParserHTML = isIgnoreParserHTML;
    return this;
  }

  public void setCss(String css) {
    this.css = css;
  }

  public String getCss() {
    return css;
  }

  private static String encodeURLComponent(String s) {
    String result = null;
    try {
      result = URLEncoder.encode(s, "UTF-8")
                         .replaceAll("\\+", "%20")
                         .replaceAll("\\%21", "!")
                         .replaceAll("\\%28", "(")
                         .replaceAll("\\%29", ")")
                         .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      result = s;
    }
    return result;
  }

  private String buildEditorLayout(WebuiRequestContext context) throws Exception {
    if (toolbar == null) toolbar = BASIC_TOOLBAR;
    if (width == null) width = "98%";
    if (height == null) height = "'200px'";
    if (enterMode == null) enterMode = CKEDITOR_ENTER_P;
    if (shiftEnterMode == null) shiftEnterMode = CKEDITOR_ENTER_BR;
    if (CKEDITOR_ENTER_P.equals(enterMode) && CKEDITOR_ENTER_DIV.equals(shiftEnterMode)
       || CKEDITOR_ENTER_DIV.equals(enterMode) && CKEDITOR_ENTER_P.equals(shiftEnterMode)) {
      forceEnterMode = true;
    }
    if (css == null) css = "\"/commons-extension/ckeditor/contents.css\"";
    if (value_ == null) value_ = "";

    StringBuilder builder = new StringBuilder();
    builder.append("<div class=\"clearfix\">");
    builder.append("  <span style=\"float:left; width:").append(width).append(";\">");
    //
    builder.append("  <textarea style=\"width:1px;height:1px;\" id=\"").append(name).append("\" name=\"").append(name).append("\">")
          .append(value_).append("</textarea>\n");
    builder.append("  </span>");
    if (isMandatory()) {
      builder.append("  <span style=\"float:left\"> &nbsp;*</span>");
    }
    builder.append("</div>");
    
    StringBuilder jsBuilder = new StringBuilder();
    //fix issue INTEG-320
    String str = name;
    String variableName = TimeConvertUtils.santializeJavaVariable(str);
    String textArea = "textarea" + variableName;
    String textArea1 = "textarea" + variableName;
    String instance = "instance" + variableName;
    String form = "form" + variableName;
    String functionName = "ckeditorGenerate" + variableName;
    jsBuilder.append("function " + functionName + "() {");
    jsBuilder.append(" var " + textArea +  " = document.getElementById('").append(name).append("'); ");
    if (isIgnoreParserHTML() && StringUtils.isNotEmpty(value_)) {
      String value = encodeURLComponent(value_);
      jsBuilder.append(" if(" + textArea + ") {")
             .append("   var isFirefox = typeof InstallTrigger !== 'undefined';")
             .append("   var value = decodeURIComponent('").append(value).append("');")
             .append("   if(isFirefox) { " + textArea + ".value = value; } else { " + textArea + ".innerText = value;}")
             .append(" }");
    }
    jsBuilder
            .append("var " + instance + " = CKEDITOR.instances['").append(name).append("'];\n")
            .append("if (" + instance + ") { ")
            .append("   CKEDITOR.remove(" + instance + "); " + instance + " = null;\n")
            .append("}\n")
            .append("$('[name=\\'").append(name).append("\\']').ckeditor({")
            .append("customConfig: '/ecmexplorer/javascript/eXo/ecm/ckeditorCustom/config.js',")
            .append("removePlugins: 'hideBottomToolbar',")
            .append("toolbar:'").append(toolbar).append("',")
            .append("toolbarLocation: 'top',")
            .append("height:").append(height).append(", contentsCss:").append(css).append(", enterMode:").append(enterMode)
            .append((isPasteAsPlainText) ? ", forcePasteAsPlainText: true" : "")
            .append(", forceEnterMode:").append(forceEnterMode)
            .append(", shiftEnterMode:").append(shiftEnterMode).append("});\n")
            .append(instance + " = CKEDITOR.instances['").append(name).append("'];\n")
            .append(instance +".on( 'change', function(e) { \n")
            .append("   document.getElementById('").append(name).append("').value = " + instance + ".getData(); \n")
            .append("});\n")
            
            .append("var " + form + " = " + textArea + "; \n")
            .append("while ("+ form + " && (" + form + ".nodeName.toLowerCase() != 'form')) { \n")
            .append("   "+ form +" = " + form + ".parentNode;\n")
            .append("} \n")
            .append("if (" + form + ") {\n")
            .append("   " + form + ".textareaName = '").append(name).append("'; \n")
            .append("   " + form + ".onmouseover=function() { \n")
            .append("     this.onmouseover=''; \n")
            .append("     var " + textArea1 + " = document.getElementById('").append(name).append("');  \n")  
            .append("     " + textArea1 + ".style.display='block'; \n")
            .append("     " + textArea1 + ".style.visibility='visible'; \n")
            .append("     " + textArea1 + ".focus(); \n")
            .append("     " + textArea1 + ".style.display='none'; \n")
            .append("   } \n")
            .append("} \n");   
    //end function   
    jsBuilder.append("}\n");
    jsBuilder.append(functionName + "();\n");
    context.getJavascriptManager().require("SHARED/commons-editor", "editor").require("SHARED/jquery", "$").addScripts(jsBuilder.toString());
    //
    return builder.toString();
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    //
    context.getWriter().write(buildEditorLayout(context));
  }

  public void decode(Object input, WebuiRequestContext context) {
    value_ = (String) input;
    if (value_ != null && value_.length() == 0) {
      value_ = null;
    }
  }
 
}
