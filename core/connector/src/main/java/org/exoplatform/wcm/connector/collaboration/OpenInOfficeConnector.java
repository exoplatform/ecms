package org.exoplatform.wcm.connector.collaboration;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Created by toannh on 11/27/14.
 * Provider all rest methods of Open Document feature.
 */
@Path("/office/")
public class OpenInOfficeConnector implements ResourceContainer {

  private final String OPEN_DOCUMENT_IN_EXCEL_ICO = "uiIcon16x16applicationxls";
  private final String OPEN_DOCUMENT_IN_WORD_ICO = "uiIcon16x16applicationmsword";
  private final String OPEN_DOCUMENT_IN_POWERPOINT_ICO = "uiIcon16x16applicationvndopenxmlformats-officedocumentpresentationmlpresentation";
  private final String OPEN_DOCUMENT_ON_DESKTOP_ICO = "uiIcon16x16FileDefault";

  private final String CONNECTOR_BUNDLE_LOCATION = "locale.wcm.resources.WCMResourceBundleConnector";
  private final String OPEN_DOCUMENT_IN_WORD_RESOURCE_KEY = "OpenInOfficeConnector.label.open-in-word";
  private final String OPEN_DOCUMENT_IN_EXCEL_RESOURCE_KEY = "OpenInOfficeConnector.label.open-in-excel";
  private final String OPEN_DOCUMENT_IN_POWERPOINT_RESOURCE_KEY = "OpenInOfficeConnector.label.open-in-powerpoint";
  private final String OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY = "OpenInOfficeConnector.label.open-in-desktop";

  private final String OPEN_DOCUMENT_DEFAULT_TITLE="Open";

  private final int CACHED_TIME = 60*24*30*12;

  private static String[] openInExcel = null, openInWord = null, openInPowerpoint = null;

  static {
    openInExcel = System.getProperty("open-in-excel")!=null?System.getProperty("open-in-excel").split(","):null;
    openInWord = System.getProperty("open-in-word")!=null?System.getProperty("open-in-word").split(","):null;
    openInPowerpoint = System.getProperty("open-in-powerpoint")!=null?System.getProperty("open-in-powerpoint").split(","):null;
  }

  /**
   * Return a JsonObject's check file to open
   * @param httpServletRequest
   * @param request
   * @param objId
   * @return
   * @throws Exception
   */
  @GET
  @Path("/updateDocumentLabel")
  public Response updateDocumentLabel(@Context HttpServletRequest httpServletRequest, @Context Request request,
          @QueryParam("objId") String objId, @QueryParam("lang") String language
  ) throws Exception {
    EntityTag etag = new EntityTag(Integer.toString((objId+"_"+language).hashCode()));
    Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
    if(builder!=null) return builder.build();

    CacheControl cc = new CacheControl();
    cc.setMaxAge(CACHED_TIME);
    String extension = objId.substring(objId.lastIndexOf(".") + 1, objId.length());
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(CONNECTOR_BUNDLE_LOCATION, new Locale(language));

    String ws = objId.split(":")[0];
    String nodePath = objId.split(":")[1];
    String repo = WCMCoreUtils.getRepository().getConfiguration().getName();

    String filePath = httpServletRequest.getScheme()+ "://" + httpServletRequest.getServerName() + ":"
            +httpServletRequest.getServerPort() + "/"
            + WCMCoreUtils.getRestContextName()+ "/private/jcr/" + repo + "/" + ws + nodePath;

    String title = resourceBundle!=null?resourceBundle.getString(OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY):OPEN_DOCUMENT_DEFAULT_TITLE;
    String ico = OPEN_DOCUMENT_ON_DESKTOP_ICO;

    if(ArrayUtils.indexOf(openInExcel, extension) != -1 && resourceBundle!=null) {
      title = resourceBundle.getString(OPEN_DOCUMENT_IN_EXCEL_RESOURCE_KEY);
      ico = OPEN_DOCUMENT_IN_EXCEL_ICO;
    }
    if(ArrayUtils.indexOf(openInPowerpoint, extension) != -1 && resourceBundle!=null) {
      title = resourceBundle.getString(OPEN_DOCUMENT_IN_POWERPOINT_RESOURCE_KEY);
      ico = OPEN_DOCUMENT_IN_POWERPOINT_ICO;
    }
    if(ArrayUtils.indexOf(openInWord, extension) != -1 && resourceBundle!=null) {
      title = resourceBundle.getString(OPEN_DOCUMENT_IN_WORD_RESOURCE_KEY);
      ico = OPEN_DOCUMENT_IN_WORD_ICO;
    }

    JSONObject rs = new JSONObject();
    rs.put("ico", ico);
    rs.put("filePath", filePath);
    rs.put("title", title);

    builder = Response.ok(rs.toString(), MediaType.APPLICATION_JSON);
    builder.tag(etag);
    builder.cacheControl(cc);
    return builder.build();
  }

}
