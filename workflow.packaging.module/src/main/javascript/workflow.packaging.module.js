eXo.require("eXo.projects.Module");
eXo.require("eXo.projects.Product");
eXo.require("eXo.projects.Workflow");

function getModule(params) {

  var kernel = params.kernel;
  var core = params.core;
  var eXoPortletContainer = params.eXoPortletContainer;
  var ws = params.ws;
  var jcr = params.eXoJcr;
  var portal = params.portal;  
  var module = new Module();
  
  module.version = "${project.version}" ;
  module.relativeMavenRepo =  "org/exoplatform/ecms";
  module.relativeSRCRepo =  "ecm/workflow";
  module.name =  "workflow";
  module.portlet = {}
  module.portlet.workflow = new Workflow("",module.version).getPortlet();

	module.web = {}
  module.web.eXoWorkflowResources = 
    new Project("org.exoplatform.ecms", "exo-ecms-ext-workflow-resources", "war", module.version) ;  
  module.web.eXoWorkflowResources.deployName = "eXoWorkflowResources" ;
  
	module.extension = {};
  module.extension.webapp = new Project("org.exoplatform.ecms", "exo-ecms-packaging-workflow-webapp", "war", module.version).
		addDependency(new Project("org.exoplatform.ecms", "exo-ecms-packaging-workflow-config", "jar", module.version));
  module.extension.webapp.deployName = "ecmworkflow-extension";		
	   	
  module.server = {};

   module.server.tomcat = {};
   module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wkf-wcm-server-tomcat", "jar", module.version);
   
  return module;
}
