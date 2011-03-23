/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.fckconfig;

import java.util.Collection;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.services.ecm.fckconfig.FCKConfigPlugin;
import org.exoplatform.services.ecm.fckconfig.FCKEditorContext;
import org.exoplatform.webui.form.wysiwyg.FCKEditorConfig;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong_phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Aug 28, 2008
 */
public class FCKDynamicSkinPlugin extends FCKConfigPlugin {

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.ecm.fckconfig.FCKConfigPlugin#addParameters(org
   * .exoplatform.webui.form.wysiwyg.FCKEditorConfig,
   * org.exoplatform.services.ecm.fckconfig.FCKEditorContext)
   */
  public void addParameters(FCKEditorConfig editorConfig, FCKEditorContext editorContext) throws Exception {
    StringBuffer cssMergedBuffer = new StringBuffer();
    ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
    SkinService skinService = (SkinService) exoContainer.getComponentInstanceOfType(SkinService.class);
    Collection<SkinConfig> collecionSkin = skinService.getPortalSkins(editorContext.getSkinName());
    for (SkinConfig skinConfig : collecionSkin) {
      cssMergedBuffer = cssMergedBuffer.append(skinConfig.getCSSPath()).append(",");
    }
    SkinConfig skinConfig = skinService.getSkin(editorContext.getPortalName(), editorContext.getSkinName());
    if (skinConfig != null) {
      cssMergedBuffer = cssMergedBuffer.append(skinConfig.getCSSPath());
    }
    String cssMerged = cssMergedBuffer.toString();
    if (cssMerged.endsWith(","))
      cssMerged = cssMerged.substring(0, cssMerged.length() - 1);

    editorConfig.put("EditorAreaCSS", cssMerged);
  }
}
