/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.document.impl.tika;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.Parser;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.io.Reader;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TikaDocumentReaderServiceImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class TikaDocumentReaderServiceImpl extends DocumentReaderServiceImpl implements Startable
{
   /**
    * The name of the value parameter in which we specify the tika configuration
    */
   public static final String TIKA_CONFIG_PATH = "tika-configuration";
   
   /**
    * The name of the value parameter in which we specify the total amount of threads
    * to use to pipe the output of the parsers into a {@link Reader}  
    */
   public static final String CONTENT_EXTRACTOR_POOL_SIZE = "content-extractor-pool-size";

   /**
    * The executor used to pipe the output of the parsers into a {@link Reader}  
    */
   private final ExecutorService contentExtractor; 
   
   /**
    * Tika configuration - configured from tika-conf.xml, otherwise default used.
    */
   private final TikaConfig conf;

   public TikaDocumentReaderServiceImpl(ConfigurationManager configManager, InitParams params) throws Exception
   {
      super(params);

      // get tika configuration
      if (params != null && params.getValueParam(TIKA_CONFIG_PATH) != null)
      {
         final InputStream is = configManager.getInputStream(params.getValueParam(TIKA_CONFIG_PATH).getValue());
         conf = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<TikaConfig>()
         {
            public TikaConfig run() throws Exception
            {
               return new TikaConfig(is);
            }
         });
      }
      else
      {
         conf = TikaConfig.getDefaultConfig();
      }
      contentExtractor = Executors.newCachedThreadPool(new TikaDocumentReaderThreadFactory());
   }

   /**
    * Returns document reader by mimeType. DocumentReaders are registered only by first user call.
    * 
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.document.DocumentReaderService#getDocumentReader
    * (java.lang.String)
    */
   @Override
   public DocumentReader getDocumentReader(String mimeType) throws HandlerNotFoundException
   {
      // first check user defined old-style and previously registered TikaDocumentReaders
      mimeType = mimeType.toLowerCase();
      DocumentReader reader = readers_.get(mimeType);

      if (reader != null)
      {
         return reader;
      }
      else
      {
         // tika-config may contain really big amount of mimetypes, but used only few,
         // so to avoid load in memory many copies of DocumentReader, we will register it
         // only if someone need it
         Parser tikaParser = conf.getParser();

         synchronized (this)
         {
            // Check if the reader has been registered since the thread is blocked
            reader = readers_.get(mimeType);
            if (reader != null)
            {
               return reader;
            }

            reader = new TikaDocumentReader(tikaParser, mimeType, contentExtractor);
            // Initialize the map with the existing values 
            Map<String, DocumentReader> tmpReaders = new HashMap<String, DocumentReader>(readers_);
            // Register new document reader 
            tmpReaders.put(mimeType, reader);
            // Update the map of readers 
            readers_ = tmpReaders;
            return reader;
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {      
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      contentExtractor.shutdown();
   }
   
   private static class TikaDocumentReaderThreadFactory implements ThreadFactory
   {
      static final AtomicInteger poolNumber = new AtomicInteger(1);

      final ThreadGroup group;

      final AtomicInteger threadNumber = new AtomicInteger(1);

      final String namePrefix;

      TikaDocumentReaderThreadFactory()
      {
         SecurityManager s = System.getSecurityManager();
         group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
         namePrefix = "content-extractor-" + poolNumber.getAndIncrement() + "-thread-";
      }

      /**
       * {@inheritDoc}
       */
      public Thread newThread(Runnable r)
      {
         Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
         if (t.isDaemon())
            t.setDaemon(false);
         if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
         return t;
      }
   }
}
