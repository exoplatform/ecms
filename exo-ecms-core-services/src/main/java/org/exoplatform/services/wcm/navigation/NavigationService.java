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
package org.exoplatform.services.wcm.navigation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.mop.Visibility;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Nov 21, 2008  
 */
public class NavigationService {

  public String getNavigationsAsJSON(List<PageNavigation> navigations) throws Exception {
	  String JSONnavigation = createJsonTree(navigations).toString();
//    PortalNavigation portalNavigation = new PortalNavigation(navigations);
//    JsonValue jsonValue = new JsonGeneratorImpl().createJsonObject(portalNavigation);
//    String JSONnavigation = jsonValue.toString();
//    JSONnavigation = JSONnavigation.substring(1, JSONnavigation.length() - 1);
//    JSONnavigation = JSONnavigation.replaceFirst("\"navigations\":", "");
    return JSONnavigation;
  }
  
  private StringBuffer createJsonTree(List<PageNavigation> navigations) {
	  StringBuffer js = new StringBuffer();
	  js.append("[");
	  boolean first = true;
	  for (PageNavigation navigation:navigations) {
		  if (!first) js.append(",");
		  first=false;
		  js.append("{");
		  js.append("\"ownerId\":\""+navigation.getOwnerId()+"\",");
		  js.append("\"ownerType\":\""+navigation.getOwnerType()+"\",");
		  js.append("\"owner\":\""+navigation.getOwner()+"\",");
		  js.append("\"priority\":\""+navigation.getPriority()+"\",");
		  js.append("\"nodes\":"+addJsonNodes(navigation.getNodes()));
		  js.append("}");
	  }
	  js.append("]");
	  
	  return js;
  }

  private String addJsonNodes(List<PageNode> nodes) {
	  StringBuffer js = new StringBuffer();
	  js.append("[");
	  boolean first = true;
	  for (PageNode node:nodes) {
		  if (isVisibleNode(node)) {
			  if (!first) js.append(",");
			  first=false;
			  js.append("{");
			  js.append("\"icon\":"+(node.getIcon()!=null?"\""+node.getIcon()+"\"":"null")+",");
			  js.append("\"label\":\""+node.getLabel()+"\",");
			  js.append("\"name\":\""+node.getName()+"\",");
			  js.append("\"resolvedLabel\":\""+node.getResolvedLabel()+"\",");
			  js.append("\"uri\":\""+node.getUri()+"\",");
			  js.append("\"nodes\":"+addJsonNodes(node.getNodes()));
			  js.append("}");
		  }
	  }
	  js.append("]");
	  return js.toString();
  }
  
  private boolean isVisibleNode(PageNode node) {
	  if (!node.getVisibility().equals(Visibility.DISPLAYED) && !node.getVisibility().equals(Visibility.TEMPORAL)) return false;
	  Date now = new Date(System.currentTimeMillis());
	  if (node.getStartPublicationDate()!=null && node.getStartPublicationDate().after(now)) return false; 
	  if (node.getEndPublicationDate()!=null && node.getEndPublicationDate().before(now)) return false; 
	  
	  return true;
  }

  /**
   * The Class PortalNavigation.
   */
  public static class PortalNavigation {

    private List<PageNavigation> navigations = new ArrayList<PageNavigation>();

    /**
     * Instantiates a new portal navigation.
     * 
     * @param list the list
     */
    public PortalNavigation(List<PageNavigation> list) {
      this.navigations = list;
    }    

    /**
     * Gets the navigations.
     * 
     * @return the navigations
     */
    public List<PageNavigation> getNavigations() { return this.navigations; }

    /**
     * Sets the navigations.
     * 
     * @param list the new navigations
     */
    public void setNavigations(List<PageNavigation> list) { this.navigations = list; }
  }      
}
