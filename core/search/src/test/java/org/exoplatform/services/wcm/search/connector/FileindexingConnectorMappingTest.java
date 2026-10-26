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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

public class FileindexingConnectorMappingTest {

  /**
   * The transcript is indexed as text only: a keyword copy of a whole
   * transcript exceeds the Lucene term limit once it passes 32766 bytes, and
   * Elasticsearch then refuses the file's whole document. The title keeps its
   * keyword copy, which the Documents app sorts on.
   *
   * @throws Exception when the mapping is not valid JSON
   */
  @Test
  public void testTranscriptionIsMappedAsTextWithoutKeywordCopy() throws Exception {
    JSONObject properties = (JSONObject) mapping().get("properties");

    JSONObject transcription = (JSONObject) properties.get("transcription");
    assertEquals("text", transcription.get("type"));
    assertEquals("offsets", transcription.get("index_options"));
    JSONObject transcriptionFields = (JSONObject) transcription.get("fields");
    assertNull(transcriptionFields.get("raw"));
    assertNotNull(transcriptionFields.get("whitespace"));
    for (Object field : transcriptionFields.values()) {
      assertEquals("text", ((JSONObject) field).get("type"));
    }

    JSONObject titleRaw = (JSONObject) ((JSONObject) ((JSONObject) properties.get("title")).get("fields")).get("raw");
    assertEquals("keyword", titleRaw.get("type"));
  }

  /**
   * Builds a connector with the minimal parameters and parses its mapping.
   *
   * @return the mapping as a JSON object
   * @throws Exception when the mapping is not valid JSON
   */
  private JSONObject mapping() throws Exception {
    InitParams initParams = new InitParams();
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("index_alias", "file_alias");
    constructorParams.setProperty("index_current", "file_v6");
    initParams.addParameter(constructorParams);
    try (MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      commonsUtils.when(() -> CommonsUtils.getService(any())).thenReturn(null);
      FileindexingConnector connector = new FileindexingConnector(initParams);
      return (JSONObject) new JSONParser().parse(connector.getMapping());
    }
  }
}
