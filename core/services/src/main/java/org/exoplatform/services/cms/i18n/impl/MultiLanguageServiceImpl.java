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
package org.exoplatform.services.cms.i18n.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.exceptions.SameAsDefaultLangException;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.value.DateValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class MultiLanguageServiceImpl implements MultiLanguageService {

  /**
   * Path to child node keep content of parent node
   */
  final static public String  JCRCONTENT           = "jcr:content";

  /**
   * Property name keep data of node
   */
  final static public String  JCRDATA              = "jcr:data";

  /**
   * Property name keep mimeType of data
   */
  final static public String  JCR_MIMETYPE         = "jcr:mimeType";

  /**
   * NodeType name nt:unstructured
   */
  final static public String  NTUNSTRUCTURED       = "nt:unstructured";

  /**
   * NodeType name nt:folder
   */
  final static public String  NTFOLDER             = "nt:folder";

  /**
   * NodeType name nt:file
   */
  final static public String  NTFILE               = "nt:file";

  /**
   * Property name jcr:lastModified
   */
  final static public String  JCR_LASTMODIFIED     = "jcr:lastModified";

  /**
   * Property name exo:voter
   */
  final static String         VOTER_PROP           = "exo:voter";

  /**
   * Property name exo:votingRate
   */
  final static String         VOTING_RATE_PROP     = "exo:votingRate";

  /**
   * Property name exo:voteTotal
   */
  final static String         VOTE_TOTAL_PROP      = "exo:voteTotal";

  /**
   * Property name exo:boteTotalOfLang
   */
  final static String         VOTE_TOTAL_LANG_PROP = "exo:voteTotalOfLang";

  /**
   * Node path
   */
  final static String         NODE                 = "/node/";

  /**
   * Node path for language
   */
  final static String         NODE_LANGUAGE        = "/node/languages/";

  /**
   * Path to content node
   */
  final static String         CONTENT_PATH         = "/node/jcr:content/";

  /**
   * Name of temporatory node
   */
  final static String         TEMP_NODE            = "temp";

  private static final String MIX_REFERENCEABLE    = "mix:referenceable";

  private static final String MIX_COMMENTABLE ="mix:commentable";

  private static final String COUNTRY_VARIANT      = "_";

  private static final Log       LOG             = ExoLogger.getLogger(MultiLanguageServiceImpl.class.getName());

  /**
   * CmsService
   */
  private CmsService cmsService_ ;


  /**
   * Constructor method
   * @param cmsService  CmsService object
   * @throws Exception
   */
  public MultiLanguageServiceImpl(CmsService cmsService) throws Exception {
    cmsService_ = cmsService ;
  }

  /**
   * Set property for given node with value, property name, PropertyType, multiple
   * @param propertyName  name of property set to node
   * @param node          node which is added new property
   * @param requiredtype  Type of property
   * @param value         Value of property
   * @param isMultiple    This property is multiple if isMultiple = true or not if isMultiple = false
   * @throws Exception
   */
  private void setPropertyValue(String propertyName,
                                Node node,
                                int requiredtype,
                                Object value,
                                boolean isMultiple) throws Exception {
    switch (requiredtype) {
    case PropertyType.STRING:
      if (value == null) {
        node.setProperty(propertyName, "");
      } else {
        if(isMultiple) {
          if (value instanceof String) node.setProperty(propertyName, new String[] { value.toString()});
          else if(value instanceof String[]) node.setProperty(propertyName, (String[]) value);
        } else {
          if(value instanceof StringValue) {
            StringValue strValue = (StringValue) value ;
            node.setProperty(propertyName, strValue.getString());
          } else {
            node.setProperty(propertyName, value.toString());
          }
        }
      }
      break;
    case PropertyType.BINARY:
      if (value == null)
        node.setProperty(propertyName, "");
      else if (value instanceof byte[])
        node.setProperty(propertyName, new ByteArrayInputStream((byte[]) value));
      else if (value instanceof String)
        node.setProperty(propertyName, new ByteArrayInputStream((value.toString()).getBytes()));
      else if (value instanceof String[])
        node.setProperty(propertyName, new ByteArrayInputStream((((String[]) value)).toString()
                                                                                    .getBytes()));
      break;
    case PropertyType.BOOLEAN:
      if (value == null)
        node.setProperty(propertyName, false);
      else if (value instanceof String)
        node.setProperty(propertyName, new Boolean(value.toString()).booleanValue());
      else if (value instanceof String[])
        node.setProperty(propertyName, (String[]) value);
      break;
    case PropertyType.LONG:
      if (value == null || "".equals(value))
        node.setProperty(propertyName, 0);
      else if (value instanceof String)
        node.setProperty(propertyName, new Long(value.toString()).longValue());
      else if (value instanceof String[])
        node.setProperty(propertyName, (String[]) value);
      break;
    case PropertyType.DOUBLE:
      if (value == null || "".equals(value))
        node.setProperty(propertyName, 0);
      else if (value instanceof String)
        node.setProperty(propertyName, new Double(value.toString()).doubleValue());
      else if (value instanceof String[])
        node.setProperty(propertyName, (String[]) value);
      break;
    case PropertyType.DATE:
      if (value == null) {
        node.setProperty(propertyName, new GregorianCalendar());
      } else {
        if(isMultiple) {
          Session session = node.getSession() ;
          if (value instanceof String) {
            Value value2add = session.getValueFactory().createValue(ISO8601.parse((String) value));
            node.setProperty(propertyName, new Value[] {value2add});
          } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            Value[] convertedCalendarValues = new Value[values.length];
            int i = 0;
            for (String stringValue : values) {
              Value value2add = session.getValueFactory().createValue(ISO8601.parse(stringValue));
              convertedCalendarValues[i] = value2add;
              i++;
            }
            node.setProperty(propertyName, convertedCalendarValues);
          }
        } else {
          if(value instanceof String) {
            node.setProperty(propertyName, ISO8601.parse(value.toString()));
          } else if(value instanceof GregorianCalendar) {
            node.setProperty(propertyName, (GregorianCalendar) value);
          } else if(value instanceof DateValue) {
            DateValue dateValue = (DateValue) value ;
            node.setProperty(propertyName, dateValue.getDate());
          }
        }
      }
      break;
    case PropertyType.REFERENCE :
      if (value == null) {
        node.setProperty(propertyName, "");
      } else if (value instanceof Value) {
        node.setProperty(propertyName, (Value)value);
      } else if (value instanceof Value[]) {
        node.setProperty(propertyName, (Value[]) value);
      } else if (value instanceof String) {
        Session session = node.getSession();
        Node catNode = null;
        String itemPath = value.toString();
        if ((itemPath != null) && (itemPath.length() > 0)) {
          if (itemPath.indexOf(":/") > -1) {
            if (itemPath.split(":/").length > 0) itemPath = "/" + itemPath.split(":/")[1];
          }
          try {
            catNode = (Node)session.getItem(itemPath);
          } catch (PathNotFoundException e) {
            catNode = session.getRootNode().getNode(itemPath);
          }
          if (catNode != null) {
            if(!catNode.isNodeType(MIX_REFERENCEABLE)) {
              catNode.addMixin(MIX_REFERENCEABLE);
              catNode.save();
            }
            Value value2add = session.getValueFactory().createValue(catNode);
            if(isMultiple) {
              node.setProperty(propertyName, new Value[] {value2add});
            } else {
              node.setProperty(propertyName, value2add);
            }
          } else {
            node.setProperty(propertyName, value.toString());
          }
        }
      }
      break ;
    }
  }

  /**
   * Add all available mixintype from current node to newLang Node
   * Set value of all mixintype from current node to newLang Node
   * @param node        current node
   * @param newLang     new node that is added mixintype
   * @param setValuesOnyIfCanAddMixin indicates if the values must be set if we cannot add the mixin
   * @throws Exception
   */
  private void setMixin(Node node, Node newLang, boolean setValuesOnyIfCanAddMixin) throws Exception {
    NodeType[] mixins = node.getMixinNodeTypes() ;
    for(NodeType mixin:mixins) {
      if(canCopy(mixin)) {
        boolean mixinAdded = false;
        if(newLang.canAddMixin(mixin.getName())) {
          newLang.addMixin(mixin.getName()) ;
          mixinAdded = true;
        }
        if (!setValuesOnyIfCanAddMixin || mixinAdded) {
          for(PropertyDefinition def: mixin.getPropertyDefinitions()) {
            if(!def.isProtected()) {
              String propName = def.getName() ;
              if(def.isMandatory() && !def.isAutoCreated()) {
                if(def.isMultiple()) {
                  newLang.setProperty(propName,node.getProperty(propName).getValues()) ;
                } else {
                  newLang.setProperty(propName,node.getProperty(propName).getValue()) ;
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Add all available mixintype from current node to newLang Node
   * Set value of all mixintype from current node to newLang Node
   * @param node        current node
   * @param newLang     new node that is added mixintype
   * @throws Exception
   */
  private void setMixin(Node node, Node newLang) throws Exception {
    setMixin(node, newLang, true);
  }

  /**
   * Add new file with data = value to newLanguageNode node
   * @param fileName        name of file
   * @param newLanguageNode current node to add file
   * @param value           data of file
   * @param lastModified    datetime of modification
   * @param mimeType        mimitype
   * @param repositoryName  name of repository
   * @return                Node which is added with data is file
   * @throws Exception
   */
  private Node addNewFileNode(String fileName,
                              Node newLanguageNode,
                              Value value,
                              Object lastModified,
                              String mimeType,
                              String repositoryName) throws Exception {
    Map<String,JcrInputProperty> inputProperties = new HashMap<String,JcrInputProperty>() ;
    JcrInputProperty nodeInput = new JcrInputProperty() ;
    nodeInput.setJcrPath("/node") ;
    nodeInput.setValue(fileName) ;
    nodeInput.setMixintype("mix:i18n,mix:votable,mix:commentable") ;
    nodeInput.setType(JcrInputProperty.NODE) ;
    inputProperties.put("/node",nodeInput) ;

    JcrInputProperty jcrContent = new JcrInputProperty() ;
    jcrContent.setJcrPath("/node/jcr:content") ;
    jcrContent.setValue("") ;
    jcrContent.setMixintype("dc:elementSet") ;
    jcrContent.setNodetype("nt:resource") ;
    jcrContent.setType(JcrInputProperty.NODE) ;
    inputProperties.put("/node/jcr:content",jcrContent) ;

    JcrInputProperty jcrData = new JcrInputProperty() ;
    jcrData.setJcrPath("/node/jcr:content/jcr:data") ;
    jcrData.setValue(value.getStream()) ;
    inputProperties.put("/node/jcr:content/jcr:data",jcrData) ;

    JcrInputProperty jcrMimeType = new JcrInputProperty() ;
    jcrMimeType.setJcrPath("/node/jcr:content/jcr:mimeType") ;
    jcrMimeType.setValue(mimeType) ;
    inputProperties.put("/node/jcr:content/jcr:mimeType",jcrMimeType) ;

    JcrInputProperty jcrLastModified = new JcrInputProperty() ;
    jcrLastModified.setJcrPath("/node/jcr:content/jcr:lastModified") ;
    jcrLastModified.setValue(lastModified) ;
    inputProperties.put("/node/jcr:content/jcr:lastModified",jcrLastModified) ;

    JcrInputProperty jcrEncoding = new JcrInputProperty() ;
    jcrEncoding.setJcrPath("/node/jcr:content/jcr:encoding") ;
    jcrEncoding.setValue("UTF-8") ;
    inputProperties.put("/node/jcr:content/jcr:encoding",jcrEncoding) ;
    cmsService_.storeNode(NTFILE, newLanguageNode, inputProperties, true) ;
    return newLanguageNode.getNode(fileName) ;
  }

  /**
   * {@inheritDoc}
   */
  private Node getFileLangNode(Node languageNode) throws Exception {
    if(languageNode.getNodes().getSize() > 0) {
      NodeIterator nodeIter = languageNode.getNodes() ;
      while(nodeIter.hasNext()) {
        Node ntFile = nodeIter.nextNode() ;
        if(ntFile.isNodeType(NTFILE)) {
          return ntFile ;
        }
      }
      return languageNode ;
    }
    return languageNode ;
  }

  /**
   * {@inheritDoc}
   */
  public void addLanguage(Node node, Map inputs, String language, boolean isDefault) throws Exception {
    Node newLanguageNode = null ;
    Node languagesNode = null ;
    String defaultLanguage = getDefault(node) ;
    String primaryNodeTypeName = node.getPrimaryNodeType().getName();
    if(node.hasNode(LANGUAGES)) languagesNode = node.getNode(LANGUAGES) ;
    else  {
      languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
      if(languagesNode.canAddMixin("exo:hiddenable"))
        languagesNode.addMixin("exo:hiddenable");
    }
    if(!defaultLanguage.equals(language)){
      if(isDefault) {
        if(languagesNode.hasNode(defaultLanguage)) {
          newLanguageNode = languagesNode.getNode(defaultLanguage) ;
        } else {
          newLanguageNode = languagesNode.addNode(defaultLanguage, primaryNodeTypeName) ;
          setMixin(node, newLanguageNode, false);
        }
      } else {
        if(languagesNode.hasNode(language)) {
          newLanguageNode = languagesNode.getNode(language) ;
        } else {
          newLanguageNode = languagesNode.addNode(language, primaryNodeTypeName) ;
          setMixin(node, newLanguageNode, false);
          newLanguageNode.setProperty(EXO_LANGUAGE, language) ;
        }
      }
    }

    setPropertyLanguage(node, newLanguageNode, inputs, isDefault, defaultLanguage, language);
    if(isDefault && languagesNode.hasNode(language)) languagesNode.getNode(language).remove() ;
  }

  /**
   * {@inheritDoc}
   */
  public void addLinkedLanguage(Node node, Node translationNode, boolean forceReplace) throws Exception {
    Node languagesNode;
    if (node.hasNode(LANGUAGES))
      languagesNode = node.getNode(LANGUAGES);
    else {
      languagesNode = node.addNode(LANGUAGES, "nt:unstructured");
      if (languagesNode.canAddMixin("exo:hiddenable"))
        languagesNode.addMixin("exo:hiddenable");
    }
    if (!translationNode.isNodeType("mix:i18n")) {
      translationNode.addMixin("mix:i18n");
      translationNode.save();
    }
    if (!node.isNodeType("mix:i18n")) {
      node.addMixin("mix:i18n");
      node.save();
    }
    String lang = translationNode.getProperty("exo:language").getString();
    if (languagesNode.hasNode(lang)) {
      if (forceReplace) {
        languagesNode.getNode(lang).remove();
        languagesNode.save();
      } else {
        throw new ItemExistsException();
      }
    } else if (getDefault(node).equals(lang)) {
      throw new SameAsDefaultLangException();
    }
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    Node linkNode = linkManager.createLink(languagesNode, "exo:symlink", translationNode, lang);
    ((ExtendedNode)linkNode).setPermission(IdentityConstants.ANY, new String[]{PermissionType.READ});
    linkNode.getSession().save();
  }

  /**
   * {@inheritDoc}
   */
  public void addLinkedLanguage(Node node, Node translationNode) throws Exception {
    addLinkedLanguage(node, translationNode, false);
  }
  
  /**
   * {@inheritDoc}
   */
  public void addSynchronizedLinkedLanguage(Node selectedNode, Node newTranslationNode) throws Exception {
    if (newTranslationNode != null && newTranslationNode.isNodeType(Utils.EXO_SYMLINK)) {
      newTranslationNode = WCMCoreUtils.getService(LinkManager.class).getTarget(newTranslationNode);
    }

    if (!newTranslationNode.isNodeType("mix:i18n")) {
      newTranslationNode.addMixin("mix:i18n");
      newTranslationNode.save();
    }

    String newLang = newTranslationNode.getProperty("exo:language").getString();

    // Only add new translation if lang of new translation
    // has not existed yet inside selected Node
    if (getLanguage(selectedNode, newLang) == null) {

      // Get all real translation Nodes of selected node.
      // If there are some, add new translation for them
      List<Node> realTranslationNodes = getRealTranslationNodes(selectedNode);
      for (Node node : realTranslationNodes) {
        try {
          addLinkedLanguage(node, newTranslationNode);
        }
        catch(ItemExistsException ex) {
          if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Language %s already existed for %s", newLang, node.getPath()));
          }
        }

        // Update translations for new translation Node
        try {
          addLinkedLanguage(newTranslationNode, node);
        }
        catch(ItemExistsException ex) {
          if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Language %s already existed for %s",
                   node.getProperty("exo:language").getString(),
                   newTranslationNode.getPath()));
          }
        }
      }

      try {
        addLinkedLanguage(newTranslationNode, selectedNode);
      }
      catch(ItemExistsException ex) {
        if (LOG.isInfoEnabled()) {
          LOG.info(String.format("Language %s already existed for %s",
                 selectedNode.getProperty("exo:language").getString(),
                 newTranslationNode.getPath()));
        }
      }

      // Add new translation to selected Node
      addLinkedLanguage(selectedNode, newTranslationNode);
    } else {
      throw new ItemExistsException();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void addLanguage(Node node, Map inputs, String language, boolean isDefault, String nodeType) throws Exception {
    Node newLanguageNode = null ;
    Node languagesNode = null ;
    String primaryNodeTypeName = node.getPrimaryNodeType().getName();
    String defaultLanguage = getDefault(node) ;
    Workspace ws = node.getSession().getWorkspace() ;
    if(node.hasNode(LANGUAGES)) languagesNode = node.getNode(LANGUAGES) ;
    else  {
      languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
      if(languagesNode.canAddMixin("exo:hiddenable"))
        languagesNode.addMixin("exo:hiddenable");
    }
    if(!defaultLanguage.equals(language)){
      if(isDefault) {
        if(languagesNode.hasNode(defaultLanguage)) {
          newLanguageNode = languagesNode.getNode(defaultLanguage) ;
        } else {
          newLanguageNode = languagesNode.addNode(defaultLanguage, primaryNodeTypeName) ;
          setMixin(node, newLanguageNode, false);
        }
      } else {
        if(languagesNode.hasNode(language)) {
          newLanguageNode = languagesNode.getNode(language) ;
        } else {
          newLanguageNode = languagesNode.addNode(language, primaryNodeTypeName) ;
          setMixin(node, newLanguageNode, false);
          newLanguageNode.setProperty(EXO_LANGUAGE, language) ;
        }
      }
      Node jcrContent = node.getNode(nodeType) ;
      if ("jcr:content".equals(nodeType)) {
        Node jcrContentNode = newLanguageNode.addNode("jcr:content", "nt:resource");
        jcrContentNode.setProperty("jcr:lastModified", new GregorianCalendar());
        jcrContentNode.setProperty("jcr:mimeType", "text/plain");
        jcrContentNode.setProperty("jcr:data", "");
      }
      node.save() ;
      if(!newLanguageNode.hasNode(nodeType)) {
        ws.copy(jcrContent.getPath(), newLanguageNode.getPath() + "/" + jcrContent.getName()) ;
      }
      Node newContentNode = newLanguageNode.getNode(nodeType) ;
      PropertyIterator props = newContentNode.getProperties() ;
      while(props.hasNext()) {
        Property prop = props.nextProperty() ;
        if(inputs.containsKey(NODE + nodeType + "/" + prop.getName())) {
          JcrInputProperty inputVariable = (JcrInputProperty) inputs.get(NODE + nodeType + "/" + prop.getName()) ;
          boolean isMultiple = prop.getDefinition().isMultiple() ;
          setPropertyValue(prop.getName(), newContentNode, prop.getType(), inputVariable.getValue(), isMultiple) ;
        }
      }
      if(isDefault) {
        Node tempNode = node.addNode(TEMP_NODE, "nt:unstructured") ;
        node.getSession().move(node.getNode(nodeType).getPath(), tempNode.getPath() + "/" + nodeType) ;
        node.getSession().move(newLanguageNode.getNode(nodeType).getPath(), node.getPath() + "/" + nodeType) ;
        node.getSession().move(tempNode.getNode(nodeType).getPath(),
                               languagesNode.getPath() + "/" + defaultLanguage + "/" + nodeType);
        tempNode.remove() ;
      }
    } else {
      JcrInputProperty inputVariable = (JcrInputProperty) inputs.get(NODE + nodeType + "/" + JCRDATA) ;
      setPropertyValue(JCRDATA, node.getNode(nodeType), inputVariable.getType(), inputVariable.getValue(), false) ;
    }
    setPropertyLanguage(node, newLanguageNode, inputs, isDefault, defaultLanguage, language);
    if(isDefault && languagesNode.hasNode(language)) languagesNode.getNode(language).remove() ;
  }

  /**
   * {@inheritDoc}
   */
  public void addFileLanguage(Node node,
                              String fileName,
                              Value value,
                              String mimeType,
                              String language,
                              String repositoryName,
                              boolean isDefault) throws Exception {
    Node newLanguageNode = null ;
    Node languagesNode = null ;
    String defaultLanguage = getDefault(node) ;
    Node ntFileLangNode = null ;
    Node oldJcrContent = node.getNode(JCRCONTENT) ;
    String olfFileName = node.getName() ;
    Value oldValue = oldJcrContent.getProperty(JCRDATA).getValue() ;
    String oldMimeType = oldJcrContent.getProperty(JCR_MIMETYPE).getString() ;
    Calendar oldLastModified = new GregorianCalendar();
    oldLastModified.setTime(oldJcrContent.getProperty(JCR_LASTMODIFIED).getDate().getTime());
    try {
      languagesNode = node.getNode(LANGUAGES) ;
    } catch(PathNotFoundException pe) {
      languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
      if(languagesNode.canAddMixin("exo:hiddenable"))
        languagesNode.addMixin("exo:hiddenable");
    }
    if(!defaultLanguage.equals(language)){
      if(isDefault) {
        try {
          newLanguageNode = languagesNode.getNode(defaultLanguage) ;
        } catch(PathNotFoundException pe) {
          newLanguageNode = languagesNode.addNode(defaultLanguage) ;
          if (newLanguageNode.canAddMixin(MIX_COMMENTABLE)) {
            newLanguageNode.addMixin(MIX_COMMENTABLE);
          }
        }
        oldJcrContent.setProperty(JCR_MIMETYPE, mimeType) ;
        oldJcrContent.setProperty(JCRDATA, value) ;
        oldJcrContent.setProperty(JCR_LASTMODIFIED, new GregorianCalendar()) ;
        oldJcrContent.save();
      } else {
        try {
          newLanguageNode = languagesNode.getNode(language) ;
        } catch(PathNotFoundException pe) {
          newLanguageNode = languagesNode.addNode(language) ;
          if (newLanguageNode.canAddMixin(MIX_COMMENTABLE)) {
            newLanguageNode.addMixin(MIX_COMMENTABLE);
          }
          if(languagesNode.canAddMixin("exo:hiddenable"))
            languagesNode.addMixin("exo:hiddenable");
        }
      }
      try {
        ntFileLangNode = newLanguageNode.getNode(fileName) ;
      } catch(PathNotFoundException pe) {
        node.save();
        if(isDefault) {
          ntFileLangNode = addNewFileNode(olfFileName,
                                          newLanguageNode,
                                          oldValue,
                                          oldLastModified,
                                          oldMimeType,
                                          repositoryName);
        } else {
          ntFileLangNode = addNewFileNode(fileName, newLanguageNode, value,
              new GregorianCalendar(), mimeType, repositoryName) ;

        }
      }
      Node newJcrContent = ntFileLangNode.getNode(JCRCONTENT) ;
      newJcrContent.setProperty(JCR_LASTMODIFIED, new GregorianCalendar());
      setMixin(node, ntFileLangNode) ;
    } else {
      node.getNode(JCRCONTENT).setProperty(JCRDATA, value) ;
    }
    if(!defaultLanguage.equals(language) && isDefault){
      Node selectedFileLangeNode = null ;
      if(languagesNode.hasNode(language)) {
        Node selectedLangNode = languagesNode.getNode(language) ;
        selectedFileLangeNode = selectedLangNode.getNode(node.getName()) ;
      }
      setVoteProperty(ntFileLangNode, node, selectedFileLangeNode) ;
      setCommentNode(node, ntFileLangNode, selectedFileLangeNode) ;
    }
    if(isDefault) node.setProperty(EXO_LANGUAGE, language) ;
    node.getSession().save() ;
  }

  /**
   * {@inheritDoc}
   */
  public void addFileLanguage(Node node, String language, Map mappings, boolean isDefault) throws Exception {
    Node newLanguageNode = null ;
    Node languagesNode = null ;
    String primaryNodeTypeName = node.getPrimaryNodeType().getName();
    String defaultLanguage = getDefault(node) ;
    if(node.hasNode(LANGUAGES)) languagesNode = node.getNode(LANGUAGES) ;
    else  {
      languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
      if(languagesNode.canAddMixin("exo:hiddenable"))
        languagesNode.addMixin("exo:hiddenable");
    }
    if(!defaultLanguage.equals(language)){
      if(isDefault) {
        if(languagesNode.hasNode(defaultLanguage)) newLanguageNode = languagesNode.getNode(defaultLanguage) ;
        else newLanguageNode = languagesNode.addNode(defaultLanguage, primaryNodeTypeName) ;
      } else {
        if(languagesNode.hasNode(language)) newLanguageNode = languagesNode.getNode(language) ;
        else newLanguageNode = languagesNode.addNode(language, primaryNodeTypeName) ;
      }
      Node jcrContent = node.getNode(JCRCONTENT) ;
      if(!newLanguageNode.hasNode(JCRCONTENT)) {
        Node newJcrContent = newLanguageNode.addNode(JCRCONTENT, "nt:resource");
        newJcrContent.setProperty(JCR_MIMETYPE, jcrContent.getProperty(JCR_MIMETYPE).getValue());
        newJcrContent.setProperty(JCRDATA, jcrContent.getProperty(JCRDATA).getValue()) ;
        newJcrContent.setProperty(JCR_LASTMODIFIED, new GregorianCalendar());
      }
      node.save() ;
      Node newContentNode = newLanguageNode.getNode(JCRCONTENT) ;
      PropertyIterator props = newContentNode.getProperties() ;
      while (props.hasNext()) {
        Property prop = props.nextProperty() ;
        if(mappings.containsKey(CONTENT_PATH + prop.getName())) {
          JcrInputProperty inputVariable = (JcrInputProperty) mappings.get(CONTENT_PATH + prop.getName()) ;
          boolean isMultiple = prop.getDefinition().isMultiple() ;
          setPropertyValue(prop.getName(), newContentNode, prop.getType(), inputVariable.getValue(), isMultiple) ;
        }
      }
      if (isDefault) {
        Node tempNode = node.addNode(TEMP_NODE, "nt:unstructured");
        node.getSession().move(node.getNode(JCRCONTENT).getPath(),
                               tempNode.getPath() + "/" + JCRCONTENT);
        node.getSession().move(newLanguageNode.getNode(JCRCONTENT).getPath(),
                               node.getPath() + "/" + JCRCONTENT);
        node.getSession().move(tempNode.getNode(JCRCONTENT).getPath(),
                               languagesNode.getPath() + "/" + defaultLanguage + "/" + JCRCONTENT);
        tempNode.remove();
      }
      // add mixin type for node
      setMixin(node, newLanguageNode) ;
    } else {
      JcrInputProperty inputVariable = (JcrInputProperty) mappings.get(CONTENT_PATH + JCRDATA) ;
      setPropertyValue(JCRDATA, node.getNode(JCRCONTENT), inputVariable.getType(), inputVariable.getValue(), false) ;
    }
    setPropertyLanguage(node, newLanguageNode, mappings, isDefault, defaultLanguage, language);
  }

  /**
   * {@inheritDoc}
   */
  public String getDefault(Node node) throws Exception {
    if(node.hasProperty(EXO_LANGUAGE)) return node.getProperty(EXO_LANGUAGE).getString() ;
    return null ;
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getSupportedLanguages(Node node) throws Exception {
    List<String> languages = new ArrayList<String>();
    String defaultLang = getDefault(node) ;
    if(defaultLang != null) languages.add(defaultLang) ;
    if(node.hasNode(LANGUAGES)){
      Node languageNode = node.getNode(LANGUAGES) ;
      NodeIterator iter  = languageNode.getNodes() ;
      while(iter.hasNext()) {
        languages.add(iter.nextNode().getName());
      }
    }
    return languages;
  }

  /**
   * Get all current supported translation Nodes of specified node
   * @param node Specified Node
   * @return All current supported translation Nodes
   * @throws Exception
   */
  private List<Node> getRealTranslationNodes(Node node) throws Exception {
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    List<Node> translationNodes = new ArrayList<Node>();
    if(node.hasNode(LANGUAGES)){
      Node languageNode = node.getNode(LANGUAGES) ;
      NodeIterator iter  = languageNode.getNodes() ;
      while(iter.hasNext()) {
        Node currNode = iter.nextNode();
        if (currNode.isNodeType("exo:symlink")) {
          translationNodes.add(linkManager.getTarget(currNode));
        }
      }
    }
    return translationNodes;
  }

  /**
   * Set property concerning vote for newLang node from node.
   * Set property concerning vote for node from selectedLangNode
   * @param newLang               node for new language
   * @param node                  current node
   * @param selectedLangNode      selected language node
   * @throws Exception
   */
  private void setVoteProperty(Node newLang, Node node, Node selectedLangNode) throws Exception {
    if(hasMixin(newLang, "mix:votable")) {
      newLang.setProperty(VOTE_TOTAL_PROP, getVoteTotal(node)) ;
      newLang.setProperty(VOTE_TOTAL_LANG_PROP, node.getProperty(VOTE_TOTAL_LANG_PROP).getLong()) ;
      newLang.setProperty(VOTING_RATE_PROP, node.getProperty(VOTING_RATE_PROP).getLong()) ;
      if(node.hasProperty(VOTER_PROP)) {
        newLang.setProperty(VOTER_PROP, node.getProperty(VOTER_PROP).getValues()) ;
      }
      if(selectedLangNode != null) {
        node.setProperty(VOTE_TOTAL_PROP, getVoteTotal(node)) ;
        if(selectedLangNode.hasProperty(VOTE_TOTAL_LANG_PROP)) {
          node.setProperty(VOTE_TOTAL_LANG_PROP, selectedLangNode.getProperty(VOTE_TOTAL_LANG_PROP).getLong()) ;
        } else {
          node.setProperty(VOTE_TOTAL_LANG_PROP, 0) ;
        }
        if(selectedLangNode.hasProperty(VOTING_RATE_PROP)) {
          node.setProperty(VOTING_RATE_PROP, selectedLangNode.getProperty(VOTING_RATE_PROP).getLong()) ;
        } else {
          node.setProperty(VOTING_RATE_PROP, 0) ;
        }
        if(selectedLangNode.hasProperty(VOTER_PROP)) {
          node.setProperty(VOTER_PROP, selectedLangNode.getProperty(VOTER_PROP).getValues()) ;
        }
      } else {
        node.setProperty(VOTE_TOTAL_PROP, getVoteTotal(node)) ;
        node.setProperty(VOTE_TOTAL_LANG_PROP, 0) ;
        node.setProperty(VOTING_RATE_PROP, 0) ;
      }
    }
  }

  /**
   * Move COMMENTS child node of current node to newLang node
   * Move COMMENTS child node of selected node to current node
   * @param newLang               node for new language
   * @param node                  current node
   * @param selectedLangNode      selected language node
   * @throws Exception
   */
  private void setCommentNode(Node node, Node newLang, Node selectedLangNode) throws Exception {
    if(node.hasNode(COMMENTS)) {
      node.getSession().move(node.getPath() + "/" + COMMENTS, newLang.getPath() + "/" + COMMENTS) ;
    }
    if(selectedLangNode != null && selectedLangNode.hasNode(COMMENTS)) {
      node.getSession().move(selectedLangNode.getPath() + "/" + COMMENTS, node.getPath() + "/" + COMMENTS) ;
    }
  }

  /**
   * Get total value in VOTE_TOTAL_LANG_PROP property of current node and all file child node
   * @param node        current node
   * @return
   * @throws Exception
   */
  private long getVoteTotal(Node node) throws Exception {
    long voteTotal = 0;
    if(!node.hasNode(LANGUAGES) && node.hasProperty(VOTE_TOTAL_PROP)) {
      return node.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
    }
    Node multiLanguages = node.getNode(LANGUAGES) ;
    if (node.hasProperty(VOTE_TOTAL_LANG_PROP))
      voteTotal = node.getProperty(VOTE_TOTAL_LANG_PROP).getLong();
    NodeIterator nodeIter = multiLanguages.getNodes() ;
    String defaultLang = getDefault(node) ;
    while(nodeIter.hasNext()) {
      Node languageNode = nodeIter.nextNode() ;
      if(node.isNodeType(NTFILE)) {
        Node jcrContentNode = node.getNode(JCRCONTENT);
        if(!jcrContentNode.getProperty(JCR_MIMETYPE).getString().startsWith("text")) {
          languageNode = getFileLangNode(languageNode) ;
        }
      }
      if(!languageNode.getName().equals(defaultLang) && languageNode.hasProperty(VOTE_TOTAL_LANG_PROP)) {
        voteTotal = voteTotal + languageNode.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
      }
    }
    return voteTotal ;
  }

  /**
   * Check whether current node has MixinTypes name = noteTypeName
   * @param node
   * @param nodeTypeName
   * @return  true: if exist noteTypeName in MixtinTypes
   *          false: if not exist
   * @throws Exception
   */
  private boolean hasMixin(Node node, String nodeTypeName) throws Exception {
    NodeType[] mixinTypes = node.getMixinNodeTypes() ;
    for(NodeType nodeType : mixinTypes) {
      if(nodeType.getName().equals(nodeTypeName)) return true ;
    }
    return false ;
  }

  /**
   * Check if the given mixin type can be copied
   * @param mixin the mixin type to check
   * @return <code>true</code> if it the mixin can be copied, <code>false</code> otherwise
   */
  private boolean canCopy(NodeType mixin) {
    final String name = mixin.getName();
    // We must prevent to copy "mix:versionable" to avoid
    // ECM-4028: Restoring previous version of multilanguage article loose all languages except root one
    return !name.equals("exo:actionable") && !name.equals("mix:versionable");
  }

  private void setPropertyLanguage(Node node,
                                   Node newLanguageNode,
                                   Map mappings,
                                   boolean isDefault,
                                   String defaultLanguage,
                                   String language) throws Exception {
    PropertyDefinition[] properties = node.getPrimaryNodeType().getPropertyDefinitions() ;
    for(PropertyDefinition pro : properties){
      if(!pro.isProtected()) {
        String propertyName = pro.getName() ;
        JcrInputProperty property = (JcrInputProperty)mappings.get(NODE + propertyName) ;
        if(defaultLanguage.equals(language) && property != null) {
          setPropertyValue(propertyName, node, pro.getRequiredType(), property.getValue(), pro.isMultiple()) ;
        } else {
          if(isDefault) {
            if(node.hasProperty(propertyName)) {
              Object value = null ;
              int requiredType = node.getProperty(propertyName).getDefinition().getRequiredType() ;
              boolean isMultiple = node.getProperty(propertyName).getDefinition().isMultiple() ;
              if(isMultiple) value = node.getProperty(propertyName).getValues() ;
              else value = node.getProperty(propertyName).getValue() ;
              setPropertyValue(propertyName, newLanguageNode, requiredType, value, isMultiple) ;
            }
            if(property != null) {
              setPropertyValue(propertyName, node, pro.getRequiredType(), property.getValue(), pro.isMultiple()) ;
            }
          } else {
            if (property != null) {
              setPropertyValue(propertyName,
                               newLanguageNode,
                               pro.getRequiredType(),
                               property.getValue(),
                               pro.isMultiple());
            }
          }
        }
      }
    }
    if (!defaultLanguage.equals(language) && isDefault) {
      Node selectedLangNode = null;
      Node languagesNode = node.getNode(LANGUAGES);
      if (languagesNode.hasNode(language))
        selectedLangNode = languagesNode.getNode(language);
      setVoteProperty(newLanguageNode, node, selectedLangNode);
      setCommentNode(node, newLanguageNode, selectedLangNode);
    }
    if(isDefault) node.setProperty(EXO_LANGUAGE, language) ;
    node.save();
    node.getSession().save();
  }
  /**
   * {@inheritDoc}
   */
  public void setDefault(Node node, String language, String repositoryName) throws Exception {
    String defaultLanguage = getDefault(node) ;
    String nodeTypeName = node.getPrimaryNodeType().getName();
    if(!defaultLanguage.equals(language)){
      Node languagesNode = null ;
      try {
        languagesNode = node.getNode(LANGUAGES) ;
      } catch(PathNotFoundException pe) {
        languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
        if(languagesNode.canAddMixin("exo:hiddenable"))
          languagesNode.addMixin("exo:hiddenable");
      }
      Node selectedLangNode = languagesNode.getNode(language) ;
      Node newLang = null;
      if(nodeTypeName.equals(NTFILE)) {
        Node jcrContentNode = node.getNode(JCRCONTENT) ;
        if(!jcrContentNode.getProperty(JCR_MIMETYPE).getString().startsWith("text")) {
          newLang = languagesNode.addNode(defaultLanguage);
          selectedLangNode = getFileLangNode(selectedLangNode) ;
          node.save();
          newLang = addNewFileNode(node.getName(), newLang, jcrContentNode.getProperty(JCRDATA).getValue(),
              new GregorianCalendar(), jcrContentNode.getProperty(JCR_MIMETYPE).getString(), repositoryName) ;
          Node newJcrContent = newLang.getNode(JCRCONTENT) ;
          newJcrContent.setProperty(JCRDATA, jcrContentNode.getProperty(JCRDATA).getValue()) ;
          newJcrContent.setProperty(JCR_MIMETYPE, jcrContentNode.getProperty(JCR_MIMETYPE).getString()) ;
        } else {
          newLang = languagesNode.addNode(defaultLanguage, nodeTypeName) ;
        }
      } else if(node.isNodeType(NTUNSTRUCTURED) || node.isNodeType(NTFOLDER)) {
        newLang = languagesNode.addNode(defaultLanguage);
        selectedLangNode = selectedLangNode.getNode(node.getName());
        newLang = newLang.addNode(node.getName(), nodeTypeName);
      } else {
        newLang = languagesNode.addNode(defaultLanguage, nodeTypeName);
      }

      PropertyDefinition[] properties = node.getPrimaryNodeType().getPropertyDefinitions() ;
      for(PropertyDefinition pro : properties){
        if(!pro.isProtected()){
          String propertyName = pro.getName() ;
          if(node.hasProperty(propertyName)) {
            if(node.getProperty(propertyName).getDefinition().isMultiple()) {
              Value[] values = node.getProperty(propertyName).getValues() ;
              newLang.setProperty(propertyName, values) ;
            } else {
              newLang.setProperty(propertyName, node.getProperty(propertyName).getValue()) ;
            }
          }
          if(selectedLangNode.hasProperty(propertyName)) {
            if(selectedLangNode.getProperty(propertyName).getDefinition().isMultiple()) {
              Value[] values = selectedLangNode.getProperty(propertyName).getValues() ;
              node.setProperty(propertyName, values) ;
            } else {
              node.setProperty(propertyName, selectedLangNode.getProperty(propertyName).getValue()) ;
            }
          }
        }
      }
      setMixin(node, newLang);
      if(nodeTypeName.equals(NTFILE)) {
        Node tempNode = node.addNode(TEMP_NODE, NTUNSTRUCTURED) ;
        node.getSession().move(node.getNode(JCRCONTENT).getPath(), tempNode.getPath() + "/" + JCRCONTENT) ;
        node.getSession().move(selectedLangNode.getNode(JCRCONTENT).getPath(), node.getPath() + "/" + JCRCONTENT) ;
        if(node.getNode(JCRCONTENT).getProperty(JCR_MIMETYPE).getString().startsWith("text")) {
          node.getSession().move(tempNode.getPath() + "/" + JCRCONTENT, newLang.getPath() + "/" + JCRCONTENT);
        }
        tempNode.remove() ;
      } else if(node.isNodeType(NTUNSTRUCTURED) || node.isNodeType(NTFOLDER)) {
        processFolderNode(node, selectedLangNode, newLang);
      } else if(hasNodeTypeNTResource(node)) {
        processWithDataChildNode(node, selectedLangNode, languagesNode, defaultLanguage, getChildNodeType(node)) ;
      }
      setVoteProperty(newLang, node, selectedLangNode) ;
      node.setProperty(EXO_LANGUAGE, language) ;
      setCommentNode(node, newLang, selectedLangNode) ;
      if(nodeTypeName.equals(NTFILE) || node.isNodeType(NTUNSTRUCTURED) ||
          node.isNodeType(NTFOLDER)) {
        languagesNode.getNode(language).remove() ;
      } else {
        selectedLangNode.remove() ;
      }
      node.save() ;
      node.getSession().save() ;
    }
  }

  /**
   * Exchange child node of current node with the node
   * @param node
   * @param selectedLangNode
   * @param newLang
   * @throws RepositoryException
   */
  private void processFolderNode(Node node, Node selectedLangNode,
      Node newLang) throws RepositoryException {
    NodeIterator nodeIter = node.getNodes();
    while(nodeIter.hasNext()) {
      Node child = nodeIter.nextNode();
      if(child.getName().equals(LANGUAGES)) continue;
      if(!node.getSession().itemExists(newLang.getPath() + "/" + child.getName()))
        node.getSession().move(child.getPath(), newLang.getPath() + "/" + child.getName());
    }
    NodeIterator selectedIter = selectedLangNode.getNodes();
    while(selectedIter.hasNext()) {
      Node child = selectedIter.nextNode();
      if(!node.getSession().itemExists(node.getPath() + "/" + child.getName()))
        node.getSession().move(child.getPath(), node.getPath() + "/" + child.getName());
    }
  }

  /**
   * Exchange child node of current node with the default node
   * @param node
   * @param newLang
   * @throws RepositoryException
   */
  private void processFolderNode(Node node, Node newLang) throws RepositoryException {
    NodeIterator nodeIter = node.getNodes();
    Node tempNode = newLang.addNode(TEMP_NODE, NTUNSTRUCTURED);
    Node selectedLangNode = newLang.getNode(node.getName());
    while(nodeIter.hasNext()) {
      Node child = nodeIter.nextNode();
      if(child.getName().equals(LANGUAGES)) continue;
      if(!node.getSession().itemExists(tempNode.getPath() + "/" + child.getName()))
        node.getSession().move(child.getPath(), tempNode.getPath() + "/" + child.getName());
    }

    NodeIterator selectedIter = selectedLangNode.getNodes();
    while(selectedIter.hasNext()) {
      Node child = selectedIter.nextNode();
      node.getSession().move(child.getPath(), node.getPath() + "/" + child.getName());
    }
    NodeIterator tempIter = tempNode.getNodes();
    while(tempIter.hasNext()) {
      Node child = tempIter.nextNode();
      if(!node.getSession().itemExists(selectedLangNode.getPath() + "/" + child.getName()))
        node.getSession().move(child.getPath(), selectedLangNode.getPath() + "/" + child.getName());
    }
    tempNode.remove();
  }

  /**
   * Exchange child node of current node with name = nodeType to
   * child node of selectedLangNode with the same name
   * @param node              current node
   * @param selectedLangNode  selected language node
   * @param languagesNode     language node
   * @param defaultLanguage   default language of node
   * @param nodeType
   * @throws Exception
   */
  private void processWithDataChildNode(Node node, Node selectedLangNode, Node languagesNode,
      String defaultLanguage, String nodeType) throws Exception {
    Node tempNode = node.addNode(TEMP_NODE, NTUNSTRUCTURED) ;
    if(!node.getSession().itemExists(tempNode.getPath() + "/" + nodeType))
      node.getSession().move(node.getNode(nodeType).getPath(), tempNode.getPath() + "/" + nodeType) ;
    if(!node.getSession().itemExists(node.getPath() + "/" + nodeType))
      node.getSession().move(selectedLangNode.getNode(nodeType).getPath(), node.getPath() + "/" + nodeType) ;
    if(!node.getSession().itemExists(languagesNode.getPath() + "/" + defaultLanguage + "/" + nodeType))
      node.getSession().move(tempNode.getNode(nodeType).getPath(),
                             languagesNode.getPath() + "/" + defaultLanguage + "/" + nodeType);
    tempNode.remove() ;
  }

  /**
   * Check whether child node with primary node type = nt:resource exists
   * @param node  current node
   * @return  true: exist
   *          false: not exist
   * @throws Exception
   */
  private boolean hasNodeTypeNTResource(Node node) throws Exception {
    if(node.hasNodes()) {
      NodeIterator nodeIter = node.getNodes() ;
      while(nodeIter.hasNext()) {
        Node childNode = nodeIter.nextNode() ;
        if(childNode.isNodeType("nt:resource")) return true ;
      }
    }
    return false ;
  }

  /**
   * Get name of child node of current node with name of PrimaryNodeType in child node = nt:resource
   * @param node  current node
   * @return name of child node if exist child node with PrimaryNodeType = nt:resource
   *         null if not exist
   * @throws Exception
   */
  private String getChildNodeType(Node node) throws Exception {
    if(node.hasNodes()) {
      NodeIterator nodeIter = node.getNodes() ;
      while(nodeIter.hasNext()) {
        Node childNode = nodeIter.nextNode() ;
        if(childNode.isNodeType("nt:resource")) return childNode.getName() ;
      }
    }
    return null ;
  }
  
  /**
   * {@inheritDoc}
   */
  public Node getLanguage(Node node, String language) throws Exception {
    if(node.hasNode(LANGUAGES + "/"+ language)) {
      Node target = node.getNode(LANGUAGES + "/"+ language) ;
      if (target.isNodeType("exo:symlink")) {
        LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
        target = linkManager.getTarget(target);
      }
      return target;
    }
    if (language.contains(COUNTRY_VARIANT)) {
      String pureLanguage = language.substring(0, language.indexOf(COUNTRY_VARIANT) ) ;
      if(node.hasNode(LANGUAGES + "/"+ pureLanguage)) {
        Node target = node.getNode(LANGUAGES + "/"+ pureLanguage) ;
        if (target.isNodeType("exo:symlink")) {
          LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
          target = linkManager.getTarget(target);
        }
        return target;
      }
    }
    return null;
  }

  public void addFolderLanguage(Node node, Map inputs, String language,
      boolean isDefault, String nodeType, String repositoryName) throws Exception {
    Node newLanguageNode = null ;
    Node languagesNode = null ;
    String defaultLanguage = getDefault(node) ;
    boolean isAddNew = false;
    if(node.hasNode(LANGUAGES)) {
      languagesNode = node.getNode(LANGUAGES) ;
    } else  {
      languagesNode = node.addNode(LANGUAGES, NTUNSTRUCTURED) ;
      if(languagesNode.canAddMixin("exo:hiddenable")) languagesNode.addMixin("exo:hiddenable");
    }
    if(!defaultLanguage.equals(language)){
      String addedLange = language;
      if(isDefault) addedLange = defaultLanguage;
      try {
        isAddNew = false;
        newLanguageNode = languagesNode.getNode(addedLange) ;
      } catch(PathNotFoundException e) {
        isAddNew = true;
        newLanguageNode = languagesNode.addNode(addedLange) ;
      }
    }
    String nodePath = cmsService_.storeNode(nodeType, newLanguageNode, inputs, isAddNew);
    Node selectedNode = (Node)node.getSession().getItem(nodePath);
    if(isAddNew) {
      setMixin(node, selectedNode, false);
      selectedNode.setProperty(EXO_LANGUAGE, language) ;
    }
    setPropertyLanguage(node, selectedNode, inputs, isDefault, defaultLanguage, language);
    if(isDefault) processFolderNode(node, newLanguageNode);
    node.getSession().save();
    if(isDefault && languagesNode.hasNode(language)) languagesNode.getNode(language).remove() ;
    languagesNode.save();
  }
}
