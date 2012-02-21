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
package org.exoplatform.ecm.webui.comparator;

import java.util.Comparator;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.ext.audit.AuditHistory;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 20, 2009
 */
public class StringComparator implements Comparator<Node> {

  public static final String ASCENDING_ORDER = "Ascending" ;
  public static final String DESCENDING_ORDER = "Descending" ;
  private String order_;
  private String type_ ;
  private static final Log LOG  = ExoLogger.getLogger("admin.StringComparator");
  public StringComparator(String order, String type) {
    this.order_ = order ;
    this.type_ = type;
  }

  private String getVersionName(Node node) throws Exception {
    return node.getBaseVersion().getName() ;
  }

  private String versionName(Node node) throws Exception {
    String returnString = "";
    returnString = String.valueOf(Utils.isVersionable(node));
    if (Utils.isVersionable(node) && !getVersionName(node).equals("jcr:rootVersion")) {
      returnString = "(" + getVersionName(node) + ")" ;
    }
    return returnString;
  }

  private boolean hasAuditHistory(Node node) throws Exception{
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    AuditService auServ = (AuditService)container.getComponentInstanceOfType(AuditService.class);
    return auServ.hasHistory(node);
  }

  private int getNumAuditHistory(Node node) throws Exception{
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    AuditService auServ = (AuditService)container.getComponentInstanceOfType(AuditService.class);
    if (auServ.hasHistory(node)) {
      AuditHistory auHistory = auServ.getHistory(node);
      return (auHistory.getAuditRecords()).size();
    }
    return 0;
  }

  private String getAuditing(Node node) throws Exception {
    String returnString = "";
    returnString = String.valueOf(Utils.isAuditable(node));
    if (Utils.isAuditable(node)&& hasAuditHistory(node)) {
      returnString = "(" + getNumAuditHistory(node) + ")";
    }
    return returnString;
  }

  private int compare(String s1, String s2) {
    if (ASCENDING_ORDER.equals(order_)) {
      return s1.compareTo(s2) ;
    }
    return s2.compareTo(s1) ;
  }

  public int compare(Node node1, Node node2) {
    int returnCompare = 0;
    try {
      if (type_.equals("Owner")) {
        if (node1.hasProperty("exo:owner")  && node2.hasProperty("exo:owner")) {
          String owner1 = node1.getProperty("exo:owner").getString();
          String owner2 = node2.getProperty("exo:owner").getString();
          returnCompare = compare(owner1, owner2);
        }
      } else if (type_.equals("Versionable")) {
        String versionNode1 = versionName(node1);
        String versionNode2 = versionName(node2);
        returnCompare = compare(versionNode1, versionNode2);
      } else if (type_.equals("Auditing")) {
        String auditing1 = getAuditing(node1);
        String auditing2 = getAuditing(node2);
        returnCompare = compare(auditing1, auditing2);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return returnCompare;
  }
}
