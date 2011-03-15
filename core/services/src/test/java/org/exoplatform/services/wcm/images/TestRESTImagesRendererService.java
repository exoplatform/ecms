/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.images;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.wcm.BaseWCMTestCase;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Jul 28, 2009
 */
public class TestRESTImagesRendererService extends BaseWCMTestCase {

  /** The rest images renderer service. */
  private RESTImagesRendererService restImagesRendererService;

  /** The Constant WEB_CONTENT_NODE_NAME. */
  private static final String WEB_CONTENT_NODE_NAME = "webContent";

  private Node documentNode;

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    restImagesRendererService = getService(RESTImagesRendererService.class);
    documentNode = (Node) session.getItem("/sites content/live/classic/documents");
  }

  /**
   * Test generate uri_01.
   *
   * When Node input does not exist.
   */
  public void testGenerateURI_01() {
    try {
      restImagesRendererService.generateImageURI(null, null);
      fail();
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  /**
   * Test generate uri_02.
   *
   * When Node input not is the "nt:file".
   */
  public void testGenerateURI_02() {
    try {
      File file = new File("src/test/resources/test.html");
      Node imageNode = this.createdNodeImages(documentNode, file.getName(), getFileImages(file));
      restImagesRendererService.generateImageURI(imageNode.getParent(), null);
      fail();
    } catch (Exception e) {
      if(e instanceof UnsupportedOperationException) {
        assertEquals("The node isn't nt:file", e.getMessage());
      } else {
        fail();
      }
    }
  }

  /**
   * Test generate uri_03.
   *
   * When Node input have jcr:data does not is images file.
   */
  public void testGenerateURI_03() {
    try {
      File file = new File("src/test/resources/test.html");
      Node imageNode = this.createdNodeImages(documentNode,file.getName(), getFileImages(file));
      imageNode.getNode("jcr:content");
      String uri = restImagesRendererService.generateImageURI(imageNode, null);

      String expected = "/" + PortalContainer.getCurrentPortalContainerName() + "/" +
                        PortalContainer.getCurrentRestContextName() +
                        "/images/repository/collaboration/sites content/live/classic/documents/webContent/medias/images/"
                        + file.getName() + "?param=file";
      assertEquals(expected, uri);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test generate uri_04.
   *
   * When Node input have jcr:data is images file.
   */
  public void testGenerateURI_04() {
    try {
      File file = new File("src/test/resources/08_resize.jpg");
      Node imageNode = this.createdNodeImages(documentNode,file.getName(), getFileImages(file));
      imageNode.getNode("jcr:content");
      String uri = restImagesRendererService.generateImageURI(imageNode, null);

      String expected = "/" + PortalContainer.getCurrentPortalContainerName() + "/" +
                        PortalContainer.getCurrentRestContextName() +
                        "/images/repository/collaboration/sites content/live/classic/documents/webContent/medias/images/"
                        + file.getName() + "?param=file";
      assertEquals(expected, uri);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test generate uri_05.
   *
   * When string property name input is null.
   */
  public void testGenerateURI_05() {
    try {
      File file = new File("src/test/resources/08_resize.jpg");
      Node imageNode = this.createdNodeImages(documentNode, file.getName(), getFileImages(file));
      imageNode.getNode("jcr:content");
      String uri = restImagesRendererService.generateImageURI(imageNode, null);

      String expected = "/" + PortalContainer.getCurrentPortalContainerName() + "/" +
                        PortalContainer.getCurrentRestContextName() +
                        "/images/repository/collaboration/sites content/live/classic/documents/webContent/medias/images/"
                        + file.getName() + "?param=file";
      assertEquals(expected, uri);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test generate uri_06.
   *
   * When string property name input is empty.
   */
  public void testGenerateURI_06() {
    try {
      File file = new File("src/test/resources/08_resize.jpg");
      Node imageNode = this.createdNodeImages(documentNode, file.getName(), getFileImages(file));
      imageNode.getNode("jcr:content");
      String uri = restImagesRendererService.generateImageURI(imageNode, "");

      String expected = "/" + PortalContainer.getCurrentPortalContainerName() + "/" +
                        PortalContainer.getCurrentRestContextName() +
                        "/images/repository/collaboration/sites content/live/classic/documents/webContent/medias/images/"
                        + file.getName() + "?param=";
      assertEquals(expected, uri);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Test generate uri_07.
   *
   * When string property name input is "test property name".
   */
  public void testGenerateURI_07() {
    try {
      File file = new File("src/test/resources/08_resize.jpg");
      Node imageNode = this.createdNodeImages(documentNode, file.getName(), getFileImages(file));
      imageNode.getNode("jcr:content");
      String uri = restImagesRendererService.generateImageURI(imageNode, "test property name");
      String expected = "/" + PortalContainer.getCurrentPortalContainerName() + "/" +
                        PortalContainer.getCurrentRestContextName() +
                        "/images/repository/collaboration/sites content/live/classic/documents/webContent/medias/images/"
                        + file.getName() + "?param=test property name";
      assertEquals(expected, uri);
    } catch (Exception e) {
      fail();
    }
  }

  /**
   * Gets the file images.
   *
   * @param file the file
   *
   * @return the file images
   *
   * @throws Exception the exception
   */
  private InputStream getFileImages(File file) throws Exception {
    InputStream fileInput = new FileInputStream(file);
    if (fileInput == null) throw new FileNotFoundException("File not found!!!");
    return fileInput;
  }

  /**
   * Created node images.
   *
   * @param parentNode the parent node
   * @param file the file
   * @param nodeName the node name
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  private Node createdNodeImages(Node parentNode, String nodeName, InputStream file) throws Exception {
    Node webContent = createWebcontentNode(parentNode, WEB_CONTENT_NODE_NAME, null, null, null);
    Node imagesFolder = webContent.getNode("medias").getNode("images");
    Node imagesFile = imagesFolder.addNode(nodeName, "nt:file");
    Node imagesContent;
    try {
      imagesContent = imagesFile.getNode("jcr:content");
    } catch (Exception ex) {
      imagesContent = imagesFile.addNode("jcr:content", "nt:resource");
    }
    imagesContent.setProperty("jcr:encoding", "UTF-8");
    imagesContent.setProperty("jcr:mimeType", "text/images");
    imagesContent.setProperty("jcr:lastModified", new Date().getTime());
    imagesContent.setProperty("jcr:data", file);

    return imagesFile;
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {
    super.tearDown();
    if (documentNode.hasNode(WEB_CONTENT_NODE_NAME))
      documentNode.getNode(WEB_CONTENT_NODE_NAME).remove();
    session.save();
  }
}
