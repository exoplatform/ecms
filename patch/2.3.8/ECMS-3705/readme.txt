Summary
	* Issue title webdav doesn't affect the publication cyclce and version history.
	* CCP Issue: 
	* Product Jira Issue: ECMS-3705.
	* Complexity: N/A

Proposal

Problem description

What is the problem to fix?
	*Updating content using webdav protocol does not affect the publication cyclce and does not control version.

Fix description

Problem analysis
	* The webdav service parameter is not defined to update version after each modification. 

How is the problem fixed?
	* Change the webdav service parameter to update version after modification. Parameter name is "auto-version"

Tests to perform

Reproduction test
	1. Start default profile and login as john
	2. Add html file "test1.html" in classic site (note: If new file is added by CE, the file extension is required)
	3. Type in the content section: test 1, save and publish it
	4. Go to the public site, edit page layout, add a content detail anywhere, point to test1 content. Now the test1 content must be visible in the published classic site.
	5. Open the webdav to the recently added file, example: webdav://localhost:8080/rest-ecmdemo/private/jcr/repository/collaboration/sites content/live/classic/web contents
	6. Edit the html content and replace the text to "test 2".
	7. Save it
	8. Go to site explorer, select the test1 content and open the "Manage publication" dialog, it doesn't display the webdav modified version, the state of document is published
    
	Problem: the document state should be modified to Draft.

Tests performed at DevLevel
	* c.f above

Tests performed at Support Level
	* c.f above

Tests performed at QA
	* n/a

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* N/A

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No

Configuration changes

Configuration changes:
	* Webdav configuration parameters

Will previous configuration continue to work?
	* No

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: 
	* Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	* 
