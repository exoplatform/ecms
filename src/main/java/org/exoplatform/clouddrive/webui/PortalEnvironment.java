/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.webui;

import org.exoplatform.clouddrive.CloudDrive.Command;
import org.exoplatform.clouddrive.CloudDriveEnvironment;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.webui.PortalEnvironment.Settings;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PortalEnvironment.java 00000 Jan 16, 2014 pnedonosko $
 * 
 */
public class PortalEnvironment extends CloudDriveEnvironment {

  protected class Settings {
    final RequestContext context;

    RequestContext       prevContext;

    Settings(RequestContext context) {
      this.context = context;
    }
  }

  protected final Map<Command, Settings> config = new HashMap<Command, Settings>();
  
  /**
   * 
   */
  public PortalEnvironment() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(Command command) throws CloudDriveException {
    config.put(command, new Settings(WebuiRequestContext.getCurrentInstance()));
    super.configure(command);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepare(Command command) throws CloudDriveException {
    Settings settings = config.get(command);
    
    settings.prevContext = WebuiRequestContext.getCurrentInstance();
    WebuiRequestContext.setCurrentInstance(settings.context);
    super.prepare(command);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cleanup(Command command) throws CloudDriveException {
    Settings settings = config.get(command);
    
    WebuiRequestContext.setCurrentInstance(settings.prevContext);
    super.cleanup(command);
  }

}
