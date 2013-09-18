/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.control.filter;

import java.security.AccessControlException;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;


/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */
public class IsEditableFilter extends UIExtensionAbstractFilter {

  public IsEditableFilter() {
    this("UIActionBar.msg.not-support");
  }

  public IsEditableFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (context == null) return true;
    Node currentNode = (Node) context.get(Node.class.getName());
    String nodeType;
    if(currentNode.hasProperty("exo:presentationType")) {
      nodeType = currentNode.getProperty("exo:presentationType").getString();
    }else {
      nodeType = currentNode.getPrimaryNodeType().getName();
    }
    for(String type:Utils.NON_EDITABLE_NODETYPES) {
      if(type.equalsIgnoreCase(nodeType)) return false;
    }
    String userName = Util.getPortalRequestContext().getRemoteUser();
    try{
      TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      templateService.getTemplatePathByUser(true, nodeType, userName);
    }catch(AccessControlException e){
      return false;
    }
    return true;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
    if (context == null) return;
    Node currentNode = (Node) context.get(Node.class.getName());
    Object[] arg = { currentNode.getPath() };
    createUIPopupMessages(context, messageKey, arg);
  }
}
