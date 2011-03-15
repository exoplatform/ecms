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
package org.exoplatform.services.cms.templates.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author benjaminmestrallet
 */
public class TemplateConfig {

  private List<NodeType> nodeTypes = new ArrayList<NodeType>();
  private List<Template> templates = new ArrayList<Template>();

  private List<NodeType> excludeRenderTemplateNodeTypes = new ArrayList<NodeType>();

  public List<NodeType> getNodeTypes() {   return this.nodeTypes; }
  public void setNodeTypes(List<NodeType> s) {  this.nodeTypes = s; }

  public List<Template> getTemplates() {   return this.templates; }
  public void setTemplates(List<Template> s) {  this.templates = s; }

  public List<NodeType> getExcludeRenderTemplateNodeTypes() {
    return excludeRenderTemplateNodeTypes;
  }

  public void setExcludeRenderTemplateNodeTypes(List<NodeType> excludeRenderTemplateNodeTypes) {
    this.excludeRenderTemplateNodeTypes = excludeRenderTemplateNodeTypes;
  }

  static public class Template {

    private String templateFile;
    private String roles;

    public String[] getParsedRoles() { return StringUtils.split(this.roles, ";"); }

    public String getRoles() {return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getTemplateFile() { return templateFile; }
    public void setTemplateFile(String templateFile) { this.templateFile = templateFile; }
  }

  static public class NodeType {

    private String nodetypeName;
    private String label;
    private boolean documentTemplate ;
    private List referencedDialog;
    private List referencedView;
    private List referencedSkin;

    public NodeType(){
      referencedDialog = new ArrayList();
      referencedView = new ArrayList();
      referencedSkin = new ArrayList();
      documentTemplate = false ;
    }

    public String getNodetypeName() { return nodetypeName ; }
    public void setNodetypeName(String s) { nodetypeName = s ; }

    public String getLabel() { return label ; }
    public void setLabel(String s) { label = s ; }

    public List getReferencedDialog() { return referencedDialog; }
    public void setReferencedDialog(List referencedDialog) { this.referencedDialog = referencedDialog; }

    public List getReferencedView() { return referencedView; }
    public void setReferencedView(List referencedView) { this.referencedView = referencedView; }

    public List getReferencedSkin() { return referencedSkin; }
    public void setReferencedSkin(List referencedSkin) { this.referencedSkin = referencedSkin; }

    public boolean getDocumentTemplate() { return this.documentTemplate ; }
    public void setDocumentTemplate( boolean b) { this.documentTemplate = b ; }
  }

}
