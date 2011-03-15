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

package org.exoplatform.services.workflow.impl.jbpm;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.services.workflow.FileDefinition;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 18, 2007
 */
public class FileDefinitionWapper implements FileDefinition {

  private org.jbpm.file.def.FileDefinition fileDefinition_ ;

  public FileDefinitionWapper(org.jbpm.file.def.FileDefinition fileDef) {
    fileDefinition_ = fileDef;
  }

  public void deploy() throws Exception {

  }

  public String getCustomizedView(String stateName) {
    return null;
  }

  public Hashtable<String, byte[]> getEntries() {
    return null;
  }

  public byte[] getEntry(String path) throws Exception {
    return fileDefinition_.getBytes(path);
  }

  public String getProcessModelName() {
    return null;
  }

  public ResourceBundle getResourceBundle(String stateName, Locale locale) {
    return null;
  }

  public List<Map<String, Object>> getVariables(String stateName) {
    return null;
  }

  public boolean isDelegatedView(String stateName) {
    return false;
  }

  public boolean isFormDefined(String stateName) {
    return false;
  }

}
