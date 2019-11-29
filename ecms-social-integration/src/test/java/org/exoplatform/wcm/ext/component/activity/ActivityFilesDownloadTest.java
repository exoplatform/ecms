package org.exoplatform.wcm.ext.component.activity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeLocation.class)
public class ActivityFilesDownloadTest {

  @Test
  public void downloadMultipleJCRFiles() throws Exception {
    NodeLocation nodeLocation1 = new NodeLocation("repository", "collaboration", "/test1.txt");
    NodeLocation nodeLocation2 = new NodeLocation("repository", "collaboration", "/test2");
    NodeLocation nodeLocation3 = new NodeLocation("repository", "collaboration", "/test3.txt");

    Node node1 = mock(Node.class);
    Node node2 = mock(Node.class);
    Node node3 = mock(Node.class);

    Node contentNode1 = mock(Node.class);
    Node contentNode3 = mock(Node.class);
    when(node1.getNode(NodetypeConstant.JCR_CONTENT)).thenReturn(contentNode1);
    when(node3.getNode(NodetypeConstant.JCR_CONTENT)).thenReturn(contentNode3);

    Property dataProperty1 = mock(Property.class);
    Property dataProperty3 = mock(Property.class);
    when(contentNode1.getProperty(NodetypeConstant.JCR_DATA)).thenReturn(dataProperty1);
    when(contentNode3.getProperty(NodetypeConstant.JCR_DATA)).thenReturn(dataProperty3);
    when(dataProperty1.getStream()).thenReturn(new ByteArrayInputStream("This is simple text".getBytes()));
    when(dataProperty3.getStream()).thenReturn(new ByteArrayInputStream("This is simple text".getBytes()));

    PowerMockito.mockStatic(NodeLocation.class);
    PowerMockito.when(NodeLocation.getNodeByLocation(Matchers.refEq(nodeLocation1))).thenReturn(node1);
    PowerMockito.when(NodeLocation.getNodeByLocation(Matchers.refEq(nodeLocation2))).thenReturn(node2);
    PowerMockito.when(NodeLocation.getNodeByLocation(Matchers.refEq(nodeLocation3))).thenReturn(node3);

    when(node1.isNodeType(NodetypeConstant.NT_FILE)).thenReturn(true);
    when(node2.isNodeType(NodetypeConstant.NT_FILE)).thenReturn(false);
    when(node3.isNodeType(NodetypeConstant.NT_FILE)).thenReturn(true);

    when(node1.hasNode(NodetypeConstant.JCR_CONTENT)).thenReturn(true);
    when(node2.hasNode(NodetypeConstant.JCR_CONTENT)).thenReturn(false);
    when(node3.hasNode(NodetypeConstant.JCR_CONTENT)).thenReturn(true);

    when(contentNode1.hasProperty(NodetypeConstant.JCR_DATA)).thenReturn(true);
    when(contentNode3.hasProperty(NodetypeConstant.JCR_DATA)).thenReturn(true);

    when(node1.hasProperty(NodetypeConstant.EXO_NAME)).thenReturn(false);
    when(node3.hasProperty(NodetypeConstant.EXO_NAME)).thenReturn(false);

    when(node1.getName()).thenReturn("test1.txt");
    when(node3.getName()).thenReturn("test3.txt");

    InputStream zis = new ActivityFilesDownloadResource(new NodeLocation[]{nodeLocation1, nodeLocation2, nodeLocation3}).getInputStream();
    assertNotNull("Returned input stream is null", zis);
    assertTrue("Empty input stream", zis.available() > 0);

    File tempfile = File.createTempFile("test", "zip");
    try (FileOutputStream fileOutputStream = new FileOutputStream(tempfile)) {
      IOUtils.copy(zis, fileOutputStream);
    }
    IOUtils.closeQuietly(zis);

    try (ZipFile zipFile = new ZipFile(tempfile)) {
      assertEquals(2, zipFile.size());

      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = (ZipEntry) entries.nextElement();
        assertTrue(zipEntry.getName().equals("test1.txt") || zipEntry.getName().equals("test3.txt"));
      }
    }
  }
}
