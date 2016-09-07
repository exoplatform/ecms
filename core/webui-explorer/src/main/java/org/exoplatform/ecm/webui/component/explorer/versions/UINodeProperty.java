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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Le Bien Thuy
 *          lebienthuy@gmail.com
 * Oct 20, 2006
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/explorer/versions/UINodeProperty.gtmpl"
)
public class UINodeProperty extends UIForm{

  public UINodeProperty() {}

  public List<Property> getVersionedNodeProperties() throws Exception{
    RepositoryService repositoryService =
      (RepositoryService)PortalContainer.getComponent(RepositoryService.class) ;
    List<Property> list = new ArrayList<Property>() ;
    NodeTypeManager nodeTypeManager = repositoryService.getCurrentRepository().getNodeTypeManager();
    NodeType jcrFrozenNode = nodeTypeManager.getNodeType("nt:frozenNode") ;
    NodeType ntVersion = nodeTypeManager.getNodeType("nt:version") ;
    NodeType ntVersionHistory = nodeTypeManager.getNodeType("nt:versionHistory") ;
    NodeType mixVersionable = nodeTypeManager.getNodeType("mix:versionable") ;
    UIVersionInfo uiVersionInfo = getAncestorOfType(UIDocumentWorkspace.class).getChild(UIVersionInfo.class) ;
    Node frozenNode = uiVersionInfo.getCurrentVersionNode().getNode("jcr:frozenNode") ;
    for(PropertyIterator propertyIter = frozenNode.getProperties(); propertyIter.hasNext() ;) {
      Property property = propertyIter.nextProperty() ;
      boolean isDefinition = false ;
      for(PropertyDefinition propDef : jcrFrozenNode.getPropertyDefinitions()) {
        if(propDef.getName().equals(property.getName())) isDefinition = true ;
      }
      for(PropertyDefinition propDef : ntVersion.getPropertyDefinitions()) {
        if(propDef.getName().equals(property.getName())) isDefinition = true ;
      }
      for(PropertyDefinition propDef : ntVersionHistory.getPropertyDefinitions()) {
        if(propDef.getName().equals(property.getName())) isDefinition = true ;
      }
      for(PropertyDefinition propDef : mixVersionable.getPropertyDefinitions()) {
        if(propDef.getName().equals(property.getName())) isDefinition = true ;
      }
      if(!isDefinition) list.add(property) ;
    }
    return list ;
  }

  public String getPropertyValue(Property property) throws Exception{
    switch(property.getType()) {
    case PropertyType.BINARY: return Integer.toString(PropertyType.BINARY) ;
    case PropertyType.BOOLEAN :return Boolean.toString(property.getValue().getBoolean()) ;
    case PropertyType.DATE : return property.getValue().getDate().getTime().toString() ;
    case PropertyType.DOUBLE : return Double.toString(property.getValue().getDouble()) ;
    case PropertyType.LONG : return Long.toString(property.getValue().getLong()) ;
    case PropertyType.NAME : return property.getValue().getString() ;
    case PropertyType.STRING : return property.getValue().getString() ;
    case PropertyType.REFERENCE : {
      if(property.getName().equals("exo:category") || property.getName().equals("exo:relation")) {
        Session session = getSystemSession() ;
        Node referenceNode = session.getNodeByUUID(property.getValue().getString()) ;
        String path = referenceNode.getPath();
        return path ;
      }
      return property.getValue().getString() ;
    }
    }
    return null ;
  }


  public List<String> getPropertyMultiValues(Property property) throws Exception {
    String propName = property.getName() ;
    if(propName.equals("exo:category")) return getCategoriesValues(property) ;
    else if(propName.equals("exo:relation")) return getRelationValues(property) ;
    List<String> values = new ArrayList<String>() ;
    for(Value value:property.getValues()) {
      values.add(value.getString()) ;
    }
    return values ;
  }

  public boolean isMultiValue(Property prop) throws Exception{
    PropertyDefinition propDef = prop.getDefinition() ;
    return propDef.isMultiple() ;
  }

  private List<String> getReferenceValues(Property property) throws Exception {
    Session session = getSystemSession() ;
    List<String> pathList = new ArrayList<String>() ;
    Value[] values = property.getValues() ;
    for(Value value:values) {
      Node referenceNode = session.getNodeByUUID(value.getString()) ;
      pathList.add(referenceNode.getPath()) ;
    }
    return pathList ;
  }

  private List<String> getRelationValues(Property relationProp) throws Exception {
    return getReferenceValues(relationProp) ;
  }

  private List<String> getCategoriesValues(Property categoryProp) throws Exception {
    return getReferenceValues(categoryProp) ;
  }

  private Session getSystemSession() throws Exception {
    ManageableRepository manageableRepository = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    String systemWorksapce = manageableRepository.getConfiguration().getDefaultWorkspaceName();
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(systemWorksapce, manageableRepository) ;
    return session ;
  }
}
