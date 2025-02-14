/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.documents.DocumentTypeService;

public class DocumentTypeServiceImpl implements DocumentTypeService, Startable {

  private static final String OPEN_DESKTOP_PROVIDER_REGEX           = "^exo.remote-edit\\.([a-z]+)$";

  private static final String OPEN_PROVIDER_RESOURCEBUNDLE_SUFFIX   = ".label";

  private static final String OPEN_PROVIDER_STYLE_SUFFIX            = ".ico";

  private static final String OPEN_DOCUMENT_ON_DESKTOP_ICO          = "uiIconOpenOnDesktop";

  private static final String OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY = "OpenInOfficeConnector.label.exo.remote-edit.desktop";

  private InitParams          params;

  public DocumentTypeServiceImpl(InitParams initParams) {
    this.params = initParams;
  }

  @Override
  public void start() {
    // load desktop application from system property to init-params
    Properties properties = System.getProperties();
    for (String key : properties.stringPropertyNames()) {
      if (key.matches(OPEN_DESKTOP_PROVIDER_REGEX)) {
        List<String> mimetypes = Arrays.asList(properties.getProperty(key) != null ? properties.getProperty(key).split(",") :
                                                                                   null);
        String resourceBundle = properties.getProperty(key + OPEN_PROVIDER_RESOURCEBUNDLE_SUFFIX);
        String ico = properties.getProperty(key + OPEN_PROVIDER_STYLE_SUFFIX);

        if (params.get(key) != null) {
          params.remove(key);
        }
        ObjectParameter objectParameter = new ObjectParameter();
        objectParameter.setName(key);
        objectParameter.setObject(new DocumentType(mimetypes, resourceBundle, ico));
        params.addParam(objectParameter);
      }
    }
  }

  @Override
  public DocumentType getDocumentType(String mimeType) {
    return params.getObjectParamValues(DocumentType.class)
                 .stream()
                 .filter(dt -> dt.getMimeTypes().contains(mimeType))
                 .findFirst()
                 .orElseGet(() -> new DocumentType(Collections.singletonList(mimeType),
                                                   OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY,
                                                   OPEN_DOCUMENT_ON_DESKTOP_ICO));
  }

}
