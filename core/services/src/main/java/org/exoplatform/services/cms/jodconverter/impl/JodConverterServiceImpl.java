package org.exoplatform.services.cms.jodconverter.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.jodconverter.JodConverterService;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

public class JodConverterServiceImpl implements JodConverterService {
	
	private SocketOpenOfficeConnection socketconnection;
	private OpenOfficeDocumentConverter converter;
	private DefaultDocumentFormatRegistry format;
	
	public JodConverterServiceImpl(InitParams initParams) throws IOException, FileNotFoundException, Exception {
		String host = initParams.getValueParam("host").getValue();
		int port = Integer.parseInt(initParams.getValueParam("port").getValue());
		socketconnection = new SocketOpenOfficeConnection(host, port);
		format = new DefaultDocumentFormatRegistry();
		converter = new OpenOfficeDocumentConverter(socketconnection);
	}
	
	/**
	 * Convert InputStream in with formatInput format to OutputStream out with
	 * formatOutput
	 * 
	 * @param input
	 * @param formatInput
	 * @param out
	 * @param formatOutput
	 * @throws Exception
	 */
	public void convert(InputStream input, String formatInput, OutputStream out,
			String formatOutput) throws ConnectException, Exception {
		converter.convert(input, format.getFormatByFileExtension(formatInput), out,
				format.getFormatByFileExtension(formatOutput));
	}
}
