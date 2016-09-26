package org.exoplatform.ecm.webui;

import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.webui.form.UIFormStringInput;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by exo on 9/22/16.
 */
public class TestDialogFormUtil {

  @Test
  public void testPrepareMap() throws Exception {
    // Given
    List inputs = new ArrayList<UIFormStringInput>();
    Map properties = new HashMap<String, String>();
    UIFormStringInput property1 = new UIFormStringInput("name", "name");
    UIFormStringInput property2 = new UIFormStringInput("title", "title");
    UIDialogForm parent = new UIDialogForm();
    JcrInputProperty jcrExoName = new JcrInputProperty();
    JcrInputProperty jcrExoTitle = new JcrInputProperty();
    inputs.add(property1);
    inputs.add(property2);
    property1.setParent(parent);
    property2.setParent(parent);
    jcrExoName.setJcrPath("/node/exo:name");
    jcrExoTitle.setJcrPath("/node/exo:title");
    properties.put("name", jcrExoName);
    properties.put("title", jcrExoTitle);

    // When
    Map<String, JcrInputProperty> map = DialogFormUtil.prepareMap(inputs, properties, null);

    // Then
    assertNotNull(map);
    assertEquals(jcrExoName.getValue(), "name");
    assertEquals(jcrExoTitle.getValue(), "title");
  }

  @Test
  public void testPrepareMapNameOnly() throws Exception {
    // Given
    List inputs = new ArrayList<UIFormStringInput>();
    Map properties = new HashMap<String, String>();
    UIFormStringInput property1 = new UIFormStringInput("name", "name");
    UIDialogForm parent = new UIDialogForm();
    JcrInputProperty jcrExoName = new JcrInputProperty();
    inputs.add(property1);
    property1.setParent(parent);
    jcrExoName.setJcrPath("/node/exo:name");
    properties.put("name", jcrExoName);

    // When
    Map<String, JcrInputProperty> map = DialogFormUtil.prepareMap(inputs, properties, null);

    // Then
    String jcrTitle = (String) map.get("/node/exo:title").getValue();
    assertNotNull(map);
    assertTrue(map.size() == 2);
    assertEquals(jcrExoName.getValue(), "name");
    assertEquals(jcrTitle, "name");
  }

}
