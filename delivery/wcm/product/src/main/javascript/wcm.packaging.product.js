eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
  var product = new Product();
  
  product.name = "eXoWCM" ;
  product.portalwar = "portal.war" ;
  product.ecmdemowar = "ecmdemo.war";
  product.codeRepo = "https://github.com/exodev/ecms" ;
  product.version = "${project.version}" ;
  product.serverPluginVersion = "${org.exoplatform.portal.version}" ;
  
  var kernel = Module.GetModule("kernel") ;
  var core = Module.GetModule("core") ;
  var ws = Module.GetModule("ws");
  var eXoJcr = Module.GetModule("jcr", {kernel : kernel, core : core, ws : ws}) ;
  var portal = Module.GetModule("portal", {kernel : kernel, ws:ws, core : core, eXoJcr : eXoJcr});         
  var wcm = Module.GetModule("wcm", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr, portal : portal});
  	
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
  
  product.addDependencies(wcm.gadgets);
  product.addDependencies(wcm.portlet.ecmadmin);
  product.addDependencies(wcm.portlet.ecmexplorer);
  product.addDependencies(wcm.web.eXoDMSResources) ;
  product.addDependencies(wcm.authoring.war);
  product.addDependencies(wcm.core.war);
  product.addDependencies(wcm.extension.war);
  product.addDependencies(wcm.waiextension.war);
  product.addDependencies(wcm.waitemplate.war);
  product.addDependencies(wcm.portlet.webpresentation);
  product.addDependencies(wcm.portlet.websearches); 
  product.addDependencies(wcm.portlet.seo);
  product.addDependencies(wcm.portlet.formgenerator);
  product.addDependencies(wcm.gadget.favorites);
  product.addDependencies(wcm.web.eXoWCMResources) ;
  product.addDependencies(wcm.web.eXoStaticResources) ;
  product.addDependencies(wcm.demo.portal);
  product.addDependencies(wcm.demo.rest);

  product.addServerPatch("tomcat", wcm.server.tomcat.patch) ;	
	
  product.module = wcm ;
  product.dependencyModule = [kernel, core, ws, eXoJcr, portal];

  return product ;
}