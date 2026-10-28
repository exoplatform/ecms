/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.listener.analytics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.exoplatform.services.cms.documents.impl.DocumentServiceImpl;

/**
 * Each event the documents analytics listener is registered on is recorded
 * under its own operation: a document created from a template is a creation,
 * not an upload in the old explorer.
 */
public class AnalyticsDocumentsListenerTest {

  @Test
  public void eachEventIsRecordedUnderItsOwnOperation() {
    assertEquals(AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf(DocumentServiceImpl.CREATE_DOC_NEW_APP));
    assertEquals(AnalyticsDocumentsListener.UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf("exo.upload.doc.newApp"));
    assertEquals(AnalyticsDocumentsListener.UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME,
                 AnalyticsDocumentsListener.operationOf("exo.upload.doc.oldApp"));
    assertEquals("documentCreated", AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME);
  }

}
