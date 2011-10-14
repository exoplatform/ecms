Summary
	* Status: Content based on custom node type doesn't appear in any "Content List" portlet
	* CCP Issue: CCP-1082, Product Jira Issue: ECMS-2687.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Content based on custom node type doesn't appear in any "Content List" portlet

Fix description

How is the problem fixed?
	* Update list of existed template in system when a new template is created.

Patch information:
	* ECMS-2687.patch

Tests to perform

Reproduction test
	* Login as root
	* Click on Administration tab [http://localhost:8080/ecmdemo/private/classic/wcmAdmin]
	* Create a new namespace named "zzz"
	* Add new note type named notetype1 with zzz as namespace and property definitions: zzz:titulo
	* Create a new template and add new notetype "notetype1" with name as "Teste do nodetype1"
	* Click on the "Content Explorer" tab > Create a new content folder "test_nodetype" in "/classic/web contents"
	* Add the path: "/classic/web contents/test_nodetype"
	* Create 2 contents on this folder in using "Teste do nodetype1" template and publish it
	* Go to the site classic and add a CLV portlet to Overview Page and choose /classic/web contents/test_nodetype as Folder Patch
	* It will show the Overview page again. The two contents should be showed in the "content list", but we see the "Sorry, no articles available" message

Tests performed at DevLevel
	* c/f above

Tests performed at QA/Support Level
	* c/f above

Documentation changes

Documentation changes:
	* Yes

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Function or ClassName change
	* core/publication/src/main/java/org/exoplatform/services/wcm/publication/WCMComposerImpl.java
	* core/publication/src/main/java/org/exoplatform/services/wcm/publication/WCMComposer.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
