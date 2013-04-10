package org.exoplatform.services.cms.jodconverter;

import java.io.File;

import org.artofsolving.jodconverter.office.OfficeException;

/**
 * JodConverter is used to convert documents into different office formats.
 *
 * @LevelAPI Experimental
 */
public interface JodConverterService {

  /**
   * Convert input File to output File with the outputFormat.
   *
   * @param input The input file
   * @param output The output file
   * @param outputFormat The extension of the output file.
   * @return True if convert successfully; otherwise, return false.
   * @throws OfficeException
   */
  public boolean convert(File input, File output, String outputFormat) throws OfficeException;

}
