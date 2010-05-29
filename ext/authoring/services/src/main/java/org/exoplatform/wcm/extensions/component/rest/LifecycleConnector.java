package org.exoplatform.wcm.extensions.component.rest;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Path("/authoring/")
public class LifecycleConnector implements ResourceContainer {

  private static final Log log         = ExoLogger.getLogger(LifecycleConnector.class);

  /**
   * 
   * example : http://localhost:8080/ecmdemo/rest-ecmdemo/authoring/bystate/?fromstate=draft&user=root&lang=en&workspace=collaboration
   * 
   * @param fromstate
   * @param user
   * @param lang
   * @param workspace
   * @param path
   * @return
   * @throws Exception
   */
  @GET
  @Path("/bystate/")
  public Response byState(		  
		  @QueryParam("fromstate") String fromstate, 
		  @QueryParam("user") String user,
		  @QueryParam("lang") String lang,
		  @QueryParam("workspace") String workspace,
		  @QueryParam("json") String json ) throws Exception {
	  return getContents(fromstate, null, null, user, lang, workspace, json); 
  }

  /**
   * 
   * example : http://localhost:8080/ecmdemo/rest-ecmdemo/authoring/tostate/?fromstate=draft&tostate=pending&user=root&lang=en&workspace=collaboration
   * 
   * @param fromstate
   * @param user
   * @param lang
   * @param workspace
   * @param path
   * @return
   * @throws Exception
   */
  @GET
  @Path("/tostate/")
  public Response toState(		  
		  @QueryParam("fromstate") String fromstate, 
		  @QueryParam("tostate") String tostate, 
		  @QueryParam("user") String user,
		  @QueryParam("lang") String lang,
		  @QueryParam("workspace") String workspace,
		  @QueryParam("json") String json ) throws Exception {
	  return getContents(fromstate, tostate, null, user, lang, workspace, json); 
  }
  
  /**
   * 
   * example : http://localhost:8080/ecmdemo/rest-ecmdemo/authoring/bydate/?fromstate=staged&date=2&lang=en&workspace=collaboration
   * 
   * @param fromstate
   * @param user
   * @param lang
   * @param workspace
   * @param path
   * @return
   * @throws Exception
   */
  @GET
  @Path("/bydate/")
  public Response byDate(		  
		  @QueryParam("fromstate") String fromstate, 
		  @QueryParam("date") String date, 
		  @QueryParam("lang") String lang,
		  @QueryParam("workspace") String workspace,
		  @QueryParam("json") String json ) throws Exception {
	  return getContents(fromstate, null, date, null, lang, workspace, json); 
  }
  
  
  /**
   * 
   * 
   * @param fromstate
   * @param tostate
   * @param user
   * @param lang
   * @param workspace
   * @return
   * @throws Exception
   */
  private Response getContents(String fromstate, 
		  String tostate, 
		  String date,
		  String user,
		  String lang,
		  String workspace,
		  String asJSon) throws Exception {
	  
	  try {
		  StringBuffer json = new StringBuffer();
		  Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		  Element root = document.createElement("contents");
		  document.appendChild(root);
		  
		  PublicationManager manager = (PublicationManager)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PublicationManager.class);
		  List<Node> nodes = manager.getContents(fromstate, tostate, date, user, lang, workspace);

		  json.append("[");
		  boolean first=true;
		  for (Node node:nodes) {
			  String name = node.getName();
			  String path = node.getPath();
			  String pubDate = (node.hasProperty("publication:startPublishedDate"))?node.getProperty("publication:startPublishedDate").getString():null;
			  if (!first) json.append(",");
			  first = false;
			  json.append("{");
			  json.append("\"name\":\""+name+"\"");
			  json.append(",\"path\":\""+path+"\"");
			  
			  Element element = document.createElement("content");
			  element.setAttribute("name", name);
			  element.setAttribute("path", path);
			  if (pubDate!=null) {
				  json.append(",\"publishedDate\":\""+pubDate+"\"");
				  element.setAttribute("publishedDate", pubDate);
			  }
			  root.appendChild(element);
			  json.append("}");
			  
		  }
		  json.append("]");

		  if ("true".equals(asJSon))
			  return Response.ok(json.toString(), MediaType.TEXT_PLAIN).build();
		  else
			  return Response.ok(new DOMSource(document), MediaType.TEXT_XML).build();
	  } catch (Exception e) {
		  Response.serverError().build();
	  }
	  return Response.ok().build();
  }
    
}
