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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileStore.java 00000 Oct 5, 2012 pnedonosko $
 */
public class FileStore {

  public static final String         METADIR_NAME        = ".exodrive";

  public static final String         METAFILE_EXT        = ".edi";

  public static final FileDateFormat METAFILE_DATEFORMAT = new FileDateFormat("yyyy-MM-dd-hh:mm:ss.SSSZ");

  public static final String         META_ID             = "id";

  public static final String         META_TYPE           = "type";

  public static final String         META_AUTHOR         = "author";

  public static final String         META_LASTUSER       = "lastuser";

  public static final String         META_CREATEDATE     = "createdate";

  public static final String         META_MODIFIEDDATE   = "modifiydate";

  public static final String         TYPE_FOLDER         = "application/vnd.exoplatform.exodrive-folder";

  public static final String         FILE_SEPARATOR      = "/";                                           // the
                                                                                                          // same
                                                                                                          // as
                                                                                                          // in
                                                                                                          // JCR!

  /**
   * I/O buffer size for internal operations (16K).
   */
  public static final int            IOBUFFER_SIZE       = 16 * 1024;                                     // 16K

  static class FileDateFormat extends SimpleDateFormat {

    private static final long serialVersionUID = 2373038959548713771L;

    FileDateFormat(String pattern) {
      super(pattern);
    }

    Calendar parseCalendar(String source) throws ParseException {
      super.parse(source);
      return (Calendar) this.calendar.clone();
    }
  }

  protected final File     local;

  protected final String   id;

  protected final String   link;

  protected final String   type;

  protected final String   author;

  protected final Calendar createDate;

  protected String         lastUser;

  protected Calendar       modifiedDate;

  /**
   * @param local
   * @param id
   * @param type
   * @param author
   * @param lastUser
   * @param createDate
   * @param modifiedDate
   */
  FileStore(File local,
            String id,
            String link,
            String type,
            String author,
            String lastUser,
            Calendar createDate,
            Calendar modifiedDate) {
    super();
    this.local = local;
    this.id = id;
    this.link = link;
    this.type = type;
    this.author = author;
    this.lastUser = lastUser;
    this.createDate = createDate;
    this.modifiedDate = modifiedDate;
  }

  public InputStream read() throws ExoDriveException {
    if (local.isFile()) {
      try {
        return new FileInputStream(local);
      } catch (FileNotFoundException e) {
        throw new ExoDriveException("File not found " + local.getName(), e);
      }
    } else {
      throw new ExoDriveException("Not a file " + local.getAbsolutePath());
    }
  }

  public long write(InputStream content) throws ExoDriveException {
    if (local.isFile()) {
      // TODO ensure concurrent writes safe (use file locks)
      try {
        // Using NIO for write. Grabbed from JCR's ValueFileIOHelper.
        FileOutputStream out = new FileOutputStream(local);
        try {
          FileChannel outch = out.getChannel();
          ReadableByteChannel inch;

          // compare classes as in Java6 Channels.newChannel(), Java5 has a bug
          // in newChannel().
          if (content instanceof FileInputStream && FileInputStream.class.equals(content.getClass())) {
            // it's user file
            inch = ((FileInputStream) content).getChannel();
          } else {
            // it's user stream (not a file)
            inch = Channels.newChannel(content);
          }

          long size = 0;
          int r = 0;
          ByteBuffer buff = ByteBuffer.allocate(IOBUFFER_SIZE);
          buff.clear();
          while ((r = inch.read(buff)) >= 0) {
            buff.flip();

            // copy all
            do {
              outch.write(buff);
            } while (buff.hasRemaining());

            buff.clear();
            size += r;
          }

          outch.force(true); // force all data to FS

          // update file metadata
          update(author, Calendar.getInstance());
          return size;
        } finally {
          out.close();
        }
      } catch (IOException e) {
        throw new ExoDriveException("File cannot be saved " + local.getName(), e);
      }
    } else {
      throw new ExoDriveException("Cannot write to directory " + local.getAbsolutePath());
    }
  }

  public void remove() throws ExoDriveException {
    // TODO removal of directories
    if (local.exists()) {
      File metaDir = new File(local.getParent(), METADIR_NAME);
      File meta = new File(metaDir, local.getName() + METAFILE_EXT);
      if (!meta.delete()) {
        meta.deleteOnExit();
      }

      if (!local.delete()) {
        local.deleteOnExit();
      }
    } else {
      throw new ExoDriveException("File not found " + local.getName());
    }
  }

  /**
   * Internal helper.
   * 
   * @param lastUser
   * @param modifiedDate
   * @throws ExoDriveException
   */
  void update(String lastUser, Calendar modifiedDate) throws ExoDriveException {
    try {
      if (local.exists() && local.isFile()) {
        // do update
        File metaDir = new File(local.getParent(), METADIR_NAME);
        File meta = new File(metaDir, local.getName() + METAFILE_EXT);

        Properties metap = new Properties();
        InputStream in = new FileInputStream(meta);
        try {
          metap.load(in);
        } finally {
          in.close();
        }

        metap.put(META_LASTUSER, lastUser);
        metap.put(META_MODIFIEDDATE, METAFILE_DATEFORMAT.format(modifiedDate.getTime()));

        if (lastUser != null && modifiedDate != null) {
          this.lastUser = lastUser;
          this.modifiedDate = modifiedDate;

          OutputStream out = new FileOutputStream(meta);
          try {
            metap.store(out,
                        "Metadata for " + local.getAbsolutePath() + ". Generated at "
                            + METAFILE_DATEFORMAT.format(Calendar.getInstance().getTime()));
          } finally {
            out.close();
          }
        } else {
          throw new ExoDriveException("Cloud file " + local.getName() + " metadata cannot be null.");
        }
      } else {
        throw new ExoDriveException("Local cloud drive not exists or it's a folder " + local.getName());
      }
    } catch (IOException ioe) {
      throw new ExoDriveException("Cannot create cloud file in storage " + local.getName(), ioe);
    }
  }

  // ************** public **************

  /**
   * @return the name
   */
  public String getName() {
    return local.getName();
  }

  /**
   * @return is folder
   */
  public boolean isFolder() {
    return local.isDirectory();
  }

  /**
   * @return the lastUser
   */
  public String getLastUser() {
    return lastUser;
  }

  /**
   * @return the modifiedDate
   */
  public Calendar getModifiedDate() {
    return modifiedDate;
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @return the link
   */
  public String getLink() {
    return link;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * @return the createDate
   */
  public Calendar getCreateDate() {
    return createDate;
  }

  // ******** protected *********

  /**
   * @return the local
   */
  File getFile() {
    return local;
  }

}
