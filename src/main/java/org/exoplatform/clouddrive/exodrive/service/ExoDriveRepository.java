/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.exodrive.service;

import static org.exoplatform.clouddrive.exodrive.service.FileStore.METADIR_NAME;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.METAFILE_DATEFORMAT;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.METAFILE_EXT;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_AUTHOR;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_CREATEDATE;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_ID;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_LASTUSER;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_MODIFIEDDATE;
import static org.exoplatform.clouddrive.exodrive.service.FileStore.META_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.exodrive.ExoDriveConnector;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;


/**
 * Exo Drive repository abstraction.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveServices.java 00000 Oct 18, 2012 pnedonosko $
 * 
 */
public class ExoDriveRepository {

  // *********** config constants ***********

  protected static final Log       LOG = ExoLogger.getLogger(ExoDriveRepository.class);

  protected final String           name;

  protected final File             baseDir;

  protected final String           baseUrl;

  protected final MimeTypeResolver mimeResolver;

  /**
   * @throws ExoDriveConfigurationException
   */
  ExoDriveRepository(String name, File baseDir, String baseUrl, MimeTypeResolver mimeResolver) throws ExoDriveConfigurationException {
    this.name = name;
    this.baseUrl = baseUrl;
    this.baseDir = baseDir;
    this.mimeResolver = mimeResolver;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the baseUrl
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  // ********* internal methods ************

  
  
  /**
   * Read file to {@link FileStore}. Here we assume the file exists and it's a {@link File}.
   * 
   * @param file
   * @param ownerName
   * @return
   * @throws ExoDriveException
   */
  protected FileStore readFile(File file, String ownerName) throws ExoDriveException {
    try {
      File metaDir = new File(file.getParent(), METADIR_NAME);
      File meta = new File(metaDir, file.getName() + METAFILE_EXT);
      Properties metap = new Properties();

      InputStream in = new FileInputStream(meta);
      try {
        metap.load(in);
      } finally {
        in.close();
      }

      String id = metap.getProperty(META_ID);
      String type = metap.getProperty(META_TYPE);
      String author = metap.getProperty(META_AUTHOR);
      String lastUser = metap.getProperty(META_LASTUSER);

      try {
        Calendar createDate = METAFILE_DATEFORMAT.parseCalendar(metap.getProperty(META_CREATEDATE));
        Calendar modifiedDate = METAFILE_DATEFORMAT.parseCalendar(metap.getProperty(META_MODIFIEDDATE));

        if (id != null && type != null && author != null && lastUser != null && createDate != null
            && modifiedDate != null) {
          return new FileStore(file,
                               id,
                               fileLink(ownerName, file.getName()),
                               type,
                               author,
                               author,
                               createDate,
                               createDate);
        }
      } catch (ParseException e) {
        throw new ExoDriveException("Cloud file storage " + file + " metadata inconsistent.", e);
      }

      throw new ExoDriveException("Cloud file storage " + file + " metadata inconsistent.");
    } catch (IOException e) {
      throw new ExoDriveException("Cannot read cloud file from storage " + file, e);
    }
  }

  /**
   * @return the baseDir
   */
  File getBaseDir() {
    return baseDir;
  }

  File userRoot(String user) {
    return new File(baseDir, user);
  }

  String fileLink(String ownerName, String name) {
    return baseUrl + "/" + ownerName + "/" + name;
  }

  String generateId(File parentDir, String name) {
    String idpath = parentDir.getAbsolutePath() + "_secret_" + name;
    return UUID.nameUUIDFromBytes(idpath.getBytes()).toString();
  }

  // ********* service methods *************

  /**
   * Tells if a file or folder exists in the user drive. If name is null then it answers if the user
   * drive exists at all.
   * 
   * @param ownerName
   * @param name
   * @return
   */
  public boolean exists(String ownerName, String name) {
    File file;
    if (name != null) {
      file = new File(userRoot(ownerName), name);
    } else {
      file = userRoot(ownerName);
    }
    return file.exists();
  }

  /**
   * Answers if the user drive exists.
   * 
   * @param ownerName
   * @return
   */
  public boolean userExists(String ownerName) {
    File file = userRoot(ownerName);
    return file.exists();
  }

  public boolean createUser(String ownerName) {
    File file = userRoot(ownerName);
    file.mkdirs();
    return file.exists();
  }
  
  public boolean removeUser(String ownerName) throws ExoDriveException {
    File file = userRoot(ownerName);
    for (FileStore fs : listFiles(ownerName)) {
      fs.remove();
    }
    return file.delete();
  }

  public FileStore create(String ownerName, String name, String type, Calendar createDate) throws ExoDriveException {
    File parentDir = userRoot(ownerName);
    if (parentDir.exists()) {
      File file = new File(parentDir, name);
      try {
        if (file.createNewFile()) {
          File metaDir = new File(parentDir, METADIR_NAME);
          metaDir.mkdirs();

          File meta = new File(metaDir, name + METAFILE_EXT);

          String id = generateId(parentDir, name);

          String mimeType = type != null ? type : mimeResolver.getMimeType(name);

          Properties metap = new Properties();
          metap.put(META_ID, id);
          metap.put(META_TYPE, mimeType);
          metap.put(META_AUTHOR, ownerName);
          metap.put(META_LASTUSER, ownerName);
          metap.put(META_CREATEDATE, METAFILE_DATEFORMAT.format(createDate.getTime()));
          metap.put(META_MODIFIEDDATE, METAFILE_DATEFORMAT.format(createDate.getTime()));

          FileStore local =
              new FileStore(file,
                            id,
                            fileLink(ownerName, file.getName()),
                            mimeType,
                            ownerName,
                            ownerName,
                            createDate,
                            createDate);

          OutputStream out = new FileOutputStream(meta);
          try {
            metap.store(out, "Metadata for " + file.getAbsolutePath() + ". Generated at "
                + METAFILE_DATEFORMAT.format(Calendar.getInstance().getTime()));
          } finally {
            out.close();
          }

          return local;
        } else {
          throw new ExoDriveException("Local cloud drive exists with the same name " + file);
        }
      } catch (IOException ioe) {
        throw new ExoDriveException("Cannot create cloud file in storage " + file, ioe);
      }
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested file not created '" + name
          + "' as parent not found " + parentDir.getAbsolutePath());
      throw new NotFoundException("User not found: " + ownerName + ". Requested file not created " + name);
    }
  }

