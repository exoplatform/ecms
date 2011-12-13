/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.form;

import java.util.Collection;

import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * May 10, 2010
 */
public class UIFormRichtextInput extends UIFormInputBase<String> {

  public static final String FULL_TOOLBAR = "Full";

  public static final String BASIC_TOOLBAR = "Basic";

  private String width;

  private String height;

  private String toolbar;

  public UIFormRichtextInput(String name, String bindingField, String value) {
    super(name, bindingField, String.class);
    this.value_ = value;
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

  public void setToolbar(String toolbar) {
    this.toolbar = toolbar;
  }

  public void processRender(WebuiRequestContext context) throws Exception {

    if (toolbar == null) toolbar = BASIC_TOOLBAR;
    if (width == null) width = "'100%'";
    if (height == null) height = "200";
    StringBuffer contentsCss = new StringBuffer();
    contentsCss.append("[");
    SkinService skinService = WCMCoreUtils.getService(SkinService.class);
    String skin = Util.getUIPortalApplication().getUserPortalConfig().getPortalConfig().getSkin();
    String portal = Util.getUIPortal().getName();
    Collection<SkinConfig> portalSkins = skinService.getPortalSkins(skin);
    SkinConfig customSkin = skinService.getSkin(portal, Util.getUIPortalApplication()
                                                            .getUserPortalConfig()
                                                            .getPortalConfig()
                                                            .getSkin());
    if (customSkin != null) portalSkins.add(customSkin);
    for (SkinConfig portalSkin : portalSkins) {
      contentsCss.append("'").append(portalSkin.createURL()).append("',");
    }
    contentsCss.delete(contentsCss.length() - 1, contentsCss.length());
    contentsCss.append("]");

    StringBuffer buffer = new StringBuffer();
    buffer.append("<div>");
    buffer.append("<span style='float:left; width:98%;'>");
    if (value_!=null) {
      buffer.append("<textarea id='" + name + "' name='" + name + "'>" + value_ + "</textarea>\n");
    }else {
      buffer.append("<textarea id='" + name + "' name='" + name + "'></textarea>\n");
    }
    
    buffer.append("<script type='text/javascript'>\n");
    buffer.append("  //<![CDATA[\n");
    buffer.append("    var instances = CKEDITOR.instances['" + name + "']; if (instances) instances.destroy(true);\n");
    buffer.append("    CKEDITOR.replace('" + name + "', {toolbar:'" + toolbar + "', height:"
        + height + ", contentsCss:" + contentsCss + "});\n");
    buffer.append("  //]]>\n");
    buffer.append("</script>\n");
    buffer.append("</span>");
    if (isMandatory()) {
      buffer.append("<span style='float:left'> &nbsp;*</span>");
    }
    
    buffer.append("</div>");    
    context.getWriter().write(buffer.toString());
  }

  public void decode(Object input, WebuiRequestContext context) throws Exception {
    value_ = (String)input;
    if (value_ != null && value_.length() == 0)
       value_ = null;
  }

}
