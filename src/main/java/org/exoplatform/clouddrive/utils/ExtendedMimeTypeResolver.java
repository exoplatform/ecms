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
package org.exoplatform.clouddrive.utils;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Extended copy of eXo Kernel's MimeTypeResolver.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ExtendedMimeTypeResolver.java 00000 Nov 22, 2014 pnedonosko $
 * 
 */
public class ExtendedMimeTypeResolver {

  /** The Constant LOG. */
  protected static final Log LOG         = ExoLogger.getLogger(ExtendedMimeTypeResolver.class);

  /**
   * A mime-type property denoting a mode for viewer/editor of this type.
   */
  public static final String X_TYPE_MODE = "x-type-mode";

  /**
   * The Class MimeTypeMap.
   */
  public static class MimeTypeMap extends BaseComponentPlugin {

    /** The paths. */
    protected final List<String> paths;

    /**
     * Instantiates a new mime type map.
     *
     * @param params the params
     * @throws ConfigurationException the configuration exception
     */
    public MimeTypeMap(InitParams params) throws ConfigurationException {
      ValuesParam param = params.getValuesParam("mimetypes-properties");
      if (param != null) {
        ArrayList<String> paths = new ArrayList<String>();
        for (Object v : param.getValues()) {
          if (v instanceof String) {
            paths.add((String) v);
          }
        }
        this.paths = paths;
      } else {
        throw new ConfigurationException("Values param mimetypes-properties required in "
            + this.getClass().getName() + " plugin");
      }
    }

    /**
     * Gets the paths.
     *
     * @return the path
     */
    public List<String> getPaths() {
      return paths;
    }
  }

  /** The config service. */
  protected final ConfigurationManager      configService;

  /** The mime types. */
  protected final Map<String, List<String>> mimeTypes  = new HashMap<String, List<String>>();

  /** The extentions. */
  protected final Map<String, List<String>> extentions = new HashMap<String, List<String>>();

  /** The modes. */
  protected final Map<String, Set<String>>  modes      = new LinkedHashMap<String, Set<String>>();

  /** The resolver. */
  protected final MimeTypeResolver          resolver;

  /**
   * Instantiates a new extended mime type resolver.
   *
   * @param configService the config service
   * @param params the params
   * @throws NullPointerException the null pointer exception
   */
  public ExtendedMimeTypeResolver(ConfigurationManager configService, InitParams params) throws NullPointerException {
    this.configService = configService;
    this.resolver = new MimeTypeResolver();
  }

