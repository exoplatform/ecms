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
package org.exoplatform.ecm.jcr.model;

public class ClipboardCommand {

  public static final String COPY      = "copy";

  public static final String CUT       = "cut";

  public static final String ADDSYMLINK = "AddSymLink";

  private String             type;

  private String             srcPath;

  private String             wsName    = null;


  public String getSrcPath() {
    return srcPath;
  }

  public void setSrcPath(String srcPath) {
    this.srcPath = srcPath;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setWorkspace(String ws) {
    wsName = ws;
  }

  public String getWorkspace() {
    return wsName;
  }
}
