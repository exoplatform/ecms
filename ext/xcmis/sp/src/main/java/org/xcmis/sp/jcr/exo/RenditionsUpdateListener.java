/**
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xcmis.sp.jcr.exo;

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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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

   private final RenditionManager renditionManager;

   private final String workspace;

   private final Repository repository;

   /**
    * Instantiates a new update listener.
    * 
    * @param repository the repository
    * @param workspace the workspace
    * @param renditionProviders the rendition providers
    */
   public RenditionsUpdateListener(Repository repository, String workspace, RenditionManager renditionManager)
   {
      this.repository = repository;
      this.workspace = workspace;
      this.renditionManager = renditionManager;
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
            if (path.contains("xcmis:system"))
            {
               return;
            }

            if (event.getPath().endsWith("jcr:content") || event.getPath().endsWith("jcr:data"))
            {
               Node node = null;
               Item item = session.getItem(path);
               if (item.isNode())
               {
                  node = session.getItem(path).getParent();
               }
               else
               {
                  node = session.getItem(path).getParent().getParent();
               }
               Node contentNode = node.getNode(JcrCMIS.JCR_CONTENT);
               Property fileContent = contentNode.getProperty(JcrCMIS.JCR_DATA);
               int length = fileContent.getStream().available();
               if (length == 0)
               {
                  {
                     NodeIterator iter = node.getNodes();
                     while (iter.hasNext())
                     {
                        Node tmp = iter.nextNode();
                        if (tmp.isNodeType(JcrCMIS.CMIS_NT_RENDITION))
                        {
                           tmp.remove();
                           node.save();
                        }
                     }
                  }
               }

               else
               {
                  MimeType mimeType = MimeType.fromString(contentNode.getProperty(JcrCMIS.JCR_MIMETYPE).getString());
                  if (contentNode.hasProperty(JcrCMIS.JCR_ENCODING))
                  {
                     mimeType.getParameters().put(CmisConstants.CHARSET,
                        contentNode.getProperty(JcrCMIS.JCR_ENCODING).getString());
                  }

                  RenditionContentStream renditionContentStream =
                     (RenditionContentStream)renditionManager.getStream(new BaseContentStream(fileContent.getStream(),
                        length, null, mimeType), mimeType);
                  if (renditionContentStream != null)
                  {
                     String id = IdGenerator.generate();
                     Node rendition = node.addNode(id, JcrCMIS.CMIS_NT_RENDITION);
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_STREAM, renditionContentStream.getStream());
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_MIME_TYPE, renditionContentStream.getMediaType()
                        .getBaseType());
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_ENCODING, renditionContentStream.getMediaType()
                        .getParameter(CmisConstants.CHARSET));
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_KIND, renditionContentStream.getKind());
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_HEIGHT, renditionContentStream.getHeight());
                     rendition.setProperty(JcrCMIS.CMIS_RENDITION_WIDTH, renditionContentStream.getWidth());
                     session.save();
                  }
               }
            }
         }
      }
      catch (Exception e)
      {
         LOG.error("Creating rendition on event failed. ", e);
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
