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
package org.exoplatform.ecm.REST.viewer;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 3, 2009  
 * 7:33:30 AM
 */
/**
 * Provide the request which will be used to display PDF file on web browser
 * {repoName} Repository name
 * {workspaceName} Workspace name
 * {nodePath} Node path
 * {pageNumber} Page number
 * {rotation} Page rotation, values are valid: 0.0f, 90.0f, 180.0f, 270.0f
 * {scale} Zoom factor to be applied to the rendered page 
 * Example: <img src="/portal/rest/pdfviewer/repository/collaboration/test.pdf/1/0.0f/1.0f">
 */
@Path("/pdfviewer/{repoName}/{workspaceName}/{pageNumber}/{rotation}/{scale}/{uuid}/")
public class PDFViewerRESTService implements ResourceContainer {

  private static final String LASTMODIFIED = "Last-Modified";
  private RepositoryService repositoryService_;

  public PDFViewerRESTService(RepositoryService repositoryService) throws Exception {
    repositoryService_ = repositoryService;
  }

  @GET
//  @InputTransformer(PassthroughInputTransformer.class)
//  @OutputTransformer(PassthroughOutputTransformer.class)
  public Response getCoverImage(@PathParam("repoName") String repoName, 
      @PathParam("workspaceName") String wsName,
      @PathParam("uuid") String uuid,
      @PathParam("pageNumber") String pageNumber,
      @PathParam("rotation") String rotation,
      @PathParam("scale") String scale) throws Exception {
    return getImageByPageNumber(repoName, wsName, uuid, pageNumber, rotation, scale);
  }

  private Response getImageByPageNumber(String repoName, String wsName, String uuid, 
      String pageNumber, String strRotation, String strScale) throws Exception {
    System.setProperty("org.icepdf.core.awtFontLoading", "true");
    Document document = new Document();
    ManageableRepository repository = repositoryService_.getRepository(repoName);
    Session session = getSystemProvider().getSession(wsName, repository);
    Node currentNode = session.getNodeByUUID(uuid);
    Node contentNode = currentNode.getNode("jcr:content");
    try {
      InputStream input = contentNode.getProperty("jcr:data").getStream() ;      
      document.setInputStream(input, currentNode.getPath());
    } catch (PDFException ex) {
      System.out.println("Error parsing PDF document " + ex);
    } catch (PDFSecurityException ex) {
      System.out.println("Error encryption not supported " + ex);
    } catch (FileNotFoundException ex) {
      System.out.println("Error file not found " + ex);
    } catch (IOException ex) {
      System.out.println("Error handling PDF document " + ex);
    }

    // save page caputres to file.
    float scale = Float.parseFloat(strScale);
    float rotation = Float.parseFloat(strRotation);
    int maximumOfPage = document.getNumberOfPages();
    int pageNum = 1;
    try {
      pageNum = Integer.parseInt(pageNumber);
    } catch(NumberFormatException e) {
      pageNum = 1;
    }
    if(pageNum >= maximumOfPage) pageNum = maximumOfPage;
    else if(pageNum < 1) pageNum = 1;
    // Paint each pages content to an image and write the image to file
    BufferedImage image = (BufferedImage) document.getPageImage(pageNum - 1, GraphicsRenderingHints.SCREEN,
        Page.BOUNDARY_CROPBOX, rotation, scale);
    RenderedImage rendImage = image;

    // capture the page image to file
    File file = null;
    try {
      file = new File("imageCapture1_" + pageNum + ".png");
      ImageIO.write(rendImage, "png", file);
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      String lastModified = contentNode.getProperty("jcr:lastModified").getString();
      session.logout();
      return Response.ok(is, "image").header(LASTMODIFIED, lastModified).build();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      image.flush();
      // clean up resources
      document.dispose();
      if(file != null) file.delete();
      session.logout();
    }
    return Response.ok().build();
  }

  private SessionProvider getSystemProvider() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service = 
      (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    return service.getSystemSessionProvider(null) ;  
  }  
}
