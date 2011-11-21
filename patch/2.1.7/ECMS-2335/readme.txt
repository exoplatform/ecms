Summary
	* Status: Unknown error when selecting the 'workflow' publication workflow
	* CCP Issue: CCP-1066, Product Jira Issue: ECMS-2335.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Unknown error when selecting the 'workflow' publication workflow

Fix description

How is the problem fixed?
	* Remove the usage of unregistered property exo:move in WorkflowPublicationPlugin class

Patch files: ECMS-2335.patch

Tests to perform

Reproduction test

	* Login as john
    	* Open the sites explorer app
    	* Open this node: /acme/web contents/site artifacts/contact_form_confirmation or any node with no publication status
    	* Open the manage publications window, and select the first option: workflow => Unknown error and the following exception appear:

Tests performed at DevLevel
	* cf Above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* Add to release note the following workaround to fix problem in workflow mode in some servers
	     Copy file ecmworkflow-extension.xml into folder tomcat/conf/Catalina/localhost

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

	* Run ECMS with workflow, in some computers(some but not all), there might be the exception
    		GRAVE: Unexpected error
    			javax.jcr.nodetype.NoSuchNodeTypeException: Nodetype not found (mixin) exo:publishLocation
            		at org.exoplatform.services.jcr.impl.core.NodeImpl.canAddMixin(NodeImpl.java:332)
	In this case, use the following workaround : copy file ecmworkflow-extension.xml into folder tomcat/conf/Catalina/localhost

Function or ClassName change
    * No
Is there a performance risk/cost?
    * No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

