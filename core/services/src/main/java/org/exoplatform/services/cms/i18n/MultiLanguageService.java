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
package org.exoplatform.services.cms.i18n;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Value;


/**
 * Author : Hung Nguyen Quang
 *          nguyenkequanghung@yahoo.com
 */

public interface MultiLanguageService {

  /**
   * Node name as LANGUAGES
   */
  final static public String LANGUAGES    = "languages";

  /**
   * Property name as EXO_LANGUAGE
   */
  final static public String EXO_LANGUAGE = "exo:language";

  /**
   * Node name as COMMENTS
   */
  final static public String COMMENTS     = "comments";

  /**
   * Get list of value in exo:language property in child node of current node
   * @param node    current node
   * @return value of exo:language property
   */
  public List<String> getSupportedLanguages(Node node) throws Exception;

  /**
   * Set data for current node
   * @param node              current node
   * @param language          language name
   * @param repositoryName    repository name
   * @throws Exception
   */
  public void setDefault(Node node, String language, String repositoryName) throws Exception;

  /**
   * Add new language for current node
   * Set value of property in inputs Map to new language node
   * @param node            current node
   * @param inputs          Map includes key and value of property
   * @param language        language name
   * @param isDefault       flag to define default language is used or not
   * @throws Exception
   */
  public void addLanguage(Node node, Map inputs, String language, boolean isDefault) throws Exception;

  /**
   * Add new language for current node
   * Processing for some new added node
   * Set value of property in inputs Map to new language node
   * Processing for nodeType in child node of current node and some new added node
   * @param node            current node
   * @param inputs          Map includes key and value of property
   * @param language        language name
   * @param isDefault       flag to define default language is used or not
   * @param nodeType        node name
   * @throws Exception
   */
  public void addLanguage(Node node, Map inputs, String language, boolean isDefault, String nodeType) throws Exception;

  /**
   * Add newLanguageNode node, then add new file to newLanguageNode
   * @param node              current node
   * @param fileName          name of file
   * @param value             value of file
   * @param mimeType          mimiType
   * @param language          language name
   * @param repositoryName    repository name
   * @param isDefault         flag to use new language or default language
   * @throws Exception
   */
  public void addFileLanguage(Node node,
                              String fileName,
                              Value value,
                              String mimeType,
                              String language,
                              String repositoryName,
                              boolean isDefault) throws Exception;

  /**
   * Add newLanguageNode node, then set property in mapping to newLanguageNode
   * @param node              current node
   * @param language          language name
   * @param mappings          Map includes property and value
   * @param isDefault         flag to use new language or default language
   * @throws Exception
   */
  public void addFileLanguage(Node node, String language, Map mappings, boolean isDefault) throws Exception;


  /**
   * Add newLanguageNode node with a symlink, based on exo:language targetNode property
   * @param node              current node
   * @param translationNode   target translation node
   * @throws Exception
   */
  public void addLinkedLanguage(Node node, Node translationNode) throws Exception;

  /**
   * Add new translation for one node and synchronize all related translation nodes.
   * 
   * @param selectedNode Selected Node
   * @param newTranslationNode New Translation Node
   * @throws Exception
   */
  public void addSynchronizedLinkedLanguage(Node selectedNode, Node newTranslationNode) throws Exception;
  
  /**
   * Add new language node as a folder
   * @param node
   * @param inputs
   * @param language
   * @param isDefault
   * @param nodeType
   * @param repositoryName
   * @throws Exception
   */
  public void addFolderLanguage(Node node,
                                Map inputs,
                                String language,
                                boolean isDefault,
                                String nodeType,
                                String repositoryName) throws Exception;

  /**
   * Get value of property exo:language in current node
   * @param node    current node
   * @return value of exo:language property
   */
  public String getDefault(Node node) throws Exception;

  /**
   * Get node following relative path = "languages/" + language
   * @param node
   * @param language
   * @return node if exist node with relative path = "languages/" + language with current node
   *         null if not exist
   */
  public Node getLanguage(Node node, String language) throws Exception;


}
