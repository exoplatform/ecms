Summary
	* Status: Error when reading a pdf file uploaded in using webdav
	* CCP Issue: N/A, Product Jira Issue: ECMS-2322.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Error when reading a pdf file uploaded in using webdav

Fix description

How is the problem fixed?
	* Add a Listener which adds automatically the property mix:referenceable. This Listener is triggered on adding new document.

Patch information:
	* Patch files: ECMS-2322.patch

Tests to perform

Reproduction test
	1. Create webdav driver
	2. Upload a pdf file in using this driver
	3. Login on browser
	4. Error on rendering this uploaded file in *Sites Explorer*

Tests performed at DevLevel
	* cf. above

Tests performed at QA/Support Level
	* cf. above

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/jcr-component-plugins-configuration.xml

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Function or ClassName change
	* Add new file core/services/src/main/java/org/exoplatform/services/cms/jcrext/AddFileDocumentAction.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
