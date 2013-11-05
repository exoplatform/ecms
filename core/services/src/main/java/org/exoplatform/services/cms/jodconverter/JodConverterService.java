package org.exoplatform.services.cms.jodconverter;

import java.io.File;

import org.artofsolving.jodconverter.office.OfficeException;

/**
 * Converts documents into different office formats.
 *
 * @LevelAPI Experimental
 */
public interface JodConverterService {

  /**
   * Converts an input file into output one with a given output format.
   *
   * @param input The input file.
   * @param output The output file.
   * @param outputFormat Format of the output file.
   * @return "True" if conversion is successful. Otherwise, it returns "false".
   * @throws OfficeException
   */
  public boolean convert(File input, File output, String outputFormat) throws OfficeException;

}
