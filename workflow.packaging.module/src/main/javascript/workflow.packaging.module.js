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
  module.relativeMavenRepo =  "org/exoplatform/ecm/workflow";
  module.relativeSRCRepo =  "ecm/workflow";
  module.name =  "workflow";
  module.portlet = {}
  module.portlet.workflow = new Workflow("",module.version).getPortlet();

module.web = {}
  module.web.eXoWorkflowResources = 
    new Project("org.exoplatform.ecm.workflow", "exo.ecm.workflow.web.eXoWorkflowResources", "war", module.version) ;  
  module.web.eXoWorkflowResources.deployName = "eXoWorkflowResources" ;
  
	module.extension = {};
  module.extension.webapp = new Project("org.exoplatform.ecm.workflow.extension", "exo.ecm.workflow.extension.webapp", "war", module.version).
		addDependency(new Project("org.exoplatform.ecm.workflow.extension", "exo.ecm.workflow.extension.config", "jar", module.version));
  module.extension.webapp.deployName = "ecmworkflow-extension";	

	module.demo = {};
   // demo portal
   module.demo.portal = 
	   new Project("org.exoplatform.ecm.workflow.demo", "exo.ecm.workflow.demo.webapp", "war", module.version).
	   addDependency(new Project("org.exoplatform.ecm.workflow.demo", "exo.ecm.workflow.demo.config", "jar", module.version));
	   module.demo.portal.deployName = "ecmworkflowdemo";  
	   	
  module.server = {};

   module.server.tomcat = {};
   module.server.tomcat.patch = new Project("org.exoplatform.ecm.workflow", "exo.ecm.workflow.server.tomcat.patch", "jar", module.version);
   module.server.tomcatdemo = {}
   module.server.tomcatdemo.patch = new Project("org.exoplatform.ecm.workflow", "exo.ecm.workflow.server.tomcat-demo.patch", "jar", module.version);
  return module;
}
