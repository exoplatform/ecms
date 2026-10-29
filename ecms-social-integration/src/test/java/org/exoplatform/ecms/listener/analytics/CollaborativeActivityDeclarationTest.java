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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import tools.jackson.databind.ObjectMapper;

/**
 * The collaborative activity declaration analytics reads from this jar: a
 * document created, uploaded in the Documents app, or edited (its content,
 * its properties, a new version) counts; an upload in the old explorer is
 * not declared. The operation names are read from the listeners that emit
 * them; the label keys are matched by hand against this module's
 * Portlets_en.properties.
 */
public class CollaborativeActivityDeclarationTest {

  private static final String DECLARATION_FILE = "analytics-collaborative-activities.json";

  @Test
  @SuppressWarnings("unchecked")
  public void theDeclarationCountsTheDocumentActions() throws IOException {
    Map<String, Object> declaration;
    try (InputStream content = getClass().getClassLoader().getResourceAsStream(DECLARATION_FILE)) {
      assertNotNull("the declaration ships at the root of ecms-social-integration, which lands in /lib", content);
      declaration = new ObjectMapper().readValue(content, Map.class);
    }

    assertEquals(List.of("Drive", "documents"), declaration.get("modules"));
    assertEquals(List.of(JCRNodeListener.FILE_UPDATED_OPERATION,
                         AnalyticsDocumentsListener.UPLOAD_DOCUMENT_NEW_APP_OPERATION_NAME,
                         AnalyticsDocumentsListener.CREATE_DOCUMENT_OPERATION_NAME),
                 declaration.get("operations"));
    assertFalse("an upload in the old explorer is not declared",
                ((List<String>) declaration.get("operations")).contains(AnalyticsDocumentsListener.UPLOAD_DOCUMENT_OLD_APP_OPERATION_NAME));
    assertFalse(declaration.containsKey("legacyOperations"));
    assertFalse(declaration.containsKey("excludedActivityTypes"));
    assertFalse(declaration.containsKey("browserOperations"));

    Map<String, Object> labels = (Map<String, Object>) declaration.get("labels");
    assertEquals("ecms.collaborativeActivity.help.title", labels.get("title"));
    assertEquals(List.of("ecms.collaborativeActivity.help.counted"), labels.get("counted"));
  }

}
