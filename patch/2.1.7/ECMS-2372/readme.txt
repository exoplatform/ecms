Summary
   	* Status: Display "%20" when creating new document in some templates
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2372.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Display "%20" when creating new document in some templates

Fix description

How is the problem fixed?
	* Auto remove leading and trailing whitespace of input fields
	* When displaying content name, we will useText.unescapeIllegalJcrChars(name) to unescapes previously escaped jcr chars.

Patch files: ECMS-2372.patch

Reproduction test
	* Login
	* Go to Site Explorer > Site Management
	* Create new document in File Plan template with space characters at heading and/or tailing in name.
    	* Fill all mandatory fields with appropriate date.
    	* Save as Draft then Close
    	* Show new document, file name is very bad display

Tests performed at DevLevel
	* cf above

Tests performed at QA/Support Level
	* cf above

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
	* Yes

Function or ClassName change
	* No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

