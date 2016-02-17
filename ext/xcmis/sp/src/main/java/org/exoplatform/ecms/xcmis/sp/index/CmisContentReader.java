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

import org.exoplatform.ecms.xcmis.sp.NotSupportedNodeTypeException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.content.ContentEntry;
import org.xcmis.search.content.command.InvocationContext;
import org.xcmis.search.content.command.read.GetChildEntriesCommand;
import org.xcmis.search.content.command.read.GetContentEntryCommand;
import org.xcmis.search.content.command.read.GetUnfiledEntriesCommand;
import org.xcmis.search.content.interceptors.ContentReaderInterceptor;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.Storage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id$
 *
 */
public class CmisContentReader extends ContentReaderInterceptor
{
   private final Storage storage;

   /** Logger. */
   private static final Log LOG = ExoLogger.getLogger(CmisContentReader.class.getName());

   private final ContentEntryAdapter contentEntryAdapter;

   /**
    * Constructor.
    *
    * @param storage
    *           Storage
    */
   public CmisContentReader(Storage storage)
   {
      super();
      this.storage = storage;
      this.contentEntryAdapter = new ContentEntryAdapter();
   }

  /**
   * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitChildEntriesCommand(
   *      org.xcmis.search.content.command.InvocationContext,
   *      org.xcmis.search.content.command.read.GetChildEntriesCommand)
   */
  @Override
  public Object visitChildEntriesCommand(InvocationContext ctx, GetChildEntriesCommand command) throws Throwable {
    List<ContentEntry> childs = new ArrayList<ContentEntry>();
    ObjectData parent = storage.getObjectById(command.getParentUuid());
    if (parent instanceof FolderData) {
      ItemsIterator<ObjectData> childDatas = ((FolderData) parent).getChildren(null);
      while (childDatas.hasNext()) {
        childs.add(contentEntryAdapter.createEntry(childDatas.next()));

      }

    }
    return childs;
  }

  /**
   * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitGetContentEntryCommand(
   *      org.xcmis.search.content.command.InvocationContext,
   *      org.xcmis.search.content.command.read.GetContentEntryCommand)
   */
   @Override
  public Object visitGetContentEntryCommand(InvocationContext ctx, GetContentEntryCommand command) throws Throwable {

    // TODO delegate exception handling
    ObjectData entry;
    try {
      entry = storage.getObjectById(command.getEntryUuid());
    } catch (ObjectNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(e.getLocalizedMessage(), e);
      }
      return null;
    } catch (NotSupportedNodeTypeException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(e.getLocalizedMessage(), e);
      }
      return null;
    }
    return contentEntryAdapter.createEntry(entry);
  }

  /**
   * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitGetUnfiledEntriesCommand(InvocationContext, GetUnfiledEntriesCommand)
   */
  @Override
  public Object visitGetUnfiledEntriesCommand(InvocationContext ctx,
                                              GetUnfiledEntriesCommand command) throws Throwable {
    return storage.getUnfiledObjectsId();
  }
}
