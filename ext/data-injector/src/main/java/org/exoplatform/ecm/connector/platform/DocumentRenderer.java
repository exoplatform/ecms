package org.exoplatform.ecm.connector.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import org.apache.pdfbox.PDFToImage;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;


public class DocumentRenderer {

  public boolean createDocument(String content, String fileName, String fileExtension) {
    try {
      if(fileExtension.equalsIgnoreCase("xls"))
        return createExcelDocument(content, fileName);
      else if(fileExtension.equalsIgnoreCase("ppt"))
        return createPowerPointDocument(content, fileName);
      else if(fileExtension.equalsIgnoreCase("pdf"))
        return createPDFDocument(content, fileName);
      else if(fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg") || 
          fileExtension.equalsIgnoreCase("png"))
        return createImageDocument(content, fileName);
      else return false;
    } catch(Exception ex) {
      return false;
    }
  }
  // Create Excel (.xls) document
  @SuppressWarnings("deprecation")
  public boolean createExcelDocument(String content, String fileName) {
    try {			

      HSSFWorkbook hwb=new HSSFWorkbook();
      HSSFSheet sheet =  hwb.createSheet("new sheet");
      String[] strs = this.splitByNumber(content, 500);
      for(int i = 0;i<strs.length;i++) {
        HSSFRow rowhead =   sheet.createRow((short)i);
        rowhead.createCell((short) 0).setCellValue(strs[i]);
      }
      FileOutputStream fileOut =  new FileOutputStream(fileName);
      hwb.write(fileOut);
      fileOut.close();			
      return true;
    } catch(Exception ex) {
      return false;
    } 
  }
  // Create powerpoint (.ppt) document
  public boolean createPowerPointDocument(String content, String fileName) {
    try {
      //create a new empty slide show
      SlideShow ppt = new HSLFSlideShow();
      Slide s1 = ppt.createSlide();
      HSLFTextBox txt = new HSLFTextBox();
      txt.setText(content);
      s1.addShape(txt);
      //save changes in a file
      FileOutputStream out = new FileOutputStream(fileName);
      ppt.write(out);
      out.close();
      return true;
    } catch(Exception ex) {
      return false;
    }
  }	
  // Create PDF (.pdf) document
  public boolean createPDFDocument(String content, String fileName) {
    try {
      OutputStream file = new FileOutputStream(new File(fileName));
      com.itextpdf.text.Document document = new com.itextpdf.text.Document();
      com.itextpdf.text.pdf.PdfWriter.getInstance(document, file);
      document.open();
      document.add(new com.itextpdf.text.Paragraph(content));
      document.add(new com.itextpdf.text.Paragraph(new Date().toString()));
      document.close();
      file.close();
      return true;
    } catch(Exception ex) {
      return false;
    }
  }
  // Create image (.png, .jpg, .jpeg) 
  public boolean createImageDocument(String content, String fileName) {
    try {
      String fileExtension = fileName.substring(fileName.indexOf('.') + 1);
      OutputStream file = new FileOutputStream(new File("temp.pdf"));
      com.itextpdf.text.Document document = new com.itextpdf.text.Document();
      com.itextpdf.text.pdf.PdfWriter.getInstance(document, file);
      document.open();
      document.add(new com.itextpdf.text.Paragraph(content));
      document.add(new com.itextpdf.text.Paragraph(new Date().toString()));
      document.close();
      file.close();

      //config option 1:convert all document to image
      String [] args_1 =  new String[5];
      args_1[0]  = "-outputPrefix";
      args_1[1]  = fileName.substring(0, fileName.indexOf("."));
      args_1[2]  = "-imageType";
      args_1[3]  = fileExtension;
      args_1[4]  = "temp.pdf";
      PDFToImage.main(args_1);
      new File("temp.pdf").delete();
      return true;
    } catch(Exception ex) {
      new File("temp.pdf").delete();
      return false;
    } 
  }

  public void read(InputStream is, OutputStream os) throws Exception {
    int bufferLength = 1024;
    int readLength = 0;
    while (readLength > -1) {
      byte[] chunk = new byte[bufferLength];
      readLength = is.read(chunk);
      if (readLength > 0) {
        os.write(chunk, 0, readLength);
      }
    }
    os.flush();
    os.close();
  }

  private String[] splitByNumber(String s, int size) {
    if(s == null || size <= 0)
      return null;
    int chunks = s.length() / size + ((s.length() % size > 0) ? 1 : 0);
    String[] arr = new String[chunks];
    for(int i = 0, j = 0, l = s.length(); i < l; i += size, j++)
      arr[j] = s.substring(i, Math.min(l, i + size));
    return arr;
  }
}