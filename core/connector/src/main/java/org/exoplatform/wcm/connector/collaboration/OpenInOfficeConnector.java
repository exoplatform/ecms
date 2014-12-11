package org.exoplatform.wcm.connector.collaboration;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.documents.impl.DocumentType;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.json.JSONObject;

import javax.jcr.Node;
import javax.jcr.Session;
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
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          toannh@exoplatform.com
 * Dec 09, 2014
 * Provider all rest methods of Open Document feature.
 */
@Path("/office/")
public class OpenInOfficeConnector implements ResourceContainer {

  private final String OPEN_DOCUMENT_ON_DESKTOP_ICO = "uiIcon16x16FileDefault";
  private final String CONNECTOR_BUNDLE_LOCATION = "locale.wcm.resources.WCMResourceBundleConnector";
  private final String OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY = "OpenInOfficeConnector.label.exo.remote-edit.desktop";
  private final String OPEN_DOCUMENT_IN_DESKTOP_APP_RESOURCE_KEY="OpenInOfficeConnector.label.exo.remote-edit.desktop-app";
  private final String OPEN_DOCUMENT_DEFAULT_TITLE="Open";

  private final int CACHED_TIME = 60*24*30*12;

  /**
   * Return a JsonObject's check file to open
   * @param request
   * @param objId
   * @return
   * @throws Exception
   */
  @GET
  @Path("/updateDocumentLabel")
  public Response updateDocumentLabel(@Context Request request,
          @QueryParam("objId") String objId, @QueryParam("lang") String language
  ) throws Exception {
    String extension = objId.substring(objId.lastIndexOf(".") + 1, objId.length());
    EntityTag etag = new EntityTag(Integer.toString((extension+"_"+language).hashCode()));
    Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
    if(builder!=null) return builder.build();

    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    DocumentTypeService documentTypeService = WCMCoreUtils.getService(DocumentTypeService.class);

    CacheControl cc = new CacheControl();
    cc.setMaxAge(CACHED_TIME);

    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(CONNECTOR_BUNDLE_LOCATION, new Locale(language));
    String title = resourceBundle!=null?resourceBundle.getString(OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY):OPEN_DOCUMENT_DEFAULT_TITLE;
    String ico = OPEN_DOCUMENT_ON_DESKTOP_ICO;

    DocumentType documentType = documentTypeService.getDocumentType(extension);

    if(documentType !=null && resourceBundle !=null ){
      try {
        if(!StringUtils.isEmpty(resourceBundle.getString(documentType.getResourceBundleKey())))
         title = resourceBundle.getString(documentType.getResourceBundleKey());
      }catch(Exception ex){
        title = resourceBundle.getString(OPEN_DOCUMENT_IN_DESKTOP_APP_RESOURCE_KEY)+" "+ documentType.getResourceBundleKey();
      }
      if(!StringUtils.isEmpty(documentType.getIconClass())) ico=documentType.getIconClass();
    }

    JSONObject rs = new JSONObject();
    rs.put("ico", ico);
    rs.put("title", title);

    builder = Response.ok(rs.toString(), MediaType.APPLICATION_JSON);
    builder.tag(etag);
    builder.cacheControl(cc);
    return builder.build();
  }

  /**
   * Return a JsonObject's check a version when file has been opened successfully by desktop application
   * @param request
   * @param filePath
   * @return
   * @throws Exception
   */
  @GET
  @Path("/checkout")
  public Response checkout(@Context Request request,
                          @QueryParam("filePath") String filePath,
                          @QueryParam("workspace") String workspace
  ) throws Exception {
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, WCMCoreUtils.getRepository());
    Node node = (Node)session.getItem(filePath);
    if(!node.isCheckedOut()) node.checkout();
    return Response.ok(String.valueOf(node.isCheckedOut()), MediaType.TEXT_PLAIN).build();
  }
}
