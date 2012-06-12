Summary
	* Issue title XSS vulnerability in Documents feature
  	* CCP Issue:  N/A
    	* Product Jira Issue: ECMS-3853.
    	* Complexity: N/A

Proposal

Problem description

What is the problem to fix?
	* XSS problems in Content Explorer and Administration portlets

Fix description

Problem analysis
	* When we displaying the content that contain javascript code. It can causes the XSS problems. So to avoid this problem, we need make sure that the content is escaped html tag before displaying it.

How is the problem fixed?
	* Escape html tags for the content before displaying it.
	* As we need change view templates. So to migrate them, We just use TemplateMigrationPlugin which has already provided by ECMS. NodeTypeTemplateUpgradePlugin will be used to upgrade node type templates. For this case, we only want to migrate template for 3 nodetypes (AccessibleMedia, ElementSet and File). So we need add remaining node types into unchanged-nodetype-templates.

Tests to perform

Reproduction test
	1. Upload a document
	2. Open it the edit its metadata
	3. Click "Edit" button, in title field type:   
		<script>alert('XSS attack ' + document.cookie)</script>
	4. Save and exit
	5. Type in quick search field some words from uploaded file to find it and script will executed:

Tests performed at DevLevel
	* cf.above

Tests performed at Support Level
	* cf.above

Tests performed at QA
	* cf.above

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* No

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Data (template, node type) migration/upgrade
	* 3 nodetypes (AccessibleMedia, ElementSet and File)

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	* 

