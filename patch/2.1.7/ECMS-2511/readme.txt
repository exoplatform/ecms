Summary
	* Status: Linked documents aren't correctly rendered in using exo:article view template in Content Detail portlet
	* CCP Issue: CCP-1031, Product Jira Issue: ECMS-2511.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Linked documents aren't correctly rendered in using exo:article view template in Content Detail portlet

Fix description

How is the problem fixed?
	* Add the getViewableLink() function which can get the viewable link of relation node

Patch information:
	* Patch files: ECMS-2511.patch

Tests to perform

Reproduction test
	# Log into the ecmdemo portal.
	# Go to siteExplorer.
	# Create a new article.
	# Save as draft
	# Add a related link to our article : using "Add Relation" from Manage Relations
	# Click on 'Manage' and published the content.
	# Go to "acme" site
	# Display the created article in "Content Detail portlet" => the related links are not clickable : code source displayed and if there is no related documents, label link is still displayed

Tests performed at DevLevel
	* Do the same step as reproduce => go to the detail page anh show detail of the document

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	*No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
