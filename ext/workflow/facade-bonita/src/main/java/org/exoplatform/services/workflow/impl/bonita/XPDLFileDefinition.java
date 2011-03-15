package org.exoplatform.services.workflow.impl.bonita;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.jcr.Node;

public class XPDLFileDefinition extends BARFileDefinition {

    public XPDLFileDefinition(InputStream inputStream) throws IOException {
        entries                       = new Hashtable<String, byte[]>();
        byte[] buffer                 = new byte[8192];

        // The entry is to be processed.
            BufferedInputStream in    = new BufferedInputStream(inputStream);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int count = 0;

            // Retrieve the corresponding bytes
            while((count = in.read(buffer, 0, buffer.length)) >= 0) {
              out.write(buffer, 0, count);
            }

            // Store the bytes in the hashtable
            entries.put("process.xpdl", out.toByteArray());
    }

    public XPDLFileDefinition(Node node) throws IOException {
        super(node);
    }

}
