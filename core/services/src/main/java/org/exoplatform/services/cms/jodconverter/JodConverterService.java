package org.exoplatform.services.cms.jodconverter;

import java.io.File;

import org.artofsolving.jodconverter.office.OfficeException;

public interface JodConverterService {

  /**
   * Convert input File to output File with outputFormat
   *
   * @param input the input file
   * @param output the output file
   * @param outputFormat the extension of output file
   * @throws OfficeException
   * @return true if convert successfully; otherwise, return false.
   */
  public boolean convert(File input, File output, String outputFormat) throws OfficeException;

}
