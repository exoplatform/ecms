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
package org.exoplatform.clouddrive.exodrive.service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.picocontainer.Startable;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.exodrive.ExoDriveConnector;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Server-side for eXo Drive implementation of Cloud Drive. It is independent to
 * {@link CloudDriveService} component handling read and write operations to
 * local files as to remote cloud drive.</br>
 * This service consumed by {@link ExoDriveConnector}. Created by The eXo
 * Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveService.java 00000 Oct 5, 2012 pnedonosko $
 */
public class ExoDriveService implements Startable {

  // *********** config constants ***********

  public static final String                      CONFIG_ROOT_DIR                 = "root-dir";

  public static final String                      CONFIG_BASE_URL                 = "base-url";

  public static final String                      PATTERN_REPOSITORY_CURRENT_NAME = "REPOSITORY_CURRENT_NAME";

  protected static final Log                      LOG                             = ExoLogger.getLogger(ExoDriveService.class);

  protected final Map<String, String>             config;

  protected final Map<String, ExoDriveRepository> repositories                    = new HashMap<String, ExoDriveRepository>();

  protected final File                            storageRoot;

  protected final String                          baseUrl;

  protected final MimeTypeResolver                mimeResolver;

  /**
   * 
   */
  public ExoDriveService(InitParams params) throws ExoDriveConfigurationException {

    PropertiesParam param = params.getPropertiesParam("storage-configuration");

    if (param != null) {
      config = Collections.unmodifiableMap(param.getProperties());

      String baseUrl = config.get(CONFIG_BASE_URL);
      if (baseUrl != null) {
        this.baseUrl = baseUrl;
      } else {
        throw new ExoDriveConfigurationException("Configuration of " + CONFIG_BASE_URL + " required.");
      }

      String rootPath = config.get(CONFIG_ROOT_DIR);
      if (rootPath != null) {
        this.storageRoot = new File(rootPath);
        if (!storageRoot.exists()) {
          this.storageRoot.mkdirs();
        }

        this.mimeResolver = new MimeTypeResolver();
        this.mimeResolver.setDefaultMimeType("application/octet-stream");
      } else {
        throw new ExoDriveConfigurationException("Configuration of " + CONFIG_ROOT_DIR + " required.");
      }

    } else {
      throw new ExoDriveConfigurationException("Property parameters storage-configuration required.");
    }
  }

  public ExoDriveRepository read(String name) throws ExoDriveException {
    ExoDriveRepository repo = repositories.get(name);
    if (repo == null) {
      File repoDir = new File(storageRoot, name);
      if (repoDir.exists()) {
        String repoUrl = baseUrl.replace(PATTERN_REPOSITORY_CURRENT_NAME, name);
        repo = new ExoDriveRepository(name, repoDir, repoUrl, mimeResolver);
        repositories.put(name, repo);
      } else {
        throw new ExoDriveException("eXo Drive repository not found " + name);
      }
    }
    return repo;
  }

  public ExoDriveRepository open(String name) throws ExoDriveConfigurationException {
    ExoDriveRepository repo = repositories.get(name);
    if (repo == null) {
      File repoDir = new File(storageRoot, name);
      repoDir.mkdirs();

      String repoUrl = baseUrl.replace(PATTERN_REPOSITORY_CURRENT_NAME, name);

      repo = new ExoDriveRepository(name, repoDir, repoUrl, mimeResolver);
      repositories.put(name, repo);
    }
    return repo;
  }

  public boolean remove(String name) throws ExoDriveConfigurationException {
    ExoDriveRepository repo = repositories.remove(name);
    if (repo != null) {
      // TODO delete recursive and fail if not possible remove
      return repo.getBaseDir().delete();
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }

}
