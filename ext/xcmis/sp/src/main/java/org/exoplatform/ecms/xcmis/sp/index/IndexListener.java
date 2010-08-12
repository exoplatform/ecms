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

package org.exoplatform.ecms.xcmis.sp.index;

import org.apache.commons.lang.Validate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.SearchService;
import org.xcmis.search.content.IndexModificationException;
import org.xcmis.spi.ObjectData;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
@Deprecated
public class IndexListener
{

   private static final Log LOG = ExoLogger.getLogger(IndexListener.class);

   /**
    * Index storage.
    */
   private final SearchService searchService;

   private final ContentEntryAdapter contentEntryAdapter;

   public IndexListener(SearchService searchService)
   {
      Validate.notNull(searchService, "The searchService argument may not be null");
      this.searchService = searchService;
      this.contentEntryAdapter = new ContentEntryAdapter();
   }

   public void created(ObjectData object)
   {
      try
      {
         searchService.update(contentEntryAdapter.createEntry(object), null);
      }
      catch (IndexModificationException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug(e.getLocalizedMessage());
         }
      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug(e.getLocalizedMessage());
         }
      }
   }

   public void removed(Set<String> removed)
   {
      try
      {
         searchService.update(Collections.EMPTY_LIST, removed);
      }
      catch (IndexModificationException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug(e.getLocalizedMessage());
         }
      }
   }

   public void updated(ObjectData object)
   {
      try
      {
         searchService.update(contentEntryAdapter.createEntry(object), object.getObjectId());
      }
      catch (IndexModificationException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug(e.getLocalizedMessage());
         }
      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug(e.getLocalizedMessage());
         }
      }
   }
}
