/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.impl;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;

/**
 * The Class NewDocumentTemplateProviderImpl.
 */
public class NewDocumentTemplateProviderImpl implements NewDocumentTemplateProvider {

  /** The plugin. */
  protected final NewDocumentTemplatePlugin plugin;

  /** The document service. */
  protected final DocumentService           documentService;

  /** The editor. */
  protected DocumentEditorProvider          editor;

  /**
   * Instantiates a new new document template provider impl.
   *
   * @param plugin the plugin
   */
  public NewDocumentTemplateProviderImpl(NewDocumentTemplatePlugin plugin) {
    this.plugin = plugin;
    this.documentService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(DocumentService.class);
  }

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  @Override
  public List<NewDocumentTemplate> getTemplates() {
    return plugin.getTemplates();
  }

  /**
   * Gets the template.
   *
   * @param name the name
   * @return the template
   */
  @Override
  public NewDocumentTemplate getTemplate(String name) {
    return plugin.getTemplate(name);
  }

  /**
   * Gets the editor class.
   *
   * @return the editor class
   */
  @Override
  public Class<? extends DocumentEditor> getEditorClass() {
    return plugin.getEditorClass();
  }

  /**
   * Creates the document.
   *
   * @param parent the parent
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  @Override
  public Node createDocument(Node parent, String title, NewDocumentTemplate template) throws Exception {
    return plugin.createDocument(parent, title, template);
  }

  /**
   * Gets the editor.
   *
   * @return the editor
   */
  @Override
  public DocumentEditorProvider getEditor() {
    return documentService.getDocumentEditorProviders()
                   .stream()
                   .filter(provider -> provider.getEditorClass() == plugin.getEditorClass())
                   .findAny()
                   .orElse(null);
  }

}
