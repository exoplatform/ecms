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

import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class handles the processing of a sheet#.xml
 * sheet part of a XSSF .xlsx file, and generates
 * row and cell events for it.
 * <br>
 * we KEEP only the following data :
 * - cells with number (date formatted or simple number)
 * - cells with string
 * <br>
 * we SKIP the following data :
 * - cells with blank value
 * - cells with boolean value
 * - cells with formula
 * - cells with error
 */
public class MSXExcelSheetXMLHandler extends DefaultHandler
{

   private static final Log LOG = ExoLogger.getExoLogger("exo.core.component.document.MSXExcelSheetXMLHandler");

   /**
    * These are the different kinds of cells we support.
    * We keep track of the current one between
    * the start and end.
    */
   enum xssfDataType
   {
      BOOLEAN,
      ERROR,
      FORMULA,
      INLINE_STRING,
      SST_STRING,
      NUMBER,
   }

   private ReadOnlySharedStringsTable sharedStringsTable;

   /**
    * The maximum number of cells to parse in the Sheet (-1 mean All cells in the sheet)
    */
   private long maxCellsToParse = -1;
   private long currentCellsParsed = 0;

   /**
    * Where our text is going
    */
   private final SheetContentsHandler output;

   // Set when V start element is seen
   private boolean vIsOpen;
   // Set when an Inline String "is" is seen
   private boolean isIsOpen;
   // Set when a header/footer element is seen
   private boolean hfIsOpen;

   // Set when cell start element is seen;
   // used when cell close element is seen.
   private xssfDataType nextDataType;

   // Used to format numeric cell values.
   private String cellRef;

   // Gathers characters as they are seen.
   private StringBuilder value = new StringBuilder();
   private StringBuilder headerFooter = new StringBuilder();

   /**
    * Accepts objects needed while parsing.
    *
    * @param strings Table of shared strings
    */
   public MSXExcelSheetXMLHandler(
       ReadOnlySharedStringsTable strings,
       SheetContentsHandler sheetContentsHandler,
       long maxCellsToParse)
   {
      this.sharedStringsTable = strings;
      this.output = sheetContentsHandler;
      this.nextDataType = xssfDataType.NUMBER;
      this.maxCellsToParse = maxCellsToParse;
   }

   private boolean isTextTag(String name)
   {
      if ("v".equals(name))
      {
         // Easy, normal v text tag
         return true;
      }
      if ("inlineStr".equals(name))
      {
         // Easy inline string
         return true;
      }
      if ("t".equals(name) && isIsOpen)
      {
         // Inline string <is><t>...</t></is> pair
         return true;
      }
      // It isn't a text tag
      return false;
   }

   public void startElement(String uri, String localName, String name,
                            Attributes attributes) throws SAXException
   {

      if (isTextTag(name))
      {
         vIsOpen = true;
         // Clear contents cache
         value.setLength(0);
      }
      else if ("is".equals(name))
      {
         // Inline string outer tag
         isIsOpen = true;
      }
      else if ("f".equals(name))
      {
         // Mark us as being a formula if not already
         if (nextDataType == xssfDataType.NUMBER)
         {
            nextDataType = xssfDataType.FORMULA;
         }
      }
      else if ("oddHeader".equals(name) || "evenHeader".equals(name) ||
          "firstHeader".equals(name) || "firstFooter".equals(name) ||
          "oddFooter".equals(name) || "evenFooter".equals(name))
      {
         hfIsOpen = true;
         // Clear contents cache
         headerFooter.setLength(0);
      }
      else if ("row".equals(name))
      {
//      int rowNum = Integer.parseInt(attributes.getValue("r")) - 1;
         output.startRow(0);
      }
      // c => cell
      else if ("c".equals(name))
      {
         // Set up defaults.
         this.nextDataType = xssfDataType.NUMBER;
         cellRef = attributes.getValue("r");
         String cellType = attributes.getValue("t");
         if ("b".equals(cellType))
            nextDataType = xssfDataType.BOOLEAN;
         else if ("e".equals(cellType))
            nextDataType = xssfDataType.ERROR;
         else if ("inlineStr".equals(cellType))
            nextDataType = xssfDataType.INLINE_STRING;
         else if ("s".equals(cellType))
            nextDataType = xssfDataType.SST_STRING;
         else if ("str".equals(cellType))
            nextDataType = xssfDataType.FORMULA;
      }
   }

   public void endElement(String uri, String localName, String name)
       throws SAXException
   {
      String thisStr = null;

      // v => contents of a cell
      if (isTextTag(name))
      {
         vIsOpen = false;

         // Process the value contents as required, now we have it all
         switch (nextDataType)
         {
            case BOOLEAN:
               currentCellsParsed++;
               break;
            case ERROR:
               currentCellsParsed++;
               break;
            case FORMULA:
               currentCellsParsed++;
               break;
            case INLINE_STRING:
               // TODO: Can these ever have formatting on them?
               XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
               thisStr = rtsi.toString();
               currentCellsParsed++;
               break;
            case SST_STRING:
               String sstIndex = value.toString();
               try
               {
                  int idx = Integer.parseInt(sstIndex);
                  RichTextString rtss = sharedStringsTable.getItemAt(idx);
                  thisStr = rtss.toString();
               }
               catch (NumberFormatException ex)
               {
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("Failed to parse SST index '" + sstIndex + "': " + ex.toString());
                  }
               }
               currentCellsParsed++;
               break;
            case NUMBER:
               currentCellsParsed++;
               break;
            default:
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("Failed to define the unexpected type '" + nextDataType + "'");
               }
               currentCellsParsed++;
               break;
         }
         // Output
         output.cell(cellRef, thisStr);
      }
      else if ("is".equals(name))
      {
         isIsOpen = false;
      }
      else if ("row".equals(name))
      {
         output.endRow();
      }
      else if ("oddHeader".equals(name) || "evenHeader".equals(name) ||
          "firstHeader".equals(name))
      {
         hfIsOpen = false;
         output.headerFooter(headerFooter.toString(), true, name);
      }
      else if ("oddFooter".equals(name) || "evenFooter".equals(name) ||
          "firstFooter".equals(name))
      {
         hfIsOpen = false;
      }
      if (maxCellsToParse >= 0 && currentCellsParsed > maxCellsToParse)
      {
         throw new StopSheetParsingException("Maximum number of cells to parse per sheet reached (max=" + maxCellsToParse + ")");
      }
   }

   /**
    * Captures characters only if a suitable element is open.
    * Originally was just "v"; extended for inlineStr also.
    */
   public void characters(char[] ch, int start, int length)
       throws SAXException
   {
      if (vIsOpen)
      {
         value.append(ch, start, length);
      }
      if (hfIsOpen)
      {
         headerFooter.append(ch, start, length);
      }
   }

   /**
    * You need to implement this to handle the results
    * of the sheet parsing.
    */
   public interface SheetContentsHandler
   {
      /**
       * A row with the (zero based) row number has started
       */
      public void startRow(int rowNum);

      /**
       * A row with the (zero based) row number has ended
       */
      public void endRow();

      /**
       * A cell, with the given formatted value, was encountered
       */
      public void cell(String cellReference, String formattedValue);

      /**
       * A header or footer has been encountered
       */
      public void headerFooter(String text, boolean isHeader, String tagName);
   }

   /**
    * This exception is used to ask the underlying SAX Parser to stop parsing a XML sheet.
    */
   public class StopSheetParsingException extends SAXException
   {

      public StopSheetParsingException(String message)
      {
         super(message);
      }

   }

}
