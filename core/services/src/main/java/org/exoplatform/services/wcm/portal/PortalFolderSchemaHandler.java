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
package org.exoplatform.services.wcm.portal;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.jcr.Node;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;

/**
 * Created by The eXo Platform SAS.
 *
 * @author : Hoa.Pham hoa.pham@exoplatform.com May 28, 2008
 */
public class PortalFolderSchemaHandler extends BaseWebSchemaHandler {
  /**
   * Instantiates a new portal folder schema handler.
   *
   * @param actionServiceContainer the action service container
   */
  public PortalFolderSchemaHandler()  {
  }

  /**
   * Gets the CSS folder.
   *
   * @param portalFolder the portal folder
   * @return the cSS folder
   * @throws Exception the exception
   */
  public Node getCSSFolder(final Node portalFolder) throws Exception {
    return portalFolder.getNode("css");
  }

  /**
   * Gets the javasscript folder.
   *
   * @param portalFolder the portal folder
   * @return the javasscript folder node
   * @throws Exception the exception
   */
  public Node getJSFolder(final Node portalFolder) throws Exception {
    return portalFolder.getNode("js");
  }

  /**
   * Gets the multimedia folder.
   *
   * @param portalFolder the portal folder
   * @return the multimedia folder
   * @throws Exception the exception
   */
  public Node getMultimediaFolder(final Node portalFolder) throws Exception {
    return portalFolder.getNode("medias");
  }

  /**
   * Gets the images folder.
   *
   * @param portalFolder the portal folder
   * @return the images folder
   * @throws Exception the exception
   */
  public Node getImagesFolder(final Node portalFolder) throws Exception {
    return portalFolder.getNode("medias/images");
  }

  /**
   * Gets the video folder.
   *
   * @param portalFolder the portal folder
   * @return the video folder
   * @throws Exception the exception
   */
  public Node getVideoFolder(final Node portalFolder) throws Exception {
    return portalFolder.getNode("medias/videos");
  }

  /**
   * Gets the audio folder.
   *
   * @param portalFolder the portal folder
   * @return the audio folder
   * @throws Exception the exception
   */
  public Node getAudioFolder(final Node portalFolder) throws Exception{
    return portalFolder.getNode("medias/audio");
  }

  /**
   * Gets the document storage.
   *
   * @param portalFolder the portal folder
   * @return the document storage
   * @throws Exception the exception
   */
  public Node getDocumentStorage(Node portalFolder) throws Exception {
    return portalFolder.getNode("documents");
  }

  /**
   * Gets the link folder.
   *
   * @param portalFolder the portal folder
   * @return the link folder
   * @throws Exception the exception
   */
  public Node getLinkFolder(Node portalFolder) throws Exception {
    return portalFolder.getNode("links");
  }

