package org.exoplatform.services.cms.documents;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.exoplatform.services.cms.documents.exception.DocumentExtensionNotSupportedException;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Interface DocumentMetadataPlugin is used to add metadata to the documents.
 */
public interface DocumentMetadataPlugin {

  /**
   * Updates metadata of the document (creator, created date).
   * 
   * @param extension the extension of the file
   * @param source the source of template file
   * @param created the created date of the file
   * @param creator the name of creator 
   * @return the result stream of the file with correct metadata
   * @throws IOException the IOException
   * @throws DocumentExtensionNotSupportedException the DocumentExtensionNotSupportedException
   */
  InputStream updateMetadata(WebuiRequestContext context,
                             String extension,
                             InputStream source,
                             Date created,
                             String creator,
                             String language) throws IOException, DocumentExtensionNotSupportedException;

  /**
   * Gets the supported extensions.
   *
   * @return the supported extensions
   */
  List<String> getSupportedExtensions();

  /**
   * Checks if the provided extension supported by the metadata plugin.
   * 
   * @param extension the extension
   * @return true if supported, false otherwise
   */
  boolean isExtensionSupported(String extension);

}
