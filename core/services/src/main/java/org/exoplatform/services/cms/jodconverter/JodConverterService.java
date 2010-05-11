package org.exoplatform.services.cms.jodconverter;

import java.io.InputStream;
import java.io.OutputStream;

public interface JodConverterService {
	
	/**
	 * Convert InputStream in with formatInput format to OutputStream out with formatOutput 
	 * @param input
	 * @param formatInput
	 * @param out
	 * @param formatOutput
	 * @throws Exception
	 */
	public void convert(InputStream input, String formatInput, OutputStream out, String formatOutput) throws Exception;

}
