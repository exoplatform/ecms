eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
  var product = new Product();
  
  product.name = "eXoWCM" ;
  product.portalwar = "portal.war" ;
	product.ecmdemowar = "ecmdemo.war";
  product.codeRepo = "ecm/wcm" ;
  product.useContentvalidation = true;
  product.version = "${project.version}" ;
  product.workflowVersion = "${project.version}" ;
  product.contentvalidationVersion = "${org.exoplatform.ecms.version}";
  product.serverPluginVersion = "${org.exoplatform.portal.version}" ;
  product.workflowJbpmVersion = "${org.jbpm.jbpm3}";
  product.workflowBonitaVersion = "${bonita.version}";
  
  var kernel = Module.GetModule("kernel") ;
  var core = Module.GetModule("core") ;
  var ws = Module.GetModule("ws");
  var eXoJcr = Module.GetModule("jcr", {kernel : kernel, core : core, ws : ws}) ;
  var portal = Module.GetModule("portal", {kernel : kernel, ws:ws, core : core, eXoJcr : eXoJcr});         
  var dms = Module.GetModule("dms", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr , portal : portal});
  var wcm = Module.GetModule("wcm", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr, portal : portal, dms : dms});
  	
  portal.starter = new Project("org.exoplatform.portal", "exo.portal.starter.war", "war", portal.version);
  portal.starter.deployName = "starter";
  product.addDependencies(portal.starter);
  
  product.addDependencies(portal.eXoGadgetServer) ;
  product.addDependencies(portal.eXoGadgets) ;
  product.addDependencies(portal.portlet.exoadmin) ;
  product.addDependencies(portal.portlet.web) ;
  product.addDependencies(portal.portlet.dashboard) ;  
  product.addDependencies(portal.web.eXoResources);
  product.addDependencies(portal.web.rest);
  product.addDependencies(portal.web.portal);
  product.addDependencies(portal.webui.portal);
  
  product.addDependencies(dms.gadgets);
  product.addDependencies(dms.portlet.ecmadmin);
  product.addDependencies(dms.portlet.ecmexplorer);
  product.addDependencies(dms.portlet.ecmbrowsecontent);
  product.addDependencies(dms.web.eXoDMSResources) ;
  
  product.addDependencies(wcm.authoring.war);
  product.addDependencies(wcm.core.war);
  product.addDependencies(wcm.extension.war);
  product.addDependencies(wcm.waiextension.war);
  product.addDependencies(wcm.waitemplate.war);
  product.addDependencies(wcm.portlet.webpresentation);
  product.addDependencies(wcm.portlet.websearches); 
  product.addDependencies(wcm.portlet.newsletter); 
  product.addDependencies(wcm.portlet.formgenerator);
  product.addDependencies(wcm.portlet.seo);
  product.addDependencies(wcm.gadget.favorites);
  product.addDependencies(wcm.web.eXoWCMResources) ;
  product.addDependencies(wcm.web.eXoStaticResources) ;
  product.addDependencies(wcm.demo.portal);
  product.addDependencies(wcm.demo.rest);

  product.addServerPatch("tomcat", wcm.server.tomcat.patch) ;
  product.addServerPatch("jboss",  portal.server.jboss.patch) ;
  product.addServerPatch("jbossear",  portal.server.jbossear.patch) ;  

	product.addServerPatch("jboss",  wcm.server.jboss.patch) ;
  product.addServerPatch("jbossear", wcm.server.jbossear.patch) ;  
	
	if(enableWorkflow) {
		var workflow = Module.GetModule("workflow", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr, portal : portal});
		product.addDependencies(workflow.web.eXoWorkflowResources);
		product.addDependencies(workflow.extension.webapp);
	}	
	
  product.module = wcm ;
  product.dependencyModule = [kernel, core, ws, eXoJcr, portal, dms];
  // Use new version of commons-logging override Product.preDeploy()
  product.preDeploy = function() { 
	  product.removeDependency(new Project("commons-logging", "commons-logging", "jar", "1.0.4"));
	  product.addDependencies(new Project("commons-logging", "commons-logging", "jar", "1.1.1"));
  }
  return product ;
}
