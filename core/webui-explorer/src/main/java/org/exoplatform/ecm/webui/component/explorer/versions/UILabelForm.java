/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.versions;

import javax.jcr.Node;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Design : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * Implement: lxchiati
 *            lebienthuy@gmail.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UILabelForm.SaveActionListener.class),
      @EventConfig(listeners = UILabelForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)

public class UILabelForm extends UIForm {

  private static  String FIELD_LABEL = "label" ;

  public UILabelForm() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_LABEL , FIELD_LABEL , null).addValidator(MandatoryValidator.class));
  }

  @SuppressWarnings("unused")
  static  public class SaveActionListener extends EventListener<UILabelForm> {
    public void execute(Event<UILabelForm> event) throws Exception {
      UILabelForm uiLabelForm = event.getSource();
      String label = uiLabelForm.getUIStringInput(FIELD_LABEL).getValue().trim();
      UIVersionInfo uiVersionInfo = uiLabelForm.getParent();
      VersionNode currentVersion = uiVersionInfo.getCurrentVersionNode();
      UIJCRExplorer uiExplorer = uiLabelForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiLabelForm.getAncestorOfType(UIApplication.class) ;
      Node currentNode = uiExplorer.getCurrentNode() ;
      if(!Utils.isNameValid(label, Utils.SPECIALCHARACTER)) {
        uiApp.addMessage(new ApplicationMessage("UILabelForm.msg.label-invalid",
            null, ApplicationMessage.WARNING)) ;
        
        return ;
      }
      try{
        currentNode.getVersionHistory().addVersionLabel(currentVersion.getName(), label, false) ;
      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UILabelForm.msg.label-exist", new Object[]{label})) ;
        
        return ;
      }
      uiLabelForm.reset() ;
      uiLabelForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiVersionInfo) ;
    }
  }

  @SuppressWarnings("unused")
  static  public class CancelActionListener extends EventListener<UILabelForm> {
    public void execute(Event<UILabelForm> event) throws Exception {
      UILabelForm uiLabelForm = event.getSource();
      uiLabelForm.reset() ;
      uiLabelForm.setRendered(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiLabelForm.getParent()) ;
    }
  }
}

