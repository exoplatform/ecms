eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
	var product = new Product();

	product.name = "eXoWorkflow" ;
	product.portalwar = "portal.war" ;
	product.codeRepo = "ecm/workflow" ;
	product.useWorkflow = true;
	product.workflowVersion = "${project.version}" ;
	product.serverPluginVersion = "${org.exoplatform.portal.version}" ;
	product.workflowJbpmVersion = "${org.jbpm.jbpm3}";
	product.workflowBonitaVersion = "${bonita.version}";

	var kernel = Module.GetModule("kernel") ;
	var core = Module.GetModule("core") ;
	var ws = Module.GetModule("ws");
	var eXoJcr = Module.GetModule("jcr", {kernel : kernel, core : core, ws : ws}) ;
	var portal = Module.GetModule("portal", {kernel : kernel, ws:ws, core : core, eXoJcr : eXoJcr });
	var workflow = Module.GetModule("workflow", {kernel : kernel, core : core, ws : ws, eXoJcr : eXoJcr, portal : portal});

	portal.starter = new Project("org.exoplatform.portal", "exo.portal.starter.war", "war", portal.version);
	portal.starter.deployName = "starter";
	product.addDependencies(portal.starter);  
	
	product.addDependencies(portal.web.rest);
	product.addDependencies(portal.portlet.exoadmin);
	product.addDependencies(portal.portlet.web);
	product.addDependencies(portal.web.portal);
	product.addDependencies(portal.portlet.dashboard);
	product.addDependencies(portal.eXoGadgetServer);
	product.addDependencies(portal.eXoGadgets);  
	product.addDependencies(portal.webui.portal);
	product.addDependencies(portal.web.eXoResources);

	product.addDependencies(workflow.web.eXoWorkflowResources);
	product.addDependencies(workflow.web.eXoStaticResources) ;
	product.addDependencies(workflow.portlet.workflow);
	product.addDependencies(workflow.extension.webapp);

	product.addServerPatch("jboss",  portal.server.jboss.patch) ;
	product.addServerPatch("jbossear",  portal.server.jbossear.patch) ;  
	
	product.module = workflow ;
	product.dependencyModule = [kernel, core, ws, eXoJcr, portal];

	return product ;
}
