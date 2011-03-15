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
package org.exoplatform.services.ecm.fckconfig;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.webui.form.wysiwyg.FCKEditorConfig;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public class FCKConfigService {

  private List<FCKConfigPlugin> fckConfigPlugins = new ArrayList<FCKConfigPlugin>();

  public FCKConfigService() { }

  /**
   * Adds the FCKConfigPlugin.
   *
   * @param plugin the FCKConfigPlugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    if(plugin instanceof FCKConfigPlugin) {
      fckConfigPlugins.add(FCKConfigPlugin.class.cast(plugin));
    }
  }

  /**
   * Use to configure the fckeditoConfig by via FCKConfigPlugin.
   *
   * @param editorConfig the FCKEditorConfig
   * @throws Exception the exception
   */
  public void processFCKEditorConfig(final FCKEditorConfig editorConfig, final FCKEditorContext context) throws Exception{
    for(FCKConfigPlugin plugin: fckConfigPlugins) {
      plugin.addParameters(editorConfig,context);
    }
  }
}
