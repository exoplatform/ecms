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
  module.relativeMavenRepo =  "org/exoplatform/ecms" ;
  module.relativeSRCRepo =  "ecms" ;
  module.name =  "dms" ;
    
  module.portlet = {}
  module.portlet.ecmadmin = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-administration", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)) .     
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-publication", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo.ecm.dms.core.component.document.viewer", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-connector", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo.ecm.dms.core.webui.dms", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo.ecm.dms.core.webui.ext", "jar", module.version)).
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
  module.portlet.ecmadmin.deployName = "ecmadmin";
  
  module.portlet.ecmexplorer = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-explorer", "exo-portlet", module.version);
  module.portlet.ecmexplorer.deployName = "ecmexplorer";

  module.portlet.ecmbrowsecontent = new Project("org.exoplatform.ecms", "exo-ecms-ext-deprecated-portlet-browsecontent", "exo-portlet", module.version);
  module.portlet.ecmbrowsecontent.deployName = "ecmbrowsecontent";

  module.portlet.jcr_console = new Project("org.exoplatform.ecms", "exo.ecm.dms.core.portlet.jcr-console", "exo-portlet", module.version).
    addDependency(new Project("exo-weblogic", "exo-weblogic-authproviders", "jar", "1.0")).
    addDependency(new Project("exo-weblogic", "exo-weblogic-loginmodule", "jar", "1.0")).  
    addDependency(new Project("commons-logging", "commons-logging", "jar", "1.0.4"));
  
  module.gadgets = new Project("org.exoplatform.ecms", "exo.ecm.dms.core.gadgets", "war", module.version).
    addDependency(ws.frameworks.json);  
  module.gadgets.deployName = "eXoDMSGadgets";
  
  module.application = {}
  
  module.application.rest = new Project("org.exoplatform.ecms", "exo-ecms-core-publication","jar", module.version).
    addDependency(ws.frameworks.json);
  
  module.web = {}
  
  module.web.eXoDMSResources = new Project("org.exoplatform.ecms", "exo.ecm.dms.core.web.eXoDMSResources", "war", module.version) ;  
  module.web.eXoDMSResources.deployName = "eXoDMSResources" ;
    
  return module;
}
