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

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.webui.form.wysiwyg.FCKEditorConfig;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
public abstract class FCKConfigPlugin extends BaseComponentPlugin {

  /**
   * This method is used to add/override some variables in fckconfig.js
   *
   * @param config the config
   * @throws Exception the exception
   */
  public abstract void addParameters(final FCKEditorConfig config, final FCKEditorContext context) throws Exception;
}
