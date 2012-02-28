package org.exoplatform.services.cms.jodconverter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.artofsolving.jodconverter.office.OfficeException;

public interface JodConverterService {

  /**
   * Convert InputStream in with formatInput format to OutputStream out with formatOutput
   *
   * @param input
   * @param formatInput
   * @param out
   * @param formatOutput
   * @throws Exception
   * @Deprecated This method is not support by JODConverter 3.0 anymore, please use
   *             {@link #convert(File, File, String)} instead of.
   */
  public void convert(InputStream input, String formatInput, OutputStream out, String formatOutput) throws Exception;

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
