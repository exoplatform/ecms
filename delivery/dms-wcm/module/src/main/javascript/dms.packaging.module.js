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
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui-administration", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)) .     
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-publication", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-viewer", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-connector", "jar", module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui", "jar", module.version)).
    addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.webui.ext", "jar", "${org.exoplatform.commons.version}")).
    addDependency(new Project("org.exoplatform", "exo-jcr-services", "jar", "${org.exoplatform.jcr-services.version}")).
    addDependency(new Project("rome", "rome", "jar", "${rome.version}")) .
    addDependency(new Project("com.totsp.feedpod", "itunes-com-podcast", "jar", "${itunes.podcast.version}")) .
    addDependency(new Project("org.icepdf", "icepdf-core", "jar", "${icepdf.version}")).
    addDependency(new Project("org.icepdf", "icepdf-viewer", "jar", "${icepdf.version}")).
    addDependency(new Project("org.artofsolving.jodconverter", "jodconverter-core", "jar", "${jodconverter-core.version}")).
    addDependency(new Project("org.openoffice", "ridl", "jar", "${openoffice.version}")).
    addDependency(new Project("org.openoffice", "unoil", "jar", "${openoffice.version}")).
    addDependency(new Project("org.openoffice", "jurt", "jar", "${openoffice.version}")).
    addDependency(new Project("org.openoffice", "juh", "jar", "${openoffice.version}"));
  module.portlet.ecmadmin.deployName = "ecmadmin";
  
  module.portlet.ecmexplorer = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-explorer", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui-explorer", "jar", module.version));
  module.portlet.ecmexplorer.deployName = "ecmexplorer";

  module.portlet.ecmbrowsecontent = new Project("org.exoplatform.ecms", "exo-ecms-ext-deprecated-portlet-browsecontent", "exo-portlet", module.version);
  module.portlet.ecmbrowsecontent.deployName = "ecmbrowsecontent";

  module.portlet.jcr_console = new Project("org.exoplatform.ecms", "exo-ecms-ext-deprecated-portlet-jcrconsole", "exo-portlet", module.version);
  module.portlet.jcr_console.deployName = "jcr_console";
  
  module.gadgets = new Project("org.exoplatform.ecms", "exo-ecms-apps-gadget-publication", "war", module.version).
    addDependency(ws.frameworks.json);  
  module.gadgets.deployName = "eXoDMSGadgets";
  
  module.application = {}
  
  module.application.rest = new Project("org.exoplatform.ecms", "exo-ecms-core-publication","jar", module.version).
    addDependency(ws.frameworks.json);
  
  module.web = {}
  
  module.web.eXoDMSResources = new Project("org.exoplatform.ecms", "exo-ecms-apps-resources-dms", "war", module.version) ;  
  module.web.eXoDMSResources.deployName = "eXoDMSResources" ;
    
  return module;
}
