/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UICategoryForm.SaveActionListener.class),
      @EventConfig(listeners = UICategoryForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UICategoryForm extends UIForm implements UIPopupComponent {

  final static public String FIELD_NAME = "name";

  final static public String FIELD_TYPE = "type";

  final static public Log LOG = ExoLogger.getLogger(UICategoryForm.class);

  public void activate() throws Exception {
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class));
    setActions(new String[] { "Save", "Cancel" });
    getUIStringInput(FIELD_NAME).setValue(null);
  }

  public void deActivate() throws Exception {}

  static public class SaveActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      UICategoryForm uiFolderForm = event.getSource();
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiFolderForm.getAncestorOfType(UIApplication.class);
      String title = uiFolderForm.getUIStringInput(FIELD_NAME).getValue();
      String name = Utils.cleanString(title);
      Node node = uiExplorer.getCurrentNode();
      if (uiExplorer.nodeIsLocked(node)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null));
        
        return;
      }
      if (name == null || name.length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null));
        
        return;
      }
      String type = "exo:taxonomy";
      try {
        Node newNode = node.addNode(Text.escapeIllegalJcrChars(name), type);
        if (newNode.canAddMixin("exo:rss-enable")) {
          newNode.addMixin("exo:rss-enable");
          newNode.setProperty("exo:title", title);
        }
        node.save();
        node.getSession().save();
        uiExplorer.updateAjax(event);
      } catch(ConstraintViolationException cve) {
        Object[] arg = { type };
        throw new MessageException(new ApplicationMessage("UIFolderForm.msg.constraint-violation",
            arg, ApplicationMessage.WARNING));
      } catch(AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.repository-exception-permission", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch(RepositoryException re) {
        String key = "";
        NodeDefinition[] definitions = node.getPrimaryNodeType().getChildNodeDefinitions();
        for (NodeDefinition def : definitions) {
          if (node.hasNode(name) || !def.allowsSameNameSiblings()) {
            key = "UIFolderForm.msg.not-allow-sameNameSibling";
          } else {
            key = "UIFolderForm.msg.repository-exception";
          }
        }
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        
        return ;
      } catch(NumberFormatException nume) {
        String key = "UIFolderForm.msg.numberformat-exception";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        
        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        if (LOG.isErrorEnabled()) {
          LOG.error("Error when create category node", e);
        }
        return;
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UICategoryForm> {
    public void execute(Event<UICategoryForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }
}
