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
package org.exoplatform.clouddrive.ecms.filters;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

/**
 * Filter files by MIME type including wildcard types. <br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FileTypeFilter.java 00000 Nov 24, 2014 pnedonosko $
 * 
 */
public class FileTypeFilter implements UIExtensionFilter {

  protected static final Log LOG = ExoLogger.getLogger(FileTypeFilter.class);

  protected Set<String>      mimeTypes;

  protected List<MimeType>   accepted;

  @Deprecated
  Collection<MimeType> mimeTypes() {
    if (accepted == null) {
      synchronized (this) {
        if (accepted == null) {
          accepted = new ArrayList<MimeType>();
          for (String m : mimeTypes) {
            try {
              accepted.add(new MimeType(m));
            } catch (MimeTypeParseException e) {
              // skip this accepted
              LOG.warn("Error parsing configured MIME type " + m + ": " + e);
            }
          }
        }
      }
    }
    return accepted;
  }

  public boolean accept(Map<String, Object> context) throws Exception {
    if (mimeTypes == null || mimeTypes.isEmpty()) {
      return true;
    }

    // try quick check first
    String type = context.get("mimeType").toString();
    if (mimeTypes.contains(type)) {
      return true;
    }

    // try wildcard (rely on MimeType.match() impl)
    // try {
    // MimeType mimeType = new MimeType(type);
    // for (MimeType accepted : mimeTypes()) {
    // if (accepted.match(mimeType)) {
    // return true;
    // }
    // }
    // } catch (MimeTypeParseException e) {
    // // unfortunately - no
    // }

    // try wildcard (type starts with accepted)
    for (String accepted : mimeTypes) {
      if (type.startsWith(accepted)) {
        return true;
      }
    }

    return false;
  }

  public UIExtensionFilterType getType() {
    return UIExtensionFilterType.MANDATORY;
  }

  public void onDeny(Map<String, Object> context) throws Exception {
  }
}
