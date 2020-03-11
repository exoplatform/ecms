package org.exoplatform.services.cms.documents.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.services.cms.documents.DocumentMetadataPlugin;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;

/**
 * The Class DocumentMetadataPluginImpl is an implementation of DocumentMetadataPlugin
 * that uses Apache POI for adding the metadata.
 * 
 */
public class ApachePOIMetadataPluginImpl extends BaseComponentPlugin implements DocumentMetadataPlugin {

  /** The Constant PPTX_EXTENSION. */
  private static final String PPTX_EXTENSION       = ".pptx";

  /** The Constant XLSX_EXTENSION. */
  private static final String XLSX_EXTENSION       = ".xlsx";

  /** The Constant DOCX_EXTENSION. */
  private static final String DOCX_EXTENSION       = ".docx";

  /** The metadataFormat. */
  private final SimpleDateFormat metadataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  /** The Constant LOG. */
  private static final Log    LOG                  = ExoLogger.getLogger(ApachePOIMetadataPluginImpl.class);
  
  /** The organization. */
  protected final OrganizationService organization;
  
  /**
   * Instantiates a new document metadata plugin impl.
   *
   * @param organization the organization
   */
  public ApachePOIMetadataPluginImpl(OrganizationService organization) {
    this.organization = organization;
    metadataFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Adds metadata to the office document (creator, content type, created).
   * 
   * @param source the source of file
   * @param template the template
   * @return stream with metadata
   * @throws Exception the exception
   */
  @Override
  public InputStream addMetadata(InputStream source, DocumentTemplate template) throws Exception {
    POIXMLDocument document = getDocument(source, template.getExtension());
    POIXMLProperties props = document.getProperties();
    POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();
    coreProps.setCreator(getCurrentUserDisplayName());
    coreProps.setContentType(template.getMimeType());
    coreProps.setCreated(metadataFormat.format(new Date()));
    File tempFile = File.createTempFile("editor-document", ".tmp");
    FileOutputStream fos = new FileOutputStream(tempFile);
    document.write(fos);
    document.close();
    fos.close();
    return new DeleteOnCloseFileInputStream(tempFile);
  }

  /**
   * Gets POIXMLDocument from inputStream and extension. Supports .docx, .xlsx and .pptx extensions.s
   * 
   * @param source the source
   * @param extension the extension
   * @return POIXMLDocument
   * @throws Exception the exception
   */
  protected POIXMLDocument getDocument(InputStream source, String extension) throws Exception {
    if (extension == null) {
      throw new Exception("Cannot provide POIXMLDocument - extension is null");
    }
    switch (extension) {
    case DOCX_EXTENSION:
      return new XWPFDocument(source);
    case XLSX_EXTENSION:
      return new XSSFWorkbook(source);
    case PPTX_EXTENSION:
      return new XMLSlideShow(source);
    default:
      throw new Exception("The document format " + extension + " is not supported");
    }
  }

  /**
   * Gets display name of current user. In case of any errors return current userId
   * 
   * @return the display name
   */
  protected String getCurrentUserDisplayName() {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    try {
      return organization.getUserHandler().findUserByName(userId).getDisplayName();
    } catch (Exception e) {
      LOG.error("Error searching user " + userId, e);
      return userId;
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
    public DeleteOnCloseFileInputStream(String fileName) throws FileNotFoundException{
       this(new File(fileName));
    }
    
    /**
     * Instantiates a new delete on close file input stream.
     *
     * @param file the file
     * @throws FileNotFoundException the file not found exception
     */
    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException{
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
           if(file != null) {
              file.delete();
              file = null;
          }
        }
    }
 }

}
