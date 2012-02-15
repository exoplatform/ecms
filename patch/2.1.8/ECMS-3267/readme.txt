Summary
	* Status: Portlet crashes and no preview available after saving from Office 2010 in webdav
	* CCP Issue: CCP-1182, Product Jira Issue: ECMS-3267.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Portlet crashes and no preview available after saving from Office 2010 in webdav

Fix description

How is the problem fixed?
	* Do not render node if extension and mime-type aren't matched.

Patch information:
	Patch files: ECMS-3267.patch

Tests to perform

Reproduction test
	* Create a webdav drive on windows 7 by using this command net use o: "http://localhost:8080/rest/private/jcr/repository/collaboration/"
	* Copy any doc or DOCX created by MS Office 2010 to this drive
	* Access to the document in the Site Explorer => The icon associated to the Mime type of Docx is the icon of nt:file
	* Open the document in the webdav drive and make a modification (even one character) then save it.
	* Access again to Site Explorer
		=> The mime type and the icon of the document have changed to XML.
		=> The portlet crashes and it is difficult to get the interface back.

Tests performed at DevLevel
	* cf Above

Tests performed at QA/Support Level
	*
	
Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* No
	
Function or ClassName change	
	* No
	
Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*

