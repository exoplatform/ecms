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
package org.exoplatform.ecm.webui.form.field;

import org.exoplatform.ecm.webui.form.DialogFormField;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormRichtextInput;

import java.util.Collection;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * May 10, 2010
 */
public class UIFormRichtextField extends DialogFormField {

  private final String TOOLBAR = "toolbar";
  private final String WIDTH = "width";
  private final String HEIGHT = "height";
  private final String ENTERMODE = "enterMode";
  private final String SHIFT_ENTER_MODE = "shiftEnterMode";

  private String toolbar;
  private String width;
  private String height;
  private String enterMode;
  private String shiftEnterMode;

  public UIFormRichtextField(String name, String label, String[] arguments) {
    super(name, label, arguments);
  }

  @SuppressWarnings("unchecked")
  public <T extends UIFormInputBase> T createUIFormInput() throws Exception {
    UIFormRichtextInput richtext = new UIFormRichtextInput(name, name, defaultValue);
    setPredefineOptions();
    richtext.setToolbar(toolbar);
    richtext.setWidth(width);
    richtext.setHeight(height);
    richtext.setEnterMode(enterMode);
    richtext.setShiftEnterMode(shiftEnterMode);
    richtext.setIgnoreParserHTML(true);

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
      contentsCss.append("'").append(portalSkin.createURL(Util.getPortalRequestContext().getControllerContext())).append("',");
    }
    contentsCss.append("'/commons-extension/ckeditor/contents.css'");
    contentsCss.append("]");    
    richtext.setCss(contentsCss.toString());
    
    if(validateType != null) {
      DialogFormUtil.addValidators(richtext, validateType);
    }
    return (T)richtext;
  }

  private void setPredefineOptions() {
    if (options == null) return;
    for(String option: options.split(",")) {
      String[] entry = option.split(":");
      if(TOOLBAR.equals(entry[0])) {
        toolbar = entry[1];
      } else if(WIDTH.equals(entry[0])) {
        width = entry[1];
      } else if(HEIGHT.equals(entry[0])) {
        height = entry[1];
      } else if(ENTERMODE.equals(entry[0])) {
        enterMode = entry[1];
      } else if(SHIFT_ENTER_MODE.equals(entry[0])) {
        shiftEnterMode = entry[1];
      }
    }
  }

}
