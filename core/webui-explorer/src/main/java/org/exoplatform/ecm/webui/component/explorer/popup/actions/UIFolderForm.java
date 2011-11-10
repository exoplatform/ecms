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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeManager;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.validator.IllegalDMSCharValidator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/popup/action/UIAddFolder.gtmpl",
    events = {
      @EventConfig(listeners = UIFolderForm.SaveActionListener.class),
      @EventConfig(listeners = UIFolderForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UIFolderForm extends UIForm implements UIPopupComponent {
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_TITLE = "title" ;
  final static public String FIELD_TYPE = "type" ;
  private String allowCreateFolder_ ;

  public UIFolderForm() throws Exception {
  }

  public void activate() throws Exception {
    RequestContext context = RequestContext.getCurrentInstance() ;
    ResourceBundle res = context.getApplicationResourceBundle() ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    Node currentNode = uiExplorer.getCurrentNode();
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    String foldertypes = uiExplorer.getDriveData().getAllowCreateFolders();
    if (foldertypes.contains(",")) {
      addUIFormInput(new UIFormSelectBox(FIELD_TYPE, FIELD_TYPE, null));
      String[] arrFoldertypes = foldertypes.split(",");
      String label = "";
      NodeTypeManager ntManager = currentNode.getSession().getWorkspace().getNodeTypeManager();
      for (String foldertype : arrFoldertypes) {
        if (currentNode.isNodeType(Utils.NT_FOLDER)
            && !ntManager.getNodeType(foldertype).isNodeType(Utils.NT_FOLDER)) {
          continue;
        }
        try {
          label = res.getString(getId() + ".label." + foldertype.replace(":", "_"));
        } catch (MissingResourceException e) {
          label = foldertype;
        }
        options.add(new SelectItemOption<String>(label, foldertype));
      }
      Collections.sort(options, new ItemOptionNameComparator());
      getUIFormSelectBox(FIELD_TYPE).setOptions(options);
    } else {
      allowCreateFolder_ = foldertypes;
    }
    addUIFormInput(new UIFormStringInput(FIELD_TITLE, FIELD_TITLE, null).addValidator(MandatoryValidator.class)
                                                                        .addValidator(IllegalDMSCharValidator.class));
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).addValidator(MandatoryValidator.class)
                                                                      .addValidator(IllegalDMSCharValidator.class));
    setActions(new String[]{"Save", "Cancel"}) ;
    getUIStringInput(FIELD_NAME).setValue(null) ;
    getUIStringInput(FIELD_TITLE).setValue(null) ;
  }
  public void deActivate() throws Exception {}

  static  public class SaveActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIFolderForm uiFolderForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiFolderForm.getAncestorOfType(UIApplication.class);
      String name = uiFolderForm.getUIStringInput(FIELD_NAME).getValue() ;
      String title = uiFolderForm.getUIStringInput(FIELD_TITLE).getValue() ;
      Node node = uiExplorer.getCurrentNode() ;
      if (uiExplorer.nodeIsLocked(node)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null)) ;
        
        return ;
      }
      if(name == null || name.length() ==0) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null)) ;
        
        return ;
      }
      if (title == null || title.length() == 0) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.title-invalid", null)) ;
        
        return ;        
      }

      String type = null ;
      if(uiFolderForm.getUIFormSelectBox(FIELD_TYPE) != null) {
        type = uiFolderForm.getUIFormSelectBox(FIELD_TYPE).getValue() ;
      } else {
        type = uiFolderForm.allowCreateFolder_ ;
      }
      name = name.trim();
      title = title.trim();
      try {
        Node addedNode = node.addNode(Text.escapeIllegalJcrChars(name), type);
        if (!addedNode.hasProperty(Utils.EXO_TITLE)) {
          addedNode.addMixin(Utils.EXO_RSS_ENABLE);
        }
        addedNode.setProperty(Utils.EXO_TITLE, title);
        node.getSession().save();
        uiExplorer.updateAjax(event) ;
      } catch(ConstraintViolationException cve) {
        Object[] arg = { type } ;
        throw new MessageException(new ApplicationMessage("UIFolderForm.msg.constraint-violation",
            arg, ApplicationMessage.WARNING)) ;
      } catch(AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.repository-exception-permission", null,
            ApplicationMessage.WARNING)) ;
        
        return ;
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
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }
}
