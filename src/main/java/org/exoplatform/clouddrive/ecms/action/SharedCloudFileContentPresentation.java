
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms.action;

import org.exoplatform.wcm.ext.component.activity.ContentPresentation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

/**
 * Add extra logic to original {@link ContentPresentation} class to handle Cloud files properly.<br>
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SharedCloudFileContentPresentation.java 00000 Apr 18, 2016 pnedonosko $
 * 
 */
@Deprecated // TODO NOT USED, experimental for showing cloud file view directly in activity stream
@ComponentConfig(lifecycle = Lifecycle.class)
public class SharedCloudFileContentPresentation extends ContentPresentation {

  /**
   * Instantiates a new shared cloud file content presentation.
   */
  public SharedCloudFileContentPresentation() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UIComponent getUIComponent(String mimeType) throws Exception {
    // add node to the context
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(org.exoplatform.ecm.webui.utils.Utils.FILE_VIEWER_EXTENSION_TYPE);

    Map<String, Object> context = new HashMap<String, Object>();
    context.put(Node.class.getName(), getNode());
    context.put(org.exoplatform.ecm.webui.utils.Utils.MIME_TYPE, mimeType);

    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, this);
      if (uiComponent != null && !"Text".equals(extension.getName())) {
        return uiComponent;
      }
    }

    return super.getUIComponent(mimeType);
  }
  
  

}
