package org.exoplatform.services.cms.documents.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.cms.documents.DocumentMetadataPlugin;
import org.exoplatform.services.cms.documents.exception.DocumentExtensionNotSupportedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class DocumentMetadataPlugin is an implementation of DocumentMetadataPlugin
 * that uses Apache POI for adding the metadata.
 * 
 */
public class ApachePOIMetadataPlugin extends BaseComponentPlugin implements DocumentMetadataPlugin {

  /** The Constant PPTX_EXTENSION. */
  private static final String       PPTX_EXTENSION       = ".pptx";

  /** The Constant XLSX_EXTENSION. */
  private static final String       XLSX_EXTENSION       = ".xlsx";

  /** The Constant DOCX_EXTENSION. */
  private static final String       DOCX_EXTENSION       = ".docx";

  /** The Constant SUPPORTED_EXTENSIONS. */
  private static final List<String> SUPPORTED_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(DOCX_EXTENSION,
                                                                                                      XLSX_EXTENSION,
                                                                                                      PPTX_EXTENSION));

  /** The metadataFormat. */
  private final SimpleDateFormat    metadataFormat       = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  /** The Constant LOG. */
  private static final Log          LOG                  = ExoLogger.getLogger(ApachePOIMetadataPlugin.class);

  /**
   * Instantiates a new document metadata plugin impl.
   */
  public ApachePOIMetadataPlugin() {
    // We should use UTC timezone instead of user's one to set correct dates using POI
    metadataFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream updateMetadata(WebuiRequestContext context,
                                    String extension,
                                    InputStream source,
                                    Date created,
                                    String creator,
                                    String language) throws IOException, DocumentExtensionNotSupportedException {
    File tempFile = File.createTempFile("editor-document", ".tmp");
    try (POIXMLDocument document = getDocument(source, extension);
         FileOutputStream fos = new FileOutputStream(tempFile))
    {
      updateDocLanguage(context, document, extension, language);
      POIXMLProperties props = document.getProperties();
      POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();
      coreProps.setCreator(creator);
      coreProps.getUnderlyingProperties().setLanguageProperty(language);
      coreProps.setCreated(metadataFormat.format(created));
      document.write(fos);
    }
    return new DeleteOnCloseFileInputStream(tempFile);
  }

  private void updateDocLanguage(WebuiRequestContext context, POIXMLDocument document, String extension, String language) {
    if(StringUtils.equals(extension, ".docx")) {
      //Change the document editing language with the user's platform language
      ((XWPFDocument) document).getStyles().getStyle("Normal").getCTStyle().getRPr().getLang().setVal(language);
    } else if (StringUtils.equals(extension, ".xlsx")) {
      //Rename every sheet created with a new name translated with the user's platform language
      ResourceBundle resourceBundle = context.getApplicationResourceBundle();
      String newSheetLabel = resourceBundle.getString("UINewDocumentForm.label.option.MicrosoftOfficeNewSheet");
              ((XSSFWorkbook) document).getCTWorkbook().getSheets().getSheetList().stream().forEach(sheet -> {
        sheet.setName(newSheetLabel+sheet.getSheetId());
      });
    } else if (StringUtils.equals(extension, ".pptx")) {
      ((XMLSlideShow) document).getCTPresentation().getDefaultTextStyle().getDefPPr().getDefRPr().setLang(language);
      //Change the language of each element found in the first slide to the user's language of the platform.
      ((XMLSlideShow) document).getSlides().get(0).getXmlObject().getCSld().getSpTree().getSpList().forEach(splist-> {
        if(splist.getTxBody() != null) {
          splist.getTxBody().getPArray(0).getEndParaRPr().setLang(language);
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getSupportedExtensions() {
    return SUPPORTED_EXTENSIONS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExtensionSupported(String extension) {
    return SUPPORTED_EXTENSIONS.contains(extension);
  }

  /**
   * Gets POIXMLDocument from inputStream and extension. Supports only SUPPORTED_EXTENSIONS
   *
   * @param source the source
   * @param extension the extension
   * @return POIXMLDocument
   * @throws DocumentExtensionNotSupportedException the document extension not supported exception
   * @throws IOException Signals that an I/O exception has occurred
   */
  protected POIXMLDocument getDocument(InputStream source, String extension) throws DocumentExtensionNotSupportedException,
                                                                             IOException {
    if (extension == null) {
      throw new DocumentExtensionNotSupportedException("Cannot provide POIXMLDocument - extension is null");
    }
    switch (extension.toLowerCase()) {
    case DOCX_EXTENSION:
      return new XWPFDocument(source);
    case XLSX_EXTENSION:
      return new XSSFWorkbook(source);
    case PPTX_EXTENSION:
      return new XMLSlideShow(source);
    default:
      throw new DocumentExtensionNotSupportedException("The document format " + extension + " is not supported");
    }
  }

  /**
   * The Class DeleteOnCloseFileInputStream.
   */
  public static class DeleteOnCloseFileInputStream extends FileInputStream {

    /** The file. */
    private File file;

    /**
     * Instantiates a new delete on close file input stream.
     *
     * @param fileName the file name
     * @throws FileNotFoundException the file not found exception
     */
    public DeleteOnCloseFileInputStream(String fileName) throws FileNotFoundException {
      this(new File(fileName));
    }

    /**
     * Instantiates a new delete on close file input stream.
     *
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     */
    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
    }

    /**
     * Close.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void close() throws IOException {
      try {
        super.close();
      } finally {
        if (file != null) {
          file.delete();
          file = null;
        }
      }
    }
  }

}
