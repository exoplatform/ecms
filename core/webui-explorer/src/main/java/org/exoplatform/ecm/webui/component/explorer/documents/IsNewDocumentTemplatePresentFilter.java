/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.webui.ext.filter.UIExtensionAbstractFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilterType;


/**
 * The filter checks if at least one NewDocumentTemplateProvider is registered.
 */
public class IsNewDocumentTemplatePresentFilter extends UIExtensionAbstractFilter {

  /**
   * Instantiates a new checks if is template plugin present filter.
   */
  public IsNewDocumentTemplatePresentFilter() {
    this(null);
  }

  /**
   * Instantiates a new checks if is template plugin present filter.
   *
   * @param messageKey the message key
   */
  public IsNewDocumentTemplatePresentFilter(String messageKey) {
    super(messageKey, UIExtensionFilterType.MANDATORY);
  }

  /**
   * Accept.
   *
   * @param context the context
   * @return true, if successful
   * @throws Exception the exception
   */
  @Override
  public boolean accept(Map<String, Object> context) throws Exception {
    DocumentService documentService = ExoContainerContext.getCurrentContainer()
                                                            .getComponentInstanceOfType(DocumentService.class);

    if (documentService != null) {
      return documentService.getNewDocumentTemplateProviders().size() > 0;
    }
    return false;
  }

  /**
   * On deny.
   *
   * @param context the context
   * @throws Exception the exception
   */
  @Override
  public void onDeny(Map<String, Object> context) throws Exception {
  }

}
