/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * 
 * Widget script responsible to fill a selectbox with JCR value contraints property definition 
 * params[0] : nodetype name
 * params[1] : property name where constraints are defined
 *
 */
public class FillSelectWithConstraints implements CmsScript {

  private RepositoryService repositoryService_;

  private String            nodetypeName = null;

  private String            property     = null;

  public FillSelectWithConstraints(RepositoryService repositoryService) {
    this.repositoryService_ = repositoryService;
  }

  public void execute(Object context) {
    UIFormSelectBox selectBox = (UIFormSelectBox) context;
    
    try {
      NodeType nodetype = getNodetype();
      PropertyDefinition[] propdefs = nodetype.getPropertyDefinitions();
        for (propertyDefinition in propdefs) {
        if (property.equals(propertyDefinition.getName())) {
          String[] constraints = propertyDefinition.getValueConstraints();
          List options = new ArrayList();
          if (constraints != null) {
            for (constraint in constraints) {
              options.add( new SelectItemOption(constraint, constraint));
            }
          }
          selectBox.setOptions(options);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to fill with constraints: " + e.getMessage());
    }

  }

  private NodeType getNodetype() throws Exception {
    NodeTypeManager nodetypeManager = repositoryService_.getCurrentRepository()
        .getNodeTypeManager();
    NodeType nodetype = nodetypeManager.getNodeType(nodetypeName);
    return nodetype;
  }

  public void setParams(String[] params) {
    if (params != null && params.length == 2) {
      nodetypeName = params[0];
      property = params[1];
    } else {
      throw new RuntimeException("2 params needed : nodetype and property");
    }
  }

}
