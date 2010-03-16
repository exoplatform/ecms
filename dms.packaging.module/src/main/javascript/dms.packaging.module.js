eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getModule(params) {

  var kernel = params.kernel;
  var core = params.core;
  var ws = params.ws;
  var jcr = params.eXoJcr;
  var portal = params.portal;  
  var module = new Module();
  
  module.version = "${project.version}" ;
  module.relativeMavenRepo =  "org/exoplatform/ecm/dms" ;
  module.relativeSRCRepo =  "ecm/dms" ;
  module.name =  "dms" ;
    
  module.portlet = {}
  module.portlet.dms = 
    new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.portlet.ecm.core.web", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.component.cms", "jar",  module.version)) .     
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.component.deployment", "jar",  module.version)) .    
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.component.publication", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.component.document.viewer", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.connector.fckeditor", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.webui.dms", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.webui.ext", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.portlet.ecm.ext.config", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.server.tomcat.patch", "jar", module.version)).
    addDependency(new Project("org.exoplatform", "exo-jcr-services", "jar", "1.12.0-Beta01")).
    addDependency(new Project("rome", "rome", "jar", "0.9")) .
    addDependency(new Project("com.totsp.feedpod", "itunes-com-podcast", "jar", "0.2")) .
    addDependency(new Project("ical4j", "ical4j", "jar", "0.9.20")) .
    addDependency(new Project("jdom", "jdom", "jar", "1.0")).
    addDependency(new Project("org.apache.ws.commons", "ws-commons-util", "jar", "1.0.1")).
    addDependency(new Project("com.sun.xml.stream", "sjsxp", "jar", "1.0")).
    addDependency(new Project("org.icepdf", "icepdf-core", "jar", "3.0")).
    addDependency(new Project("org.icepdf", "icepdf-viewer", "jar", "3.0")).
		addDependency(new Project("org.fontbox", "fontbox", "jar", "0.1.0"));
    
    module.portlet.dms.deployName = "ecm";
  
  module.portlet.jcr_console = 
    new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.portlet.jcr-console", "exo-portlet", module.version).
    addDependency(new Project("exo-weblogic", "exo-weblogic-authproviders", "jar", "1.0")).
	  addDependency(new Project("exo-weblogic", "exo-weblogic-loginmodule", "jar", "1.0")).  
	  addDependency(new Project("commons-logging", "commons-logging", "jar", "1.0.4"));
  
  module.gadgets = 
    new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.gadgets", "war", module.version).
    addDependency(ws.frameworks.json);  
    module.gadgets.deployName = "eXoDMSGadgets";
  
  module.application = {}
  module.application.rest = new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.component.publication","jar", module.version).
  	addDependency(ws.frameworks.json);
  
  module.web = {}
  module.web.eXoDMSResources = 
    new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.web.eXoDMSResources", "war", module.version) ;  
  module.web.eXoDMSResources.deployName = "eXoDMSResources" ;
	
	module.web.fck = 
    new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.web.fck", "war", module.version) ;  
  module.web.fck.deployName = "fck" ;
      
	module.extension = {};
  module.extension.webapp = new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.extension.webapp", "war", module.version).
		addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.extension.config", "jar", module.version));
  module.extension.webapp.deployName = "dms-extension";	

	module.demo = {};
   // demo portal
   module.demo.portal = 
	   new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.demo.webapp", "war", module.version).
	   addDependency(new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.demo.config", "jar", module.version));
	   module.demo.portal.deployName = "dmsdemo";  
	   
   // demo rest endpoint	   
   module.demo.rest = 
       new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.demo.rest-war", "war", module.version);
       module.demo.rest.deployName = "rest-dmsdemo"; 
       
	
  module.server = {}

   module.server.tomcat = {}
   module.server.tomcat.patch = new Project("org.exoplatform.ecm.dms.core", "exo.ecm.dms.core.server.tomcat.patch", "jar", module.version);

  return module;
}
