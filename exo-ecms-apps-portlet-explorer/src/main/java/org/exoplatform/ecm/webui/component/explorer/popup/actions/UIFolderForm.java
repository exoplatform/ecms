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
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

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
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UIFolderForm.SaveActionListener.class),
      @EventConfig(listeners = UIFolderForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UIFolderForm extends UIForm implements UIPopupComponent {
  final static public String FIELD_NAME = "name" ;
  final static public String FIELD_TYPE = "type" ;
  private String allowCreateFolder_ ;

  public UIFolderForm() throws Exception {
  }

  public void activate() throws Exception { 
    RequestContext context = RequestContext.getCurrentInstance() ;
    ResourceBundle res = context.getApplicationResourceBundle() ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    String foldertypes = uiExplorer.getDriveData().getAllowCreateFolders();
    if (foldertypes.contains(",")) {
      addUIFormInput(new UIFormSelectBox(FIELD_TYPE, FIELD_TYPE, null));
      String[] arrFoldertypes = foldertypes.split(",");
      for (String foldertype : arrFoldertypes) {
        options.add(new SelectItemOption<String>(res.getString(getId() + ".label." + foldertype.replace(":", "_")),  foldertype));
      }
      Collections.sort(options, new ItemOptionNameComparator());
      getUIFormSelectBox(FIELD_TYPE).setOptions(options);
    } else {
      allowCreateFolder_ = foldertypes;
    }
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null
        ).addValidator(MandatoryValidator.class).addValidator(IllegalDMSCharValidator.class));
    setActions(new String[]{"Save", "Cancel"}) ;
    getUIStringInput(FIELD_NAME).setValue(null) ;
    if (getUIFormSelectBox(FIELD_TYPE) != null) {
      if (uiExplorer.getCurrentNode().isNodeType(Utils.NT_FOLDER)) {
        if (getAncestorOfType(UIJCRExplorer.class).getCurrentNode().isNodeType(Utils.NT_FOLDER)) {
          options.clear();
          options.add(new SelectItemOption<String>(res.getString(getId() + ".label." + Utils.NT_FOLDER.replace(":", "_")), Utils.NT_FOLDER));
          Collections.sort(options, new ItemOptionNameComparator());
          getUIFormSelectBox(FIELD_TYPE).setOptions(options);
        }
      }
    }
  }
  public void deActivate() throws Exception {}

  static  public class SaveActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIFolderForm uiFolderForm = event.getSource() ;
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiFolderForm.getAncestorOfType(UIApplication.class);
      String name = uiFolderForm.getUIStringInput(FIELD_NAME).getValue() ;
      Node node = uiExplorer.getCurrentNode() ;                  
      if (uiExplorer.nodeIsLocked(node)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }      
      if(name == null || name.length() ==0) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      }      
      
      String type = null ;
      if(uiFolderForm.getUIFormSelectBox(FIELD_TYPE) != null) {
        type = uiFolderForm.getUIFormSelectBox(FIELD_TYPE).getValue() ;
      } else {
        type = uiFolderForm.allowCreateFolder_ ;
      }
      try {
        node.addNode(Text.escapeIllegalJcrChars(name), type);
        node.save();
        node.getSession().save();
        if(!uiExplorer.getPreference().isJcrEnable())  { node.getSession().save() ; }
        uiExplorer.updateAjax(event) ;
      } catch(ConstraintViolationException cve) {  
        Object[] arg = { type } ;
        throw new MessageException(new ApplicationMessage("UIFolderForm.msg.constraint-violation",
            arg, ApplicationMessage.WARNING)) ;
      } catch(AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.repository-exception-permission", null, 
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
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
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return ;
      } catch(NumberFormatException nume) {
        String key = "UIFolderForm.msg.numberformat-exception";
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
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