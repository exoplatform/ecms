/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp;

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.RenditionContentStream;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.utils.MimeType;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

public class RenditionsUpdateListener implements EventListener
{
   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(RenditionsUpdateListener.class.getName());

   private final String workspace;

   private final Repository repository;

   /**
    * Instantiates a new update listener.
    *
    * @param repository the repository
    * @param workspace the workspace
    */
   public RenditionsUpdateListener(Repository repository, String workspace)
   {
      this.repository = repository;
      this.workspace = workspace;
   }

   /**
    * {@inheritDoc}
    */
   public void onEvent(EventIterator eventIterator)
   {
      SessionProvider sessionProvider = null;
      try
      {
         sessionProvider = SessionProvider.createSystemProvider();
         Session session = sessionProvider.getSession(workspace, (ManageableRepository)repository);
         while (eventIterator.hasNext())
         {
            Event event = eventIterator.nextEvent();
            String path = event.getPath();
            // No processing of renditions for all nodes in "/xcmis:system".
            if (!path.startsWith("/xcmis:system") && path.endsWith("/jcr:data"))
            {
               Property jcrData = null;
               try {
               jcrData = (Property)session.getItem(path);
               } catch (PathNotFoundException ex){
                 // No data;
               }
               if (jcrData == null)
                 return; //No data, nothing to do;
               Node jcrContent = jcrData.getParent();
               Node fileNode = jcrContent.getParent();
               // Do nothing since 'nt:file' without mixin 'cmis:document' may
               // not have renditions.
               if (fileNode.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
               {
                  // Remove all existed renditions since content is changed.
                  for (NodeIterator iter = fileNode.getNodes(); iter.hasNext();)
                  {
                     Node next = iter.nextNode();
                     if (next.isNodeType(JcrCMIS.CMIS_NT_RENDITION))
                     {
                        next.remove();
                     }
                  }
                  // If new content set then create new rendition.
                  long length = jcrData.getLength();
                  if (length > 0)
                  {
                     MimeType mimeType = MimeType.fromString(jcrContent.getProperty(JcrCMIS.JCR_MIMETYPE).getString());
                     if (jcrContent.hasProperty(JcrCMIS.JCR_ENCODING))
                     {
                        mimeType.getParameters().put(CmisConstants.CHARSET,
                           jcrContent.getProperty(JcrCMIS.JCR_ENCODING).getString());
                     }

                     RenditionContentStream renditionContentStream =
                        RenditionManager.getInstance().getStream(
                           new BaseContentStream(jcrData.getStream(), length, null, mimeType), mimeType);
                     if (renditionContentStream != null)
                     {
                        String id = IdGenerator.generate();
                        Node rendition = fileNode.addNode(id, JcrCMIS.CMIS_NT_RENDITION);
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_STREAM, renditionContentStream.getStream());
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_MIME_TYPE, renditionContentStream.getMediaType()
                           .getBaseType());
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_ENCODING, renditionContentStream.getMediaType()
                           .getParameter(CmisConstants.CHARSET));
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_KIND, renditionContentStream.getKind());
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_HEIGHT, renditionContentStream.getHeight());
                        rendition.setProperty(JcrCMIS.CMIS_RENDITION_WIDTH, renditionContentStream.getWidth());
                     }
                  }
                  session.save();
               }
            }
         }
      }
      catch (Exception e)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error("Creating rendition on event failed. " + e.getMessage(), e);
        }
      }
      finally
      {
         if (sessionProvider != null)
         {
            sessionProvider.close();
         }
      }
   }
}
