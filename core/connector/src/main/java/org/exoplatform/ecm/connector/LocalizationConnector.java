package org.exoplatform.ecm.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
      return Response.ok(cleanString(name))
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


  /**
   * Clean string.
   *
   * @param str the str
   *
   * @return the string
   */
  private static String cleanString(String str) {
    Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
    str = accentsconverter.transliterate(str);
    //the character ? seems to not be changed to d by the transliterate function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
    // delete special character
    for(int i = 0; i < cleanedStr.length(); i++) {
      char c = cleanedStr.charAt(i);
      if(c == ' ') {
        if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
          cleanedStr.deleteCharAt(i--);
        } else {
          c = '-';
          cleanedStr.setCharAt(i, c);
        }
        continue;
      }
      if(i > 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        cleanedStr.deleteCharAt(i--);
        continue;
      }
      if(i > 0 && c == '-' && cleanedStr.charAt(i-1) == '-')
        cleanedStr.deleteCharAt(i--);
    }
    String clean = cleanedStr.toString().toLowerCase();
    if (clean.endsWith("-")) {
      clean = clean.substring(0, clean.length()-1);
    }

    return clean;
  }


}
