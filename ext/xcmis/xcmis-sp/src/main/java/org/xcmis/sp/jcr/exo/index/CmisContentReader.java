/*
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
package org.xcmis.sp.jcr.exo.index;

import org.xcmis.search.content.ContentEntry;
import org.xcmis.search.content.command.InvocationContext;
import org.xcmis.search.content.command.read.GetChildEntriesCommand;
import org.xcmis.search.content.command.read.GetContentEntryCommand;
import org.xcmis.search.content.command.read.GetUnfiledEntriesCommand;
import org.xcmis.search.content.interceptors.ContentReaderInterceptor;
import org.xcmis.sp.jcr.exo.index.IndexListener.ContentEntryAdapter;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.Storage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class CmisContentReader extends ContentReaderInterceptor
{
   private final Storage storage;

   private final ContentEntryAdapter contentEntryAdapter;

   /**
    * Constructor.
    * @param storage Storage
    */
   public CmisContentReader(Storage storage)
   {
      super();
      this.storage = storage;
      this.contentEntryAdapter = new ContentEntryAdapter();
   }

   /**
    * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitChildEntriesCommand(org.xcmis.search.content.command.InvocationContext, org.xcmis.search.content.command.read.GetChildEntriesCommand)
    */
   @Override
   public Object visitChildEntriesCommand(InvocationContext ctx, GetChildEntriesCommand command) throws Throwable
   {
      List<ContentEntry> childs = new ArrayList<ContentEntry>();
      ObjectData parent = storage.getObjectById(command.getParentUuid());
      if (parent instanceof FolderData)
      {
         ItemsIterator<ObjectData> childDatas = ((FolderData)parent).getChildren(null);
         while (childDatas.hasNext())
         {
            childs.add(contentEntryAdapter.createEntry(childDatas.next()));

         }

      }
      return childs;
   }

   /**
    * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitGetContentEntryCommand(org.xcmis.search.content.command.InvocationContext, org.xcmis.search.content.command.read.GetContentEntryCommand)
    */
   @Override
   public Object visitGetContentEntryCommand(InvocationContext ctx, GetContentEntryCommand command) throws Throwable
   {

      ObjectData entry;
      try
      {
         entry = storage.getObjectById(command.getEntryUuid());
      }
      catch (ObjectNotFoundException e)
      {
         return null;
      }
      return contentEntryAdapter.createEntry(entry);
   }

   /**
    * @see org.xcmis.search.content.interceptors.ContentReaderInterceptor#visitGetUnfilledEntriesCommand(org.xcmis.search.content.command.InvocationContext, org.xcmis.search.content.command.read.GetUnfilledEntriesCommand)
    */
   @Override
   public Object visitGetUnfiledEntriesCommand(InvocationContext ctx, GetUnfiledEntriesCommand command)
      throws Throwable
   {
      return storage.getUnfiledObjectsId();
   }
}
