package org.exoplatform.ecm.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.rest.resource.ResourceContainer;

import com.ibm.icu.text.Transliterator;

@Path("/l11n/")
public class LocalizationConnector implements ResourceContainer {

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  private static final String CONTENT_TYPE = "Content-Type";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  @GET
  @Path("/cleanName/")
  public Response getCleanName(
      @QueryParam("name") String name
  ) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    try {
      return Response.ok(org.exoplatform.services.cms.impl.Utils.cleanString(name))
                     .header(CONTENT_TYPE, "text/html; charset=utf-8")
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } catch (Exception e) {
      Response.serverError().build();
    }
    return Response.ok()
                   .header(CONTENT_TYPE, "text/html; charset=utf-8")
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
  
  @GET
  @Path("/convertName/")
  public Response convertName(
      @QueryParam("name") String name
  ) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    try {
      return Response.ok(Text.convertJcrChars(name))
                     .header(CONTENT_TYPE, "text/html; charset=utf-8")
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } catch (Exception e) {
      Response.serverError().build();
    }
    return Response.ok()
                   .header(CONTENT_TYPE, "text/html; charset=utf-8")
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

}
