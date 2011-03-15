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
package org.exoplatform.services.cms.actions.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class ActionConfig {

  private boolean autoCreatedInNewRepository ;
  private String repository;
  private String workspace;
  private List actions = new ArrayList(5);

  public List getActions() { return actions; }
  public void setActions(List actions) { this.actions = actions; }

  public boolean getAutoCreatedInNewRepository() { return this.autoCreatedInNewRepository ; }
  public void setAutoCreatedInNewRepository(boolean isAuto) { this.autoCreatedInNewRepository = isAuto ; }

  public String getRepository() { return repository; }
  public void setRepository(String repository) { this.repository = repository; }

  public String getWorkspace() { return workspace; }
  public void setWorkspace(String workspace) { this.workspace = workspace; }

  static public class Mixin {
    private String name;
    private String properties;

    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }

    public Map<String, String> getParsedProperties() {
      Map<String, String> propMap = new HashMap<String, String>();
      String[] props = StringUtils.split(this.properties, ";");
      for (int i = 0; i < props.length; i++) {
        String prop = props[i];
        String[] couple = StringUtils.split(prop, "=");
        propMap.put(couple[0], couple[1]);
      }
      return propMap;
    }


    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }

  }

  static public class TaxonomyAction {
    private String name;
    private String type;
    private String description;
    private String homePath;
    private String targetWspace;
    private String targetPath;
    private List<String> lifecyclePhase = new ArrayList<String>();
    private String roles;
    private List<String> affectedNodeTypes;
    private List mixins = new ArrayList(10);

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getHomePath() {
      return homePath;
    }

    public void setHomePath(String homePath) {
      this.homePath = homePath;
    }

    public List<String> getLifecyclePhase() {
      return lifecyclePhase;
    }

    public void setLifecyclePhase(List<String> lifecyclePhase) {
      this.lifecyclePhase = lifecyclePhase;
    }

    public List getMixins() {
      return mixins;
    }

    public void setMixins(List mixins) {
      this.mixins = mixins;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRoles() {
      return roles;
    }

    public void setRoles(String roles) {
      this.roles = roles;
    }

    public String getTargetPath() {
      return targetPath;
    }

    public void setTargetPath(String targetPath) {
      this.targetPath = targetPath;
    }

    public String getTargetWspace() {
      return targetWspace;
    }

    public void setTargetWspace(String targetWspace) {
      this.targetWspace = targetWspace;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public List<String> getAffectedNodeTypes() {
      return affectedNodeTypes;
    }
    public void setAffectedNodeTypes(List<String> affectedNodeTypes) {
      this.affectedNodeTypes = affectedNodeTypes;
    }

  }

  static public class Action {
    private String name;
    private String type;
    private String description;
    private String srcWorkspace;
    private String srcPath;
    private boolean isDeep = true;
    private List<String> uuid;
    private List<String> nodeTypeName;
    private List<String> lifecyclePhase;
    private String roles;
    private String variables;
    private List<String> affectedNodeTypes;
    private List mixins = new ArrayList(10);

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSrcPath() { return srcPath; }
    public void setSrcPath(String srcPath) { this.srcPath = srcPath; }

    public String getSrcWorkspace() { return srcWorkspace; }
    public void setSrcWorkspace(String srcWorkspace) { this.srcWorkspace = srcWorkspace; }

    public boolean isDeep() { return isDeep; }
    public void setDeep(boolean isDeep) { this.isDeep = isDeep; }

    public List<String> getUuid() { return uuid; }
    public void setUuid(List<String> uuid) { this.uuid = uuid; }

    public List<String> getNodeTypeName() { return nodeTypeName; }
    public void setNodeTypeName(List<String> nodeTypeName) { this.nodeTypeName = nodeTypeName; }

    public List<String> getLifecyclePhase() { return lifecyclePhase; }
    public void setLifecyclePhase(List<String> lifecyclePhase) { this.lifecyclePhase = lifecyclePhase; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public List getMixins() { return mixins; }
    public void setMixins(List mixins) { this.mixins = mixins; }

    public List<String> getAffectedNodeTypes() {
      return affectedNodeTypes;
    }

    public void setAffectedNodeTypes(List<String> affectedNodeTypes) {
      this.affectedNodeTypes = affectedNodeTypes;
    }
  }

}
