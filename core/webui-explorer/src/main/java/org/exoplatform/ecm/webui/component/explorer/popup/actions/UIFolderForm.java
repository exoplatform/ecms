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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
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
import org.exoplatform.webui.form.input.UICheckBoxInput;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/popup/action/UIAddFolder.gtmpl",
    events = {
      @EventConfig(listeners = UIFolderForm.SaveActionListener.class),
      @EventConfig(listeners = UIFolderForm.OnChangeActionListener.class),
      @EventConfig(listeners = UIFolderForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)

public class UIFolderForm extends UIForm implements UIPopupComponent {
  public static final String FIELD_TITLE_TEXT_BOX = "titleTextBox";
  public static final String FIELD_CUSTOM_TYPE_CHECK_BOX = "customTypeCheckBox";
  public static final String FIELD_CUSTOM_TYPE_SELECT_BOX = "customTypeSelectBox";

  private static final Log LOG  = ExoLogger.getLogger(UIFolderForm.class.getName());
  private static final String DEFAULT_NAME = "untitled";
  private static final String MANAGED_SITES = "Managed Sites";

  private String selectedType;

  /**
   * Constructor.
   *
   * @throws Exception
   */
  public UIFolderForm() throws Exception {
    // Title checkbox
    UIFormStringInput titleTextBox = new UIFormStringInput(FIELD_TITLE_TEXT_BOX, FIELD_TITLE_TEXT_BOX, null);
    this.addUIFormInput(titleTextBox);

    // Custom type checkbox
    UICheckBoxInput customTypeCheckBox = new UICheckBoxInput(FIELD_CUSTOM_TYPE_CHECK_BOX, FIELD_CUSTOM_TYPE_CHECK_BOX, false);
    customTypeCheckBox.setRendered(false);
    customTypeCheckBox.setLabel("UIFolderForm".concat("label").concat(FIELD_CUSTOM_TYPE_CHECK_BOX));
    customTypeCheckBox.setOnChange("OnChange");
    this.addUIFormInput(customTypeCheckBox);

    // Custom type selectbox
    UIFormSelectBox customTypeSelectBox = new UIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX, FIELD_CUSTOM_TYPE_SELECT_BOX, null);
    customTypeSelectBox.setRendered(false);
    this.addUIFormInput(customTypeSelectBox);

    // Set action
    this.setActions(new String[]{"Save", "Cancel"});
  }

  /**
   * Activate form.
   */
  public void activate() {
    try {
      UICheckBoxInput customTypeCheckBox = this.getUICheckBoxInput(FIELD_CUSTOM_TYPE_CHECK_BOX);
      UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);

      // Get allowed folder types in current path
      UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
      List<String> folderTypes = Utils.getAllowedFolderTypesInCurrentPath(uiExplorer.getCurrentNode(),
                                                                          uiExplorer.getDriveData());

      // Only render custom type checkbox if at least 2 folder types allowed
      if (folderTypes.size() > 1) {
        customTypeCheckBox.setRendered(true);
        if (MANAGED_SITES.equals(this.getAncestorOfType(UIJCRExplorer.class).getDriveData().getName())) {
          customTypeCheckBox.setChecked(true);
          customTypeSelectBox.setRendered(true);
          this.fillCustomTypeSelectBox(folderTypes);
        } else {
          customTypeCheckBox.setChecked(false);
          customTypeSelectBox.setRendered(false);
        }
      } else {
        customTypeCheckBox.setRendered(false);
        customTypeSelectBox.setRendered(false);
        this.setSelectedType(folderTypes.get(0));
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e.getMessage());
      }
    }
  }

  public void deActivate() {}

  /**
   * Fill data to custom type select box.
   *
   * @param folderTypes
   * @throws Exception
   */
  private void fillCustomTypeSelectBox(List<String> folderTypes) throws Exception {
    UIFormSelectBox customTypeSelectBox = this.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    for (String folderType : folderTypes) {
      String label = this.getLabel(folderType.replace(":", "_"));
      options.add(new SelectItemOption<String>(label, folderType));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    customTypeSelectBox.setOptions(options);
  }

  /**
   * Get selected Folder Type.
   *
   * @return the selectedType
   */
  public String getSelectedType() {
    return selectedType;
  }

  /**
   * Set selected folder type.
   *
   * @param selectedType the selectedType to set
   */
  private void setSelectedType(String selectedType) {
    this.selectedType = selectedType;
  }

  public static class OnChangeActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIFolderForm uiFolderForm = event.getSource();
      UICheckBoxInput customTypeCheckBox = uiFolderForm.getUICheckBoxInput(FIELD_CUSTOM_TYPE_CHECK_BOX);
      UIFormSelectBox customTypeSelectBox = uiFolderForm.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);

      // Allowed folder types
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class);
      List<String> folderTypes =
          Utils.getAllowedFolderTypesInCurrentPath(uiExplorer.getCurrentNode(),
                                                   uiExplorer.getDriveData());

      // Fill custom type select box
      if (customTypeCheckBox.isChecked()) {
        uiFolderForm.fillCustomTypeSelectBox(folderTypes);
        customTypeSelectBox.setRendered(true);
      } else {
        customTypeSelectBox.setRendered(false);
      }

      event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderForm);
    }
  }

  static  public class SaveActionListener extends EventListener<UIFolderForm> {
    public void execute(Event<UIFolderForm> event) throws Exception {
      UIFolderForm uiFolderForm = event.getSource();
      UIJCRExplorer uiExplorer = uiFolderForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiFolderForm.getAncestorOfType(UIApplication.class);
      List<String> folderTypes =
          Utils.getAllowedFolderTypesInCurrentPath(uiExplorer.getCurrentNode(), uiExplorer.getDriveData());
      UICheckBoxInput customTypeCheckBox = uiFolderForm.getUICheckBoxInput(FIELD_CUSTOM_TYPE_CHECK_BOX);
      UIFormSelectBox customTypeSelectBox = uiFolderForm.getUIFormSelectBox(FIELD_CUSTOM_TYPE_SELECT_BOX);

      // Get title and name
      String title = uiFolderForm.getUIStringInput(FIELD_TITLE_TEXT_BOX).getValue();

      // Validate input
      Node currentNode = uiExplorer.getCurrentNode();
      if (uiExplorer.nodeIsLocked(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderForm);
        return;
      }
      if(StringUtils.isBlank(title)) {
        uiApp.addMessage(new ApplicationMessage("UIFolderForm.msg.name-invalid", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderForm);
        return;
      }

      // The name automatically determined from the title according to the current algorithm.
      String name = Text.escapeIllegalJcrChars(org.exoplatform.services.cms.impl.Utils.cleanString(title));

      // Set default name if new title contain no valid character
      if (StringUtils.isEmpty(name)) {
        name = DEFAULT_NAME;
      }

      // Get selected folder type
      if (customTypeCheckBox.isRendered()) {
        if (customTypeCheckBox.isChecked()) {
          String selectedValue = customTypeSelectBox.getValue();
          uiFolderForm.setSelectedType(selectedValue);
        } else {
          if (folderTypes.contains(Utils.NT_FOLDER)) {
            uiFolderForm.setSelectedType(Utils.NT_FOLDER);
          } else {
            // Message showing type nt:folder is not enabled, choose other type
            uiApp.addMessage(
                             new ApplicationMessage("UIFolderForm.msg.ntFolder-not-avaiable",
                                                    null,
                                                    ApplicationMessage.WARNING));
            event.getRequestContext().addUIComponentToUpdateByAjax(uiFolderForm);
            return;
          }
        }
      }

      try {
        // Add node
        Node addedNode = currentNode.addNode(name, uiFolderForm.getSelectedType());

        // Set title
        if (!addedNode.hasProperty(Utils.EXO_TITLE)) {
          addedNode.addMixin(Utils.EXO_RSS_ENABLE);
        }
        addedNode.setProperty(Utils.EXO_TITLE, title);

        currentNode.save();
        uiExplorer.updateAjax(event);
      } catch(ConstraintViolationException cve) {
        Object[] arg = { uiFolderForm.getSelectedType() };
        throw new MessageException(
            new ApplicationMessage("UIFolderForm.msg.constraint-violation", arg, ApplicationMessage.WARNING));
      } catch(AccessDeniedException accessDeniedException) {
        uiApp.addMessage(
            new ApplicationMessage("UIFolderForm.msg.repository-exception-permission", null, ApplicationMessage.WARNING));
      } catch(ItemExistsException re) {
        uiApp.addMessage(
            new ApplicationMessage("UIFolderForm.msg.not-allow-sameNameSibling", null, ApplicationMessage.WARNING));
      } catch(RepositoryException re) {
        String key = "UIFolderForm.msg.repository-exception";
        NodeDefinition[] definitions = currentNode.getPrimaryNodeType().getChildNodeDefinitions();
        boolean isSameNameSiblingsAllowed = false;
        for (NodeDefinition def : definitions) {
          if (def.allowsSameNameSiblings()) {
            isSameNameSiblingsAllowed = true;
            break;
          }
        }
        if (currentNode.hasNode(name) && !isSameNameSiblingsAllowed) {
          key = "UIFolderForm.msg.not-allow-sameNameSibling";
        }
        uiApp.addMessage(
            new ApplicationMessage(key, null, ApplicationMessage.WARNING));
      } catch(NumberFormatException nume) {
        uiApp.addMessage(
            new ApplicationMessage("UIFolderForm.msg.numberformat-exception", null, ApplicationMessage.WARNING));
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
