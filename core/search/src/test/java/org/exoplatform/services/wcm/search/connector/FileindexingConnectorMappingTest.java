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
package org.exoplatform.services.wcm.search.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class FileindexingConnectorMappingTest {

  /**
   * The transcript is indexed as text only: a keyword copy of a whole
   * transcript exceeds the Lucene term limit once it passes 32766 bytes, and
   * Elasticsearch then refuses the file's whole document. The title keeps its
   * keyword copy, which the Documents app sorts on. This test reads the mapping
   * the connector sends; it does not run Elasticsearch, so it does not show
   * that the engine accepts the mapping or a long transcript.
   */
  @Test
  public void testTranscriptionIsMappedAsTextWithoutKeywordCopy() {
    JsonNode properties = mapping().get("properties");

    JsonNode transcription = properties.get("transcription");
    assertEquals("text", transcription.get("type").asString());
    assertEquals("offsets", transcription.get("index_options").asString());
    JsonNode transcriptionFields = transcription.get("fields");
    assertFalse(transcriptionFields.has("raw"));
    assertTrue(transcriptionFields.has("whitespace"));
    for (JsonNode field : transcriptionFields.values()) {
      assertEquals("text", field.get("type").asString());
    }

    assertEquals("keyword", properties.get("title").get("fields").get("raw").get("type").asString());
  }

  /**
   * Builds a connector with the minimal parameters and parses its mapping with
   * a strict JSON parser, which refuses what Elasticsearch refuses, such as a
   * trailing comma.
   *
   * @return the mapping as a JSON tree
   */
  private JsonNode mapping() {
    InitParams initParams = new InitParams();
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("index_alias", "file_alias");
    constructorParams.setProperty("index_current", "file_v6");
    initParams.addParameter(constructorParams);
    try (MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      commonsUtils.when(() -> CommonsUtils.getService(any())).thenReturn(null);
      FileindexingConnector connector = new FileindexingConnector(initParams);
      return JsonMapper.builder().build().readTree(connector.getMapping());
    }
  }
}
