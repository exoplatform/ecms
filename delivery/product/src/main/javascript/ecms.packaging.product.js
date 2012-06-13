eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
  var product = new Product();
  
  product.name = "eXoECMS" ;
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
  var ecms = Module.GetModule("ecms", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr, portal : portal});
  	
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

  product.addDependencies(ecms.gadgets);
  product.addDependencies(ecms.portlet.ecmadmin);
  product.addDependencies(ecms.portlet.ecmexplorer);
  product.addDependencies(ecms.authoring.war);
  product.addDependencies(ecms.core.war);
  product.addDependencies(ecms.extension.war);
  product.addDependencies(ecms.waiextension.war);
  product.addDependencies(ecms.waitemplate.war);
  product.addDependencies(ecms.portlet.webpresentation);
  product.addDependencies(ecms.portlet.websearches); 
  product.addDependencies(ecms.portlet.seo);
  product.addDependencies(ecms.portlet.fastcontentcreator);
  product.addDependencies(ecms.gadget.favorites);
  product.addDependencies(ecms.web.eXoWCMResources) ;
  product.addDependencies(ecms.demo.portal);
  product.addDependencies(ecms.demo.rest);

  product.addServerPatch("tomcat", ecms.server.tomcat.patch) ;	
	
  product.module = ecms ;
  product.dependencyModule = [kernel, core, ws, eXoJcr, portal];

  return product ;
}
