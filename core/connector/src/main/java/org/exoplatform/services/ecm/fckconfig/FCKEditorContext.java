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
package org.exoplatform.services.ecm.fckconfig;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
@SuppressWarnings({ "hiding", "serial" })
public class FCKEditorContext extends HashMap<String, Object> {

  public String getPortalName() { return (String)this.get("portalName"); }
  public void setPortalName(String portalName) { this.put("portalName",portalName); }

  public String getSkinName() {return (String)this.get("skinName"); }
  public void setSkinName(String skinName) { this.put("skinName",skinName);}

  public String getRepository() { return (String)this.get("repository"); }
  public void setRepository(String repository) { this.put("repository",repository); }

  public String getWorkspace() {return (String)this.get("workspace"); }
  public void setWorkspace(String worksapce) { this.put("workspace",worksapce); }

  public String getCurrentNodePath() { return (String) this.get("jcrPath"); }
  public void setCurrentNodePath(String path) { this.put("jcrPath",path); }
 }
