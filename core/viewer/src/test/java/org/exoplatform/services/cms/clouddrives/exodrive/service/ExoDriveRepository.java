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
package org.exoplatform.services.cms.clouddrives.exodrive.service;

import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.FILE_SEPARATOR;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.METADIR_NAME;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.METAFILE_DATEFORMAT;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.METAFILE_EXT;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_AUTHOR;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_CREATEDATE;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_ID;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_LASTUSER;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_MODIFIEDDATE;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.META_TYPE;
import static org.exoplatform.services.cms.clouddrives.exodrive.service.FileStore.TYPE_FOLDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Exo Drive repository abstraction. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExoDriveServices.java 00000 Oct 18, 2012 pnedonosko $
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
  ExoDriveRepository(String name, File baseDir, String baseUrl, MimeTypeResolver mimeResolver)
      throws ExoDriveConfigurationException {
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
   * Read file to {@link FileStore}. Here we assume the file exists and it's a
   * {@link File}.
   * 
   * @param ownerName
   * @param file
   * @return
   * @throws ExoDriveException
   */
  protected FileStore openStore(String ownerName, File file) throws ExoDriveException {
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

        if (id != null && type != null && author != null && lastUser != null && createDate != null && modifiedDate != null) {
          return new FileStore(file, id, fileLink(ownerName, file.getName()), type, author, author, createDate, createDate);
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

  String fileLink(String ownerName, String path) {
    return baseUrl + "/" + ownerName + "/" + path;
  }

  String generateId(File parentDir, String path) {
    String idpath = parentDir.getAbsolutePath() + "_secret_" + path;
    return UUID.nameUUIDFromBytes(idpath.getBytes()).toString();
  }

  File findFile(File userDir, String path) throws ExoDriveException {
    List<String> pathFiles = Arrays.asList(path.split(FILE_SEPARATOR));
    File parent = userDir;
    File file = null;
    for (Iterator<String> piter = pathFiles.iterator(); piter.hasNext();) {
      String pfn = piter.next();
      File pf = new File(parent, pfn);
      if (piter.hasNext()) {
        if (pf.exists()) {
          if (pf.isDirectory()) {
            // ok, it's a folder in the file path
            parent = pf;
          } else {
            throw new ExoDriveException("File found on the parent path '" + path + "', folder expected: " + pf.getPath());
          }
        } else {
          throw new ExoDriveException("Parent path not found '" + path + "', folder doesn't exist: " + pf.getPath());
        }
      } else {
        if (pf.exists()) {
          throw new ExoDriveException("File already exists " + pf.getPath());
        } else {
          file = pf;
        }
      }
    }
    return file;
  }

  // ********* service methods *************

  /**
   * Tells if a file or folder exists in the user drive. If name is null then it
   * answers if the user drive exists at all.
   * 
   * @param ownerName
   * @param path
   * @return
   */
  public boolean exists(String ownerName, String path) {
    File file;
    if (path != null) {
      file = new File(userRoot(ownerName), path);
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

  public FileStore create(String ownerName, String path, String type, Calendar createDate) throws ExoDriveException {
    File userDir = userRoot(ownerName);
    if (userDir.exists()) {
      File file = findFile(userDir, path);
      if (file == null) {
        throw new ExoDriveException("File cannot be created with such path '" + path + "'");
      }

      try {
        if (TYPE_FOLDER.equals(type)) {
          // create folder
          if (!file.mkdir()) {
            throw new ExoDriveException("Cannot crate new folder " + file.getPath());
          }
        } else {
          // create file
          if (!file.createNewFile()) {
            throw new ExoDriveException("Cannot create new file" + file.getPath());
          }
        }
        File metaDir = new File(userDir, METADIR_NAME);
        metaDir.mkdirs();

        File meta = new File(metaDir, path + METAFILE_EXT);

        String id = generateId(userDir, path);

        String mimeType = type != null ? type : mimeResolver.getMimeType(path);

        Properties metap = new Properties();
        metap.put(META_ID, id);
        metap.put(META_TYPE, mimeType);
        metap.put(META_AUTHOR, ownerName);
        metap.put(META_LASTUSER, ownerName);
        metap.put(META_CREATEDATE, METAFILE_DATEFORMAT.format(createDate.getTime()));
        metap.put(META_MODIFIEDDATE, METAFILE_DATEFORMAT.format(createDate.getTime()));

        FileStore local = new FileStore(file,
                                        id,
                                        fileLink(ownerName, file.getName()),
                                        mimeType,
                                        ownerName,
                                        ownerName,
                                        createDate,
                                        createDate);

        OutputStream out = new FileOutputStream(meta);
        try {
          metap.store(out,
                      "Metadata for " + file.getAbsolutePath() + ". Generated at "
                          + METAFILE_DATEFORMAT.format(Calendar.getInstance().getTime()));
        } finally {
          out.close();
        }

        return local;
      } catch (IOException ioe) {
        throw new ExoDriveException("Cannot create file in storage " + file, ioe);
      }
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested file not created '" + path + "' as parent not found "
          + userDir.getAbsolutePath());
      throw new NotFoundException("User not found: " + ownerName + ". Requested file not created " + path);
    }
  }

  public FileStore read(String ownerName, String path) throws ExoDriveException {
    File userDir = userRoot(ownerName);
    File file = findFile(userDir, path);
    if (file != null && file.exists()) {
      return openStore(ownerName, file);
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested storage not exists " + userDir.getAbsolutePath());
      throw new NotFoundException((userDir.exists() ? "Cloud file " + path + " not found." : "User not found " + ownerName));
    }
  }

  public List<FileStore> listFiles(String ownerName) throws ExoDriveException {
    List<FileStore> res = new ArrayList<FileStore>();
    File userDir = userRoot(ownerName);
    File[] userFiles = userDir.listFiles();
    if (userFiles != null) {
      for (File f : userFiles) {
        if (!f.isDirectory()) {
          res.add(openStore(ownerName, f));
        }
      }
    } else {
      LOG.warn("User not found: " + ownerName + ". Requested storage not exists " + userDir.getAbsolutePath());
      throw new NotFoundException("User not found " + ownerName);
    }
    return res;
  }

  public List<FileStore> listFiles(String ownerName, FileStore parentDir) throws ExoDriveException {
    File userDir = userRoot(ownerName);
    if (parentDir.getFile().getAbsolutePath().startsWith(userDir.getAbsolutePath())) {
      List<FileStore> res = new ArrayList<FileStore>();
      if (parentDir.isFolder()) {
        throw new NotFoundException("Parent not a folder " + parentDir.getFile().getAbsolutePath());
      } else {
        File[] userFiles = parentDir.getFile().listFiles();
        if (userFiles != null) {
          for (File f : userFiles) {
            if (!f.isDirectory()) {
              res.add(openStore(ownerName, f));
            }
          }
        } else {
          throw new NotFoundException("Cannot read parent folder " + parentDir.getFile().getAbsolutePath());
        }
        return res;
      }
    } else {
      throw new NotFoundException("Not user '" + ownerName + "' folder " + parentDir.getFile());
    }
  }
}
