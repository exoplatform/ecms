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
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.BaseWebSchemaHandler;
import org.exoplatform.services.wcm.core.NodetypeConstant;

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
    return portalFolder.hasNode("css") ? portalFolder.getNode("css") : null;
  }

  /**
   * Gets the javasscript folder.
   *
   * @param portalFolder the portal folder
   * @return the javasscript folder node
   * @throws Exception the exception
   */
  public Node getJSFolder(final Node portalFolder) throws Exception {
    return portalFolder.hasNode("js") ? portalFolder.getNode("js") : null;
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

  protected void updateNode(Node node) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    addMixin(node, NodetypeConstant.EXO_OWNEABLE); 
    addMixin(node, NodetypeConstant.EXO_DATETIME); 
    addMixin(node, NodetypeConstant.EXO_MODIFY);
    addMixin(node, NodetypeConstant.EXO_SORTABLE);
    
    node.setProperty(NodetypeConstant.EXO_DATE_CREATED, new GregorianCalendar());
    node.setProperty(NodetypeConstant.EXO_LAST_MODIFIED_DATE, new GregorianCalendar());
    
    ConversationState conversationState = ConversationState.getCurrent();
    String userName = (conversationState == null) ? node.getSession().getUserID() :
                                                    conversationState.getIdentity().getUserId();
    node.setProperty(NodetypeConstant.EXO_LAST_MODIFIER, userName);
    
    node.setProperty(NodetypeConstant.EXO_NAME, node.getName());    
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.core.BaseWebSchemaHandler#process(javax.jcr.Node)
   */
  public void onCreateNode(SessionProvider sessionProvider, final Node portalFolder) throws Exception {
    Calendar calendar = new GregorianCalendar();
    if (!portalFolder.hasNode("js")) {
      Node jsFolder = portalFolder.addNode("js","exo:jsFolder");
      updateNode(jsFolder);
    }

    if (!portalFolder.hasNode("css")) {
      Node cssFolder = portalFolder.addNode("css","exo:cssFolder");
      updateNode(cssFolder);
    }

    if (!portalFolder.hasNode("medias")) {
      Node multimedia = portalFolder.addNode("medias","exo:multimediaFolder");
      updateNode(multimedia);
      
      Node images = multimedia.addNode("images",NT_FOLDER);
      addMixin(images, "exo:pictureFolder");
      updateNode(images);

      Node video = multimedia.addNode("videos",NT_FOLDER);
      addMixin(video, "exo:videoFolder");
      updateNode(video);
      
      Node audio = multimedia.addNode("audio",NT_FOLDER);
      addMixin(audio, "exo:musicFolder");
      updateNode(audio);
    }

    if (!portalFolder.hasNode("documents")) {
      Node document = portalFolder.addNode("documents",NT_UNSTRUCTURED);
      addMixin(document, "exo:documentFolder");
      
      updateNode(document);
      
      document.addMixin("exo:privilegeable");
      ((ExtendedNode)document).setPermission(IdentityConstants.ANY, PermissionType.ALL);
    }

    if (!portalFolder.hasNode("web contents")) {
      Node webContents = portalFolder.addNode("web contents","exo:webFolder");
      updateNode(webContents);

      Node themes = webContents.addNode("site artifacts","exo:themeFolder");
      updateNode(themes);
    }

    if (!portalFolder.hasNode("links")) {
      Node links = portalFolder.addNode("links", "exo:linkFolder");
      updateNode(links);
    }

    if (!portalFolder.hasNode("categories")) {
      Node categoryFolder = portalFolder.addNode("categories", NT_UNSTRUCTURED);
      updateNode(categoryFolder);
    }

    if (!portalFolder.hasNode("ApplicationData")) {
      Node applicationDataFolder = portalFolder.addNode("ApplicationData", NT_UNSTRUCTURED);
      updateNode(applicationDataFolder);      
      addMixin(applicationDataFolder, "exo:hiddenable");

      Node newsletterApplicationFolder = applicationDataFolder.addNode("NewsletterApplication", NT_UNSTRUCTURED);
      updateNode(newsletterApplicationFolder);

      Node defaultTemplatesFolder = newsletterApplicationFolder.addNode("DefaultTemplates", NT_UNSTRUCTURED);
      updateNode(defaultTemplatesFolder);

      Node newsletterCategoriesFolder = newsletterApplicationFolder.addNode("Categories", NT_UNSTRUCTURED);
      updateNode(newsletterCategoriesFolder);

      Node newsletterUserFolder = newsletterApplicationFolder.addNode("Users", NT_UNSTRUCTURED);
      updateNode(newsletterUserFolder);
    }

    portalFolder.getSession().save();
  }
}
