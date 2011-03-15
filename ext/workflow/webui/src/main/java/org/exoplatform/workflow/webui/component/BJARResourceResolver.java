/*
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
 */
package org.exoplatform.workflow.webui.component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.workflow.FileDefinition;
import org.exoplatform.services.workflow.Task;
import org.exoplatform.services.workflow.WorkflowServiceContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Jan 9, 2009
 */
public class BJARResourceResolver extends ResourceResolver {

  private WorkflowServiceContainer service_;

  public BJARResourceResolver(WorkflowServiceContainer service) {
    service_ = service;
  }

  public URL getResource(String url) throws Exception {
    throw new Exception("This method is not  supported");
  }

  public InputStream getInputStream(String fileLocation) throws Exception  {
    String[] infos = StringUtils.split(fileLocation, ":/");
    if (infos.length == 2) {
      Task taskInstance = service_.getTask(infos[0]);
      FileDefinition fD = service_.getFileDefinitionService().retrieve(taskInstance.getProcessId());
      byte[] file = fD.getEntry(infos[1]);
      return new ByteArrayInputStream(file);
    }
    throw new Exception("Cannot retrieve data in process "
        + fileLocation
        + "Make sure you have a valid location");
  }

  public List<URL> getResources(String url) throws Exception {
    throw new Exception("This method is not  supported");
  }

  public List<InputStream> getInputStreams(String url) throws Exception {
    ArrayList<InputStream>  inputStreams = new ArrayList<InputStream>(1);
    inputStreams.add(getInputStream(url));
    return inputStreams;
  }

  public boolean isModified(String url, long lastAccess) { return false; }

  public String createResourceId(String url) { return  url; }

  public String getResourceScheme() {  return "jcr:"; }
}
