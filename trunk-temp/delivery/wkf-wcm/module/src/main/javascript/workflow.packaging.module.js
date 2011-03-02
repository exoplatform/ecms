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
  module.portlet = {};
	
	var workflow = new Workflow("",module.version, module.name);
  module.portlet.workflow = workflow.getPortlet();

	module.web = {}
  module.web.eXoWorkflowResources = 
    new Project("org.exoplatform.ecms", "exo-ecms-ext-workflow-resources", "war", module.version) ;  
  module.web.eXoWorkflowResources.deployName = "eXoWorkflowResources" ;

  module.web.eXoStaticResources = new Project("org.exoplatform.ecms", "exo-ecms-apps-resources-static", "war", module.version);
  module.web.eXoStaticResources.deployName = "eXoStaticResources";	
  
	module.extension = {};
  module.extension.webapp = new Project("org.exoplatform.ecms", "exo-ecms-packaging-workflow-webapp", "war", module.version).
		addDependency(new Project("org.exoplatform.ecms", "exo-ecms-packaging-workflow-config", "jar", module.version));
  module.extension.webapp.deployName = "ecmworkflow-extension";		
	   	
  module.server = {};

   module.server.tomcat = {};
   module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wkf-wcm-server-tomcat", "jar", module.version);
	 
	 module.server.jboss = {};
   module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wkf-wcm-server-jboss", "jar", module.version);
	 
	 module.server.jbossear = {};
   module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wkf-wcm-server-jboss-ear", "jar", module.version);
   
  return module;
}
