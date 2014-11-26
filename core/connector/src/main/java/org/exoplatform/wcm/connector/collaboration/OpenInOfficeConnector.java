package org.exoplatform.wcm.connector.collaboration;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
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


/**
 * Created by toannh on 11/27/14.
 * Provider all rest methods of Open Document features.
 */
@Path("/office/")
public class OpenInOfficeConnector implements ResourceContainer {

  private final String OPEN_EXCEL = "Open_Excel";
  private final String OPEN_POWERPOINT = "Open_Powerpoint";
  private final String OPEN_WORD = "Open_Word";
  private final String OPEN_DESKTOP = "Open_Desktop";

  private final String OPEN_DOCUMENT_IN_EXCEL_ICO = "uiIconEcmsOpenDocumentInExcel";
  private final String OPEN_DOCUMENT_IN_WORD_ICO = "uiIconEcmsOpenDocumentInWord";
  private final String OPEN_DOCUMENT_IN_POWERPOINT_ICO = "uiIconEcmsOpenDocumentInPowerpoint";
  private final String OPEN_DOCUMENT_IN_DESKTOP_ICO = "uiIconEcmsOpenDocumentInDesktop";

  private final int CACHED_TIME = 60*24*30*12;

  private static String[] openInExcel = null, openInWord = null, openInPowerpoint = null;

  static {
    openInExcel = System.getProperty("open-in-excel")!=null?System.getProperty("open-in-excel").split(","):null;
    openInWord = System.getProperty("open-in-word")!=null?System.getProperty("open-in-word").split(","):null;
    openInPowerpoint = System.getProperty("open-in-powerpoint")!=null?System.getProperty("open-in-powerpoint").split(","):null;
  }

  private JSONObject rs = new JSONObject();

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
  public Response openDocument(@Context HttpServletRequest httpServletRequest, @Context Request request,
          @QueryParam("objId") String objId
  ) throws Exception {
    CacheControl cc = new CacheControl();
    cc.setMaxAge(CACHED_TIME);
    String extension = objId.substring(objId.lastIndexOf(".")+1, objId.length());

    EntityTag etag = new EntityTag(Integer.toString(objId.hashCode()));
    Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
    if(builder!=null) return builder.build();

    String ws = objId.split(":")[0];
    String nodePath = objId.split(":")[1];
    String portalName = WCMCoreUtils.getPortalName();
    String repo = WCMCoreUtils.getRepository().getConfiguration().getName();

    String filePath = httpServletRequest.getScheme()+ "://" + httpServletRequest.getServerName() + ":"
            +httpServletRequest.getServerPort() + "/"
            + getRestContextName(portalName)+ "/private/jcr/" + repo + "/" + ws + nodePath;

    String result=OPEN_DESKTOP;

    if(ArrayUtils.indexOf(openInExcel, extension) != -1) {
      result = OPEN_EXCEL;
      rs.put("ico", OPEN_DOCUMENT_IN_EXCEL_ICO);
    }
    if(ArrayUtils.indexOf(openInPowerpoint, extension) != -1) {
      result = OPEN_POWERPOINT;
      rs.put("ico", OPEN_DOCUMENT_IN_POWERPOINT_ICO);
    }
    if(ArrayUtils.indexOf(openInWord, extension) != -1) {
      result = OPEN_WORD;
      rs.put("ico", OPEN_DOCUMENT_IN_WORD_ICO);
    }

    if(OPEN_DESKTOP.equals(result)) rs.put("ico", OPEN_DOCUMENT_IN_DESKTOP_ICO);

    rs.put("type", result);
    rs.put("filePath", filePath);

    builder = Response.ok(rs.toString(), MediaType.APPLICATION_JSON);
    builder.tag(etag);
    builder.cacheControl(cc);
    return builder.build();
  }

  private String getRestContextName(String portalContainerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.
            getComponentInstance(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(portalContainerName);
  }

}
