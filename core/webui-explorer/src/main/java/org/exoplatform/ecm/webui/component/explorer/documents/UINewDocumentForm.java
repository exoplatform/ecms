/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.form.validator.XSSValidator;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
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
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * The UINewDocumentForm is displayed when 'New Document' button was clicked in ECMS Action Bar.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:groovy/ecm/webui/UINewDocument.gtmpl", events = {
    @EventConfig(listeners = UINewDocumentForm.SaveActionListener.class),
    @EventConfig(listeners = UINewDocumentForm.CancelActionListener.class, phase = Phase.DECODE) })
public class UINewDocumentForm extends UIForm implements UIPopupComponent {

  /** The Constant FIELD_TITLE_TEXT_BOX. */
  public static final String  FIELD_TITLE_TEXT_BOX  = "titleTextBox";

  /** The Constant FIELD_TYPE_SELECT_BOX. */
  public static final String  FIELD_TYPE_SELECT_BOX = "typeSelectBox";

  /** The Constant DEFAULT_NAME. */
  private static final String DEFAULT_NAME          = "untitled";

  /** The Constant LOG. */
  protected static final Log  LOG                   = ExoLogger.getLogger(UINewDocumentForm.class.getName());

  /** The document service. */
  protected DocumentService   documentService;

  /**
   * Constructor.
   *
   */
  public UINewDocumentForm() throws Exception{
    this.documentService = this.getApplicationComponent(DocumentService.class);
    // Title textbox
    UIFormStringInput titleTextBox = new UIFormStringInput(FIELD_TITLE_TEXT_BOX, FIELD_TITLE_TEXT_BOX, null);
    titleTextBox.addValidator(MandatoryValidator.class).addValidator(ECMNameValidator.class).addValidator(XSSValidator.class);
    this.addUIFormInput(titleTextBox);

    List<NewDocumentTemplateProvider> templateProviders = documentService.getNewDocumentTemplateProviders();

    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    Identity identity = ConversationState.getCurrent().getIdentity();
    templateProviders.forEach(provider -> {
      if (provider.getEditor().isAvailableForUser(identity)) {
        provider.getTemplates().forEach(template -> {
          DocumentSelectItemOption<String> option = new DocumentSelectItemOption<>(template.getName(), provider);
          options.add(option);
        });
      }
    });

    UIFormSelectBox typeSelectBox = new UIFormSelectBox(FIELD_TYPE_SELECT_BOX, FIELD_TYPE_SELECT_BOX, options);
    typeSelectBox.setRendered(true);
    this.addUIFormInput(typeSelectBox);
    this.setActions(new String[] { "Save", "Cancel" });
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class SaveActionListener extends EventListener<UINewDocumentForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<UINewDocumentForm> event) throws Exception {

      UINewDocumentForm uiDocumentForm = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentForm.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiDocumentForm.getAncestorOfType(UIApplication.class);

      Node currentNode = uiExplorer.getCurrentNode();
      if (uiExplorer.nodeIsLocked(currentNode)) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
        return;
      }
      // Get title
      String title = uiDocumentForm.getUIStringInput(FIELD_TITLE_TEXT_BOX).getValue();

      if (StringUtils.isBlank(title)) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.name-invalid", null, ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentForm);
        return;
      }

      UIFormSelectBox typeSelectBox = uiDocumentForm.getUIFormSelectBox(FIELD_TYPE_SELECT_BOX);
      List<SelectItemOption<String>> options = typeSelectBox.getOptions();
      DocumentSelectItemOption<String> selectedOption =
                                                      (DocumentSelectItemOption<String>) options.stream()
                                                                                                .filter(option -> option.isSelected())
                                                                                                .findFirst()
                                                                                                .get();
      NewDocumentTemplateProvider templateProvider = selectedOption.getTemplateProvider();
      String name = selectedOption.getLabel();
      NewDocumentTemplate template = templateProvider.getTemplate(name);

      DocumentEditorProvider editorProvider = templateProvider.getEditor();
      title = getFileName(title, template);
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (editorProvider != null && editorProvider.isAvailableForUser(identity)) {
        editorProvider.beforeDocumentCreate(template, currentNode.getPath(), title);
      }

      Node document = null;
      try {
        document = templateProvider.createDocument(currentNode, title, template);
      } catch (ConstraintViolationException cve) {
        Object[] arg = { typeSelectBox.getValue() };
        throw new MessageException(new ApplicationMessage("UINewDocumentForm.msg.constraint-violation",
                                                          arg,
                                                          ApplicationMessage.WARNING));
      } catch (AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.repository-exception-permission",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (ItemExistsException re) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.not-allow-sameNameSibling",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (RepositoryException re) {
        String key = "UINewDocumentForm.msg.repository-exception";
        NodeDefinition[] definitions = currentNode.getPrimaryNodeType().getChildNodeDefinitions();
        boolean isSameNameSiblingsAllowed = false;
        for (NodeDefinition def : definitions) {
          if (def.allowsSameNameSiblings()) {
            isSameNameSiblingsAllowed = true;
            break;
          }
        }
        if (currentNode.hasNode(title) && !isSameNameSiblingsAllowed) {
          key = "UINewDocumentForm.msg.not-allow-sameNameSibling";
        }
        uiApp.addMessage(new ApplicationMessage(key, null, ApplicationMessage.WARNING));
      } catch (NumberFormatException nume) {
        uiApp.addMessage(new ApplicationMessage("UINewDocumentForm.msg.numberformat-exception",
                                                null,
                                                ApplicationMessage.WARNING));
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
      }

      if (document != null && editorProvider != null && editorProvider.isAvailableForUser(identity)) {
        editorProvider.onDocumentCreated(document.getSession().getWorkspace().getName(), document.getPath());
      }
      uiExplorer.updateAjax(event);
    }

    public String getFileName(String title, NewDocumentTemplate template) {

      title = Text.escapeIllegalJcrChars(title);
      if (StringUtils.isEmpty(title)) {
        title = DEFAULT_NAME;
      }
      String extension = template.getExtension();
      if (extension == null || extension.trim().isEmpty()) {
        String path = template.getPath();
        if (path.contains(".")) {
          extension = path.substring(path.lastIndexOf("."));
        }
      }
      if (!title.endsWith(extension)) {
        title += extension;
      }
      return title;
    }
  }

  /**
   * The listener interface for receiving cancelAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class CancelActionListener extends EventListener<UINewDocumentForm> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<UINewDocumentForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  /**
   * Activate.
   */
  @Override
  public void activate() {
    // Nothing
  }

  /**
   * De activate.
   */
  @Override
  public void deActivate() {
    // Nothing
  }
}
