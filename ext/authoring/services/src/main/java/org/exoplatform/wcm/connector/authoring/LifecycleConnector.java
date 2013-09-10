package org.exoplatform.wcm.connector.authoring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Returns a list of content during a given state range of the publication lifecycle.
 *
 * @LevelAPI Provisional
 *
 * @anchor LifecycleConnector
 */
@Path("/authoring/")
public class LifecycleConnector implements ResourceContainer {

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY        = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  /**
   * Returns a list of content from the beginning state.
   *
   * {@code
   * For example: http://localhost:8080/portal/rest/authoring/bystate/?fromstate
   * =draft&user=root&lang=en&workspace=collaboration
   * }
   *
   * @param fromstate The beginning state of the content.
   * @param user  The author of the content.
   * @param lang  The language of the content.
   * @param workspace The workspace name which contains the content.
   * @param json The format of the returned data.
   * @return List of content.
   * @throws Exception The exception
   *
   * @anchor LifecycleConnector.byState
   */
  @GET
  @Path("/bystate/")
  public Response byState(@QueryParam("fromstate") String fromstate,
                          @QueryParam("user") String user,
                          @QueryParam("lang") String lang,
                          @QueryParam("workspace") String workspace,
                          @QueryParam("json") String json) throws Exception {
    return getContents(fromstate, null, null, user, lang, workspace, json);
  }

  /**
   * Returns a list of content from the beginning state to the last state.
   *
   * {@code
   * For example: http://localhost:8080/portal/rest/authoring/tostate/?fromstate
   * =draft&tostate=pending&user=root&lang=en&workspace=collaboration
   * }
   *
   * @param fromstate  The beginning state of the content.
   * @param tostate The destination state of the content.
   * @param user The author of the content.
   * @param lang The language of the content.
   * @param workspace The workspace name which contains the content.
   * @param json The format of the returned data.
   * @return List of content.
   * @throws Exception The exception
   *
   * @anchor LifecycleConnector.toState
   */
  @GET
  @Path("/tostate/")
  public Response toState(@QueryParam("fromstate") String fromstate,
                          @QueryParam("tostate") String tostate,
                          @QueryParam("user") String user,
                          @QueryParam("lang") String lang,
                          @QueryParam("workspace") String workspace,
                          @QueryParam("json") String json) throws Exception {
    return getContents(fromstate, tostate, null, user, lang, workspace, json);
  }

  /**
   * Returns a list of content from the given beginning state to the published state and before the given date.
   *
   * {@code
   * For example: http://localhost:8080/portal/rest/authoring/bydate/?fromstate
   * =staged&date=2&lang=en&workspace=collaboration
   * }
   *
   * @param fromstate The beginning state of the content.
   * @param date The date before when the content is published.
   * @param workspace The workspace name which contains the content.
   * @param lang The language of the content.
   * @param json The format of the returned data.
   * @return List of content.
   * @throws Exception The exception
   *
   * @anchor LifecycleConnector.byDate
   */
  @GET
  @Path("/bydate/")
  public Response byDate(@QueryParam("fromstate") String fromstate,
                         @QueryParam("date") String date,
                         @QueryParam("lang") String lang,
                         @QueryParam("workspace") String workspace,
                         @QueryParam("json") String json) throws Exception {
    return getContents(fromstate, null, date, null, lang, workspace, json);
  }

  /**
   * @param fromstate
   * @param tostate
   * @param user
   * @param lang
   * @param workspace
   * @param asJSon
   * @return
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private Response getContents(String fromstate,
                               String tostate,
                               String date,
                               String user,
                               String lang,
                               String workspace,
                               String asJSon) throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = document.createElement("contents");
      document.appendChild(root);

      PublicationManager manager = WCMCoreUtils.getService(PublicationManager.class);
      List<Node> nodes = manager.getContents(fromstate, tostate, date, user, lang, workspace);

      JSONArray jsonList = new JSONArray();
      for (Node node : nodes) {
        String name = node.getName();
        String path = node.getPath();
        String title = null;
        String pubDate = null;
        if (node.hasProperty("exo:title"))
          title = node.getProperty("exo:title").getString();
        if (node.hasProperty("publication:startPublishedDate")) {
          pubDate = node.getProperty("publication:startPublishedDate").getString();
        }
        JSONObject jsonElt = new JSONObject();

        jsonElt.put("name", name);
        if (title != null)
          jsonElt.put("title", title);
        jsonElt.put("path", path);

        Element element = document.createElement("content");
        element.setAttribute("name", name);
        if (title != null)
          element.setAttribute("title", title);
        element.setAttribute("path", path);
        if (pubDate != null) {
          jsonElt.put("publishedDate", pubDate);
          element.setAttribute("publishedDate", pubDate);
        }
        root.appendChild(element);
        jsonList.add(jsonElt);

      }

      if ("true".equals(asJSon))
        return Response.ok(jsonList.toString(), MediaType.TEXT_PLAIN)
                       .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      else
        return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                       .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
    } catch (Exception e) {
      Response.serverError().build();
    }
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }
}
