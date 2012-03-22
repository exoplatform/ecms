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
  var xcmisVersion = "${org.xcmis.version}";
  var abderaVersion = "${abdera.version}";
  var luceneRegexVersion = "${lucene-regex.version}";
  var axiomVersion = "${axiom.version}";
  var jaxenVersion = "${jaxen.version}";
  var antlrVersion = "${org.antlr.version}";
  var commonsVersion = "${org.exoplatform.commons.version}";
  var antisamyVersion = "${org.owasp.antisamy.version}";
  var batikVersion = "${org.apache.batik.version}";
  var batikUtilVersion = "${org.apache.batik-util.version}";
  var sacVersion = "${org.w3c.sac.version}";
  module.relativeMavenRepo =  "org/exoplatform/ecms" ;
  module.relativeSRCRepo =  "ecms" ;
  module.name =  "wcm" ;
  
  module.portlet = {};
  
  module.portlet.webpresentation = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-presentation", "exo-portlet", module.version).       
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-connector", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-publication", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-publication-plugins", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-authoring-services", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-webui", "jar",  module.version)).
    addDependency(ws.frameworks.json).
    addDependency(jcr.frameworks.command).
    addDependency(portal.webui.portal);
  module.portlet.webpresentation.deployName = "presentation";
    
  module.portlet.websearches = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-search", "exo-portlet", module.version).    
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-search", "jar",  module.version));
  module.portlet.websearches.deployName = "searches";

  module.portlet.newsletter = new Project("org.exoplatform.ecms", "exo-ecms-ext-newsletter-portlet", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-newsletter-services", "jar",  module.version));
  module.portlet.newsletter.deployName = "newsletter";

  module.portlet.formgenerator = new Project("org.exoplatform.ecms", "exo-ecms-ext-formgenerator-portlet", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version));
  module.portlet.formgenerator.deployName = "formgenerator";

	module.portlet.seo = new Project("org.exoplatform.ecms", "exo-ecms-apps-portlet-seo", "exo-portlet", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-webui-seo", "jar",  module.version)).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-core-services", "jar",  module.version));
  module.portlet.seo.deployName = "seo";      

  module.gadget = {};
  module.gadget.favorites = new Project("org.exoplatform.ecms", "exo-ecms-apps-gadget-favorites", "war", module.version);    
  module.gadget.favorites.deployName = "favorites";

  module.web = {};
  
  module.web.eXoWCMResources = new Project("org.exoplatform.ecms", "exo-ecms-apps-resources-wcm", "war", module.version).
    addDependency(portal.web.eXoResources);
  module.web.eXoWCMResources.deployName = "eXoWCMResources";
    
  module.web.eXoStaticResources = new Project("org.exoplatform.ecms", "exo-ecms-apps-resources-static", "war", module.version);
  module.web.eXoStaticResources.deployName = "eXoStaticResources";

  module.authoring = {};
  module.authoring.war = new Project("org.exoplatform.ecms", "exo-ecms-ext-authoring-apps", "war", module.version).
  addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-authoring-webui", "jar", module.version));
  module.authoring.war.deployName = "authoring-apps";	      
  
  
  module.core = {};
  module.core.war = new Project("org.exoplatform.ecms", "exo-ecms-core-webapp", "war", module.version);
  module.core.war.deployName = "ecm-wcm-core";	      	    

  module.extension = {};
  
  module.extension.war = 
    new Project("org.exoplatform.ecms", "exo-ecms-packaging-wcm-webapp", "war", module.version).
    // xCMIS dependencies
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-ext-xcmis-sp", "jar", module.version)).
    addDependency(new Project("org.xcmis", "xcmis-spi", "jar", xcmisVersion)).
    addDependency(new Project("org.xcmis", "xcmis-renditions", "jar", xcmisVersion)).
    addDependency(new Project("org.xcmis", "xcmis-restatom", "jar", xcmisVersion)).
    addDependency(new Project("org.xcmis", "xcmis-search-model", "jar", xcmisVersion)).
    addDependency(new Project("org.xcmis", "xcmis-search-parser-cmis", "jar", xcmisVersion)).
    addDependency(new Project("org.xcmis", "xcmis-search-service", "jar", xcmisVersion)).
    addDependency(new Project("org.apache.abdera", "abdera-client", "jar", abderaVersion)).
    addDependency(new Project("org.apache.abdera", "abdera-core", "jar", abderaVersion)).
    addDependency(new Project("org.apache.abdera", "abdera-i18n", "jar", abderaVersion)).
    addDependency(new Project("org.apache.abdera", "abdera-parser", "jar", abderaVersion)).
    addDependency(new Project("org.apache.abdera", "abdera-server", "jar", abderaVersion)).
    addDependency(new Project("org.apache.lucene", "lucene-regex", "jar", luceneRegexVersion)).
    addDependency(new Project("org.apache.ws.commons.axiom", "axiom-api", "jar", axiomVersion)).
    addDependency(new Project("org.apache.ws.commons.axiom", "axiom-impl", "jar", axiomVersion)).
    addDependency(new Project("jaxen", "jaxen", "jar", jaxenVersion)).
    addDependency(new Project("org.antlr", "antlr-runtime", "jar", antlrVersion)).
		addDependency(new Project("org.owasp", "antisamy", "jar", antisamyVersion)).
		addDependency(new Project("org.apache.xmlgraphics", "batik-css", "jar", batikVersion)).
		addDependency(new Project("batik", "batik-util", "jar", batikUtilVersion)).
		addDependency(new Project("org.w3c", "sac", "jar", sacVersion)).
    addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.component.product", "jar", commonsVersion)).
    addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.component.upgrade", "jar", commonsVersion));
  
  module.extension.war.deployName = "ecm-wcm-extension";	      	    

  module.waiextension = {};
  module.waiextension.war = 
    new Project("org.exoplatform.ecms", "exo-ecms-packaging-waiportal-webapp", "war", module.version);
  module.waiextension.war.deployName = "ecm-waiportal-extension";

  module.waitemplate = {};
  module.waitemplate.war = 
    new Project("org.exoplatform.ecms", "exo-ecms-apps-wai-template", "war", module.version);
  module.waitemplate.war.deployName = "ecm-template-waiportal";

  module.demo = {};
  
  module.demo.portal = new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-config", "jar", module.version));
  module.demo.portal.deployName = "ecmdemo";	      
    
  module.demo.rest = new Project("org.exoplatform.ecms", "exo-ecms-packaging-ecmdemo-rest-webapp", "war", module.version);
  module.demo.rest.deployName = "rest-ecmdemo";		

  module.server = {};
  module.server.tomcat = {};
  module.server.tomcat.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wcm-server-tomcat", "jar", module.version);
	
  module.server.jbossear = {};
  module.server.jbossear.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wcm-server-jboss-ear", "jar", module.version);

  module.server.jboss = {};
  module.server.jboss.patch = new Project("org.exoplatform.ecms", "exo-ecms-delivery-wcm-server-jboss", "jar", module.version);	
    
  return module;
}
