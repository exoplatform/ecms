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
package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;

/**
 * The Interface NewDocumentTemplatePlugin provides API for getting templates of specific provider.
 * Also allows to create documents based on DocumentTemplate.
 */
public interface NewDocumentTemplatePlugin {

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  List<DocumentTemplate> getTemplates();
  
  /**
   * Gets the template 
   * 
   * @param name the name
   * @return the template
   */
  DocumentTemplate getTemplate(String name);
  
  /**
   * Gets editor plugin
   * 
   * @return the editorPlugin
   */
  DocumentEditorPlugin getEditor();

  /**
   * Creates the document from specified template.
   *
   * @param parent the parent
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception;

}