  public FileStore read(String ownerName, String name) throws ExoDriveException {
    File parentDir = userRoot(ownerName);
    File file = new File(parentDir, name);
    if (file.exists() && file.isFile()) {
      return readFile(file, ownerName);
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested storage not exists "
          + parentDir.getAbsolutePath());
      throw new NotFoundException((parentDir.exists() ? "Cloud file " + name + " not found."
          : "User not found " + ownerName));
    }
  }

  public List<FileStore> listFiles(String ownerName) throws ExoDriveException {
    List<FileStore> res = new ArrayList<FileStore>();
    File parentDir = userRoot(ownerName);
    File[] userFiles = parentDir.listFiles();
    if (userFiles != null) {
      for (File f : userFiles) {
        if (f.isFile()) {
          res.add(readFile(f, ownerName));
        }
      }
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested storage not exists "
          + parentDir.getAbsolutePath());
      throw new NotFoundException("User not found " + ownerName);
    }
    return res;
  }

  public List<FileStore> listFiles(String ownerName, FileStore parentDir) throws ExoDriveException {
    List<FileStore> res = new ArrayList<FileStore>();
    File ownerDir = userRoot(ownerName);
    if (parentDir.getFile().getAbsolutePath().startsWith(ownerDir.getAbsolutePath())) {
      File[] userFiles = parentDir.getFile().listFiles();
      if (userFiles != null) {
        for (File f : parentDir.getFile().listFiles()) {
          if (f.isFile()) {
            res.add(readFile(f, ownerName));
          }
        }
      } else {
        LOG.warn("User not found: " + ownerName + ". Requested storage not exists "
            + parentDir.getFile().getAbsolutePath());
        throw new NotFoundException("User not found " + ownerName);
      }
      return res;
    } else {
      throw new NotFoundException("Not user '" + ownerName + "' folder " + parentDir.getFile());
    }
  }
}
