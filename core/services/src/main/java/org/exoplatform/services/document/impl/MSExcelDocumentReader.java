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
package org.exoplatform.services.document.impl;

import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Stream based MS Excel Document Reader with low memory and cpu needs.
 */
public class MSExcelDocumentReader extends BaseDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.MSExcelDocumentReader");

   private static final int MAX_CELL = 5000;

   /**
    * Get the application/excel mime type.
    * 
    * @return The string with application/excel mime type.
    */
   public String[] getMimeTypes()
   {
      return new String[]{"application/excel", "application/xls", "application/vnd.ms-excel"};
   }

   /**
    * Returns only a text from .xls file content with the following rules:
    * <br>
    * we only index :
    * <ul>
    * <li>a maximum of 5000 cells</li>
    * <li>after 5000 cells processed, we abort the parsing</li>
    * </ul>
    * <br>
    * we KEEP only the following data :
    * <ul>
    * <li> tab name {@link org.apache.poi.hssf.record.BoundSheetRecord}</li>
    * <li> cells with string with a {@literal length > 2 chars} (Strings which are not the result of a formula) ({@link org.apache.poi.hssf.record.LabelSSTRecord}}</li>
    * </ul>
    * we SKIP the following data :
    * <ul>
    * <li> cells with number (date formatted or simple number) ({@link org.apache.poi.hssf.record.NumberRecord}}</li>
    * <li> cells with blank value ({@link org.apache.poi.hssf.record.BlankRecord}}</li>
    * <li> cells with boolean or error value ({@link org.apache.poi.hssf.record.BoolErrRecord}}</li>
    * <li> cells with formula ({@link org.apache.poi.hssf.record.FormulaRecord}}</li>
    * </ul>
    *
    * @param is an input stream with .xls file content.
    * @return The string only with text from file content.
    */
   public String getContentAsText(InputStream is) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }

      final StringBuilder builder = new StringBuilder("");

      try
      {
         if (is.available() == 0)
         {
            return "";
         }
         // create a new org.apache.poi.poifs.filesystem.Filesystem
         POIFSFileSystem poifs = new POIFSFileSystem(is);
         InputStream din = null;
         try
         {
            // get the Workbook (excel part) stream in a InputStream
            din = poifs.createDocumentInputStream("Workbook");
            // construct out HSSFRequest object
            HSSFRequest req = new HSSFRequest();
            req.addListenerForAllRecords(new XLSHSSFListener(builder));
            // create our event factory
            HSSFEventFactory factory = new HSSFEventFactory();
            // process our events based on the document input stream
            factory.processEvents(req, din);
         }
         finally
         {
            // and close our document input stream (don't want to leak these!)
            if (din != null)
            {
               try
               {
                  din.close();
               }
               catch (IOException e)
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
            }
         }
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch (IOException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
      }
      return builder.toString();
   }

   public String getContentAsText(InputStream is, String encoding) throws IOException, DocumentReadException
   {
      // Ignore encoding
      return getContentAsText(is);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getProperties(java.io.
    *      InputStream)
    */
   public Properties getProperties(InputStream is) throws IOException, DocumentReadException
   {
      POIPropertiesReader reader = new POIPropertiesReader();
      reader.readDCProperties(is);
      return reader.getProperties();
   }

   class XLSHSSFListener extends AbortableHSSFListener
   {
      private StringBuilder builder;

      public int cellnum = 0;

      // SSTRecords store a array of unique strings used in Excel.
      private SSTRecord sstrec;

      XLSHSSFListener(StringBuilder builder)
      {
         this.builder = builder;
      }

      @Override
      public short abortableProcessRecord(Record record)
      {
         if (cellnum < MAX_CELL)
         {
            switch (record.getSid())
            {
               // SKIP cells containing Numbers (Contains a numeric cell value.)
               case NumberRecord.sid:
                  // NumberRecord numrec = (NumberRecord) record;
                  cellnum++;
                  break;
               // SKIP blank cells
               case BlankRecord.sid:
                  // BlankRecord blankrec = (BlankRecord) record;
                  break;
               // SKIP formula cells
               case FormulaRecord.sid:
                  // FormulaRecord formrec = (FormulaRecord) record;
                  cellnum++;
                  break;
               // SKIP Boolean or Error cells
               case BoolErrRecord.sid:
                  // BoolErrRecord boolrec = (BoolErrRecord) record;
                  cellnum++;
                  break;
               // SSTRecords store a array of unique strings used in Excel.
               case SSTRecord.sid:
                  sstrec = (SSTRecord) record;
                  break;
               case LabelSSTRecord.sid:
                  LabelSSTRecord lrec = (LabelSSTRecord) record;
                  UnicodeString lrecValue = sstrec.getString(lrec.getSSTIndex());
                  if (lrecValue.getCharCount() > 2)
                  {
                     builder.append(lrecValue).append(" ");
                  }
                  cellnum++;
                  break;
               case StringRecord.sid:
                  // StringRecord sr = (StringRecord) record;
                  cellnum++;
                  break;
               case BoundSheetRecord.sid:
                  BoundSheetRecord bsr = (BoundSheetRecord) record;
                  builder.append(bsr.getSheetname()).append(" ");
                  break;
            }
            // continue to process cells
            return 0;
         }
         else
         {
            // stop cells processing
            return -1;
         }
      }
   }
}
