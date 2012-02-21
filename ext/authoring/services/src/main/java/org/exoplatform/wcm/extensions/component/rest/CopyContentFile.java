package org.exoplatform.wcm.extensions.component.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.extensions.security.SHAMessageDigester;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
@Path("/copyfile/")
public class CopyContentFile implements ResourceContainer {

  private static final Log    log                           = ExoLogger.getLogger(CopyContentFile.class);

  private static final String OK_RESPONSE                   = "OK";

  private static final String KO_RESPONSE                   = "KO";

  private static String       stagingStorage;

  private static String       targetKey;

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY        = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  public CopyContentFile(InitParams params) {
    stagingStorage = params.getValueParam("stagingStorage").getValue();
    targetKey = params.getValueParam("targetKey").getValue();
  }

  @POST
  @Path("/copy/")
  public Response copyFile(String param) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Start Execute CopyContentFile Web Service");
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    try {

      String[] tabParam = param.split("&&");
      String timesTamp = tabParam[0].split("=")[1];
      String clientHash = tabParam[1].split("=")[1];
      String contents = tabParam[2].split("contentsfile=")[1];
      String[] tab = targetKey.split("$TIMESTAMP");
      StringBuffer resultKey = new StringBuffer();
      for (int k = 0; k < tab.length; k++) {
        resultKey.append(tab[k]);
        if (k != (tab.length - 1))
          resultKey.append(timesTamp);
      }
      String serverHash = SHAMessageDigester.getHash(resultKey.toString());
      if (serverHash != null && serverHash.equals(clientHash)) {
        Date date = new Date();
        long time = date.getTime();
        File stagingFolder = new File(stagingStorage);
        if (!stagingFolder.exists())
          stagingFolder.mkdirs();
        File contentsFile = new File(stagingStorage + File.separator + clientHash + "-" + time
            + ".xml");
        OutputStream ops = new FileOutputStream(contentsFile);
        ops.write(contents.getBytes());
        ops.close();
      } else {
        if (log.isWarnEnabled()) {
          log.warn("Anthentification failed...");
        }
        return Response.ok(KO_RESPONSE + "...Anthentification failed", "text/plain")
                               .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
      }
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("error when copying content file" + ex.getMessage());
      }
      return Response.ok(KO_RESPONSE + "..." + ex.getMessage(), "text/plain")
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    }
    if (log.isDebugEnabled()) {
      log.debug("Start Execute CopyContentFile Web Service");
    }
    return Response.ok(OK_RESPONSE
                           + "...content has been successfully copied in the production server",
                       "text/plain")
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();

  }

}
