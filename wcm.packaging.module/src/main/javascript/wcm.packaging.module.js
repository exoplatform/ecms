eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getModule(params) {

  var kernel = params.kernel;
  var core = params.core;
  var ws = params.ws;
  var jcr = params.eXoJcr;
  var portal = params.portal;
  var dms = params.dms;
  
  var module = new Module();

  module.version = "${project.version}" ;
  module.relativeMavenRepo =  "org/exoplatform/ecm/wcm" ;
  module.relativeSRCRepo =  "ecm/wcm" ;
  module.name =  "wcm" ;
  
  module.portlet = {};
  
  module.portlet.webpresentation = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-presentation", "exo-portlet", module.version).       
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-connector", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-publication", "jar",  module.version)).    
    addDependency(ws.frameworks.json).
    addDependency(jcr.frameworks.command).
    addDependency(jcr.frameworks.web).
    addDependency(portal.webui.portal);
    
  module.portlet.websearches = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-search", "exo-portlet", module.version).    
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-search", "jar",  module.version));
    
  module.portlet.newsletter = new Project("org.exoplatform.ecms", "exo-ecms-ext-newsletter-portlet", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-newsletter-services", "jar",  module.version));
    
  module.portlet.formgenerator = new Project("org.exoplatform.ecms", "exo-ecms-ext-formgenerator-portlet", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version));

  module.web = {};
  
  module.web.eXoWCMResources = new Project("org.exoplatform.ecms", "exo-ecms-apps-resources-wcm", "war", module.version).
    addDependency(portal.web.eXoResources);
  
  module.extension = {};
  
  module.extension.war = new Project("org.exoplatform.ecms", "exo-ecms-packaging-wcm-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-packaging-wcm-config", "jar", module.version));
  module.extension.war.deployName = "ecm-wcm-extension";	      

  module.demo = {};
  
  module.demo.portal = new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-config", "jar", module.version));
  module.demo.portal.deployName = "ecmdemo";	      
    
  module.demo.rest = new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-rest-webapp", "war", module.version);
  module.demo.rest.deployName = "rest-ecmdemo";	      

  module.server = {};
  module.server.tomcat = {};
  module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wcm-server-tomcat", "jar", module.version);
   
  return module;
}