  /**
   * Adds the plugin.
   *
   * @param typesMap the types map
   */
  public void addPlugin(final MimeTypeMap typesMap) {
    try {
      SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Void>() {
        public Void run() throws Exception {
          for (String path : typesMap.getPaths()) {
            try {
              Scanner scanner = null;
              InputStream stream = configService.getInputStream(path);
              if (stream != null) {
                scanner = new Scanner(stream, "ISO-8859-1");
              }
              if (scanner == null) {
                LOG.warn("Cannot read extended mimetypes from path " + path);
              } else {
                try {
                  while (scanner.hasNextLine()) {
                    processLine(scanner.nextLine());
                  }
                } finally {
                  scanner.close();
                }
              }
            } catch (IOException e) {
              throw new IOException("Error loadinng extended mimetypes from path " + path + ": "
                  + e.getMessage(), e);
            }
          }
          return null;
        }
      });
    } catch (IOException e) {
      throw new InternalError("Unable to load extended mimetypes: " + e.toString());
    }
  }

  /**
   * Get MIMEType which corresponds to file extension. If file extension is unknown the default
   * MIMEType will be returned. If there are more than one MIMETypes for specific extension the
   * first occurred in the list will be returned.
   *
   * @param filename the filename
   * @return String MIMEType
   */
  public String getMimeType(String filename) {
    String mimeType = resolver.getMimeType(filename);
    if (resolver.getDefaultMimeType().equals(mimeType)) {
      // default resolver didn't recognize the type
      // try guess from this resolver map
      String ext = filename.substring(filename.lastIndexOf(".") + 1);
      if (ext.isEmpty()) {
        ext = filename;
      }
      List<String> values = mimeTypes.get(ext.toLowerCase());
      mimeType = values == null ? mimeType : values.get(0);
    }
    return mimeType;
  }

  /**
   * Get MIMEType which corresponds to file content. If file content
   * does not allow to determine MIMEtype, the default MIMEType will be returned.
   *
   * @param filename the filename
   * @param is the is
   * @return String MIMEType
   */
  public String getMimeType(String filename, InputStream is) {
    String mimeType = getMimeType(filename);
    if (resolver.getDefaultMimeType().equals(mimeType)) {
      mimeType = resolver.getMimeType(filename, is);
    }
    return mimeType;
  }

  /**
   * Get file extension corresponds to MIMEType. If MIMEType is empty or equals
   * default MIMEType empty string will be returned. If there is no file extension
   * for specific MIMEType the empty string will be returned also. In case when
   * there are more than one extension for specific MIMEType the first occurred
   * extension in the list will be returned if MIMEType ends with this extension
   * otherwise just first occurred.
   * 
   * @param mimeType
   *          MIMEType
   * @return file extension
   */
  public String getExtension(String mimeType) {
    String extension = resolver.getExtension(mimeType);
    if (extension.length() == 0) {
      // use this resolver map (the same logic as in MimeTypeResolver)
      mimeType = mimeType.toLowerCase();

      if (mimeType.isEmpty() || mimeType.equals(resolver.getDefaultMimeType())) {
        return "";
      }

      List<String> values = extentions.get(mimeType);
      if (values == null) {
        return "";
      }

      String resultExt = "";
      for (String ext : values) {
        if (mimeType.endsWith(ext)) {
          return ext;
        }

        if (resultExt.isEmpty()) {
          resultExt = ext;
        }
      }
      extension = resultExt;
    }
    return extension;
  }

  /**
   * Returns default MIMEType.
   * 
   * @return String
   */
  public String getDefaultMimeType() {
    return resolver.getDefaultMimeType();
  }

  /**
   * Set default MIMEType.
   *
   * @param type the new default mime type
   */
  public void setDefaultMimeType(String type) {
    resolver.setDefaultMimeType(type);
  }

  /**
   * Return optional representation (UI) mode parameter (x-type-mode) for given mime type or <code>null</code>
   * if mode cannot be determined.
   * 
   * @param type {@link String} a mime-type string
   * @return {@link String} with UI mode for given mime-type or <code>null</code> if mode cannot be determined
   */
  public String getMimeTypeMode(String type) {
    return getMimeTypeMode(type, null);
  }

  /**
   * Return optional representation (UI) mode parameter (x-type-mode) for given MIME type or/and a file name.
   * If type is <code>null</code> then the type will be defined from given file name first. If mode cannot be
   * determined for given type and file name not <code>null</code>, then an attempt will be tried for a type
   * defined for this name. Method returns <code>null</code> if mode cannot be determined from given
   * parameters.
   * 
   * @param type {@link String} a MIME type string or <code>null</code>
   * @param name {@link String} a file name or <code>null</code>
   * @return {@link String} with UI mode for given MIME type or <code>null</code> if mode cannot be determined
   */
  public String getMimeTypeMode(String type, String name) {
    if (type == null && name != null) {
      type = getMimeType(name);
    }
    if (type != null) {
      try {
        boolean tryResolved;
        do {
          tryResolved = false;
          MimeType mimeType = new MimeType(type);
          String mode = mimeType.getParameter(ExtendedMimeTypeResolver.X_TYPE_MODE);
          if (mode == null || mode.length() == 0) {
            // try in this resolved map
            Set<String> modeList = modes.get(mimeType.getBaseType());
            if (modeList != null && modeList.size() == 1) {
              // if have one-to-one relation - OK, else will try by filename
              return modeList.iterator().next();
            } else if (name != null) {
              // try with a type resolved from given filename
              type = getMimeType(name);
              tryResolved = true;
              name = null; // null to do not repeat this attempt
            } else if (modeList != null && modeList.size() > 0) {
              // worse case: we have several modes for given type and need choose... first one
              return modeList.iterator().next();
            }
          } else {
            return mode;
          }
        } while (type != null && tryResolved);
      } catch (MimeTypeParseException e) {
        LOG.warn("Error parsing mimetype " + type + ": " + e.getMessage());
      }
    }
    return null;
  }

  /**
   * Load MIMEType and corresponding extension.
   *
   * @param aLine the a line
   */
  protected void processLine(String aLine) {
    aLine = aLine.toLowerCase();
    int p = aLine.indexOf("=");

    String ext = aLine.substring(0, p);
    String mimetype = aLine.substring(p + 1);

    try {
      MimeType mimeType = new MimeType(mimetype);
      String mode = mimeType.getParameter(ExtendedMimeTypeResolver.X_TYPE_MODE);
      if (mode != null && mode.length() > 0) {
        String baseType = mimeType.getBaseType();
        Set<String> modeList = modes.get(baseType);
        if (modeList == null) {
          modeList = new LinkedHashSet<String>();
        }
        modeList.add(mode);
        modes.put(baseType, modeList);
      }
    } catch (MimeTypeParseException e) {
      LOG.warn("Error parsing mimetype " + mimetype + ": " + e.getMessage());
    }

    // add mimetype
    List<String> values = mimeTypes.get(ext);
    if (values == null) {
      values = new ArrayList<String>();
      mimeTypes.put(ext, values);
    }
    values.add(mimetype);

    // add extension
    values = extentions.get(mimetype);
    if (values == null) {
      values = new ArrayList<String>();
      extentions.put(mimetype, values);
    }
    values.add(ext);
  }
}