  /**
   * Gets the web content storage.
   *
   * @param portalFolder the portal folder
   * @return the web content storage
   * @throws Exception the exception
   */
  public Node getWebContentStorage (final Node portalFolder) throws Exception {
    return portalFolder.getNode("web contents");
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getHandlerNodeType()
   */
  protected String getHandlerNodeType() { return "exo:portalFolder"; }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#getParentNodeType()
   */
  protected String getParentNodeType() { return "nt:unstructured"; }

  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#process(javax.jcr.Node)
   */
  public void onCreateNode(SessionProvider sessionProvider, final Node portalFolder) throws Exception {
    Calendar calendar = new GregorianCalendar();
    if (!portalFolder.hasNode("js")) {
      Node jsFolder = portalFolder.addNode("js","exo:jsFolder");
      addMixin(jsFolder,"exo:owneable");
      addMixin(jsFolder,"exo:datetime");
      jsFolder.setProperty("exo:dateCreated",calendar);
    }

    if (!portalFolder.hasNode("css")) {
      Node cssFolder = portalFolder.addNode("css","exo:cssFolder");
      addMixin(cssFolder,"exo:owneable");
      addMixin(cssFolder,"exo:datetime");
      cssFolder.setProperty("exo:dateCreated",calendar);
    }

    if (!portalFolder.hasNode("medias")) {
      Node multimedia = portalFolder.addNode("medias","exo:multimediaFolder");
      addMixin(multimedia,"exo:owneable");
      addMixin(multimedia,"exo:datetime");
      multimedia.setProperty("exo:dateCreated",calendar);
      Node images = multimedia.addNode("images",NT_FOLDER);
      addMixin(images, "exo:pictureFolder");
      addMixin(images,"exo:owneable");
      addMixin(images,"exo:datetime");
      images.setProperty("exo:dateCreated",calendar);

      Node video = multimedia.addNode("videos",NT_FOLDER);
      addMixin(video, "exo:videoFolder");
      addMixin(video,"exo:owneable");
      addMixin(video,"exo:datetime");
      video.setProperty("exo:dateCreated",calendar);

      Node audio = multimedia.addNode("audio",NT_FOLDER);
      addMixin(audio, "exo:musicFolder");
      addMixin(audio,"exo:owneable");
      addMixin(audio,"exo:datetime");
      audio.setProperty("exo:dateCreated",calendar);
    }

    if (!portalFolder.hasNode("documents")) {
      Node document = portalFolder.addNode("documents",NT_UNSTRUCTURED);
      addMixin(document, "exo:documentFolder");
      addMixin(document,"exo:owneable");
      addMixin(document,"exo:datetime");
      document.setProperty("exo:dateCreated",calendar);
      document.addMixin("exo:privilegeable");
      ((ExtendedNode)document).setPermission(IdentityConstants.ANY, PermissionType.ALL);
    }

    if (!portalFolder.hasNode("web contents")) {
      Node webContents = portalFolder.addNode("web contents","exo:webFolder");
      addMixin(webContents,"exo:owneable");
      addMixin(webContents,"exo:datetime");
      webContents.setProperty("exo:dateCreated",calendar);

      Node themes = webContents.addNode("site artifacts","exo:themeFolder");
      addMixin(themes,"exo:owneable");
      addMixin(themes,"exo:datetime");
      themes.setProperty("exo:dateCreated",calendar);
    }

    if (!portalFolder.hasNode("links")) {
      Node links = portalFolder.addNode("links", "exo:linkFolder");
      addMixin(links,"exo:owneable");
      addMixin(links,"exo:datetime");
      links.setProperty("exo:dateCreated",calendar);
    }

    if (!portalFolder.hasNode("categories")) {
      Node categoryFolder = portalFolder.addNode("categories", NT_UNSTRUCTURED);
      addMixin(categoryFolder, "exo:owneable");
      addMixin(categoryFolder,"exo:datetime");
      categoryFolder.setProperty("exo:dateCreated", calendar);
    }

    if (!portalFolder.hasNode("ApplicationData")) {
      Node applicationDataFolder = portalFolder.addNode("ApplicationData", NT_UNSTRUCTURED);
      addMixin(applicationDataFolder, "exo:owneable");
      addMixin(applicationDataFolder,"exo:datetime");
      addMixin(applicationDataFolder, "exo:hiddenable");
      applicationDataFolder.setProperty("exo:dateCreated", calendar);

      Node newsletterApplicationFolder = applicationDataFolder.addNode("NewsletterApplication", NT_UNSTRUCTURED);
      addMixin(newsletterApplicationFolder, "exo:owneable");
      addMixin(newsletterApplicationFolder,"exo:datetime");
      newsletterApplicationFolder.setProperty("exo:dateCreated", calendar);

      Node defaultTemplatesFolder = newsletterApplicationFolder.addNode("DefaultTemplates", NT_UNSTRUCTURED);
      addMixin(defaultTemplatesFolder, "exo:owneable");
      addMixin(defaultTemplatesFolder,"exo:datetime");
      defaultTemplatesFolder.setProperty("exo:dateCreated", calendar);

      Node newsletterCategoriesFolder = newsletterApplicationFolder.addNode("Categories", NT_UNSTRUCTURED);
      addMixin(newsletterCategoriesFolder, "exo:owneable");
      addMixin(newsletterCategoriesFolder,"exo:datetime");
      newsletterCategoriesFolder.setProperty("exo:dateCreated", calendar);

      Node newsletterUserFolder = newsletterApplicationFolder.addNode("Users", NT_UNSTRUCTURED);
      addMixin(newsletterUserFolder, "exo:owneable");
      addMixin(newsletterUserFolder,"exo:datetime");
      newsletterUserFolder.setProperty("exo:dateCreated", calendar);
    }

    portalFolder.getSession().save();
  }
}
