/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PortalEnvironment.java 00000 Jan 16, 2014 pnedonosko $
 */
public class PortalEnvironment extends CloudDriveEnvironment {

  /**
   * The Class Settings.
   */
  protected class Settings {
    
    /** The context. */
    final RequestContext context;

    /** The prev context. */
    RequestContext       prevContext;

    /**
     * Instantiates a new settings.
     *
     * @param context the context
     */
    Settings(RequestContext context) {
      this.context = context;
    }
  }

  /** The config. */
  protected final Map<Command, Settings> config = Collections.synchronizedMap(new WeakHashMap<Command, Settings>());

  /**
   * Instantiates a new portal environment.
   */
  public PortalEnvironment() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(Command command) throws CloudDriveException {
    super.configure(command);
    config.put(command, new Settings(WebuiRequestContext.getCurrentInstance()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepare(Command command) throws CloudDriveException {
    super.prepare(command);
    Settings settings = config.get(command);
    if (settings != null) {
      settings.prevContext = WebuiRequestContext.getCurrentInstance();
      WebuiRequestContext.setCurrentInstance(settings.context);
    } else {
      throw new CloudDriveException(this.getClass().getName() + " setting not configured for " + command
          + " to be prepared.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cleanup(Command command) throws CloudDriveException {
    Settings settings = config.remove(command);
    if (settings != null) {
      WebuiRequestContext.setCurrentInstance(settings.prevContext);
    } else {
      throw new CloudDriveException(this.getClass().getName() + " setting not configured for " + command
          + " to be cleaned.");
    }
    super.cleanup(command);
  }

}
