Summary
	
	* Status: PERF : String.split removal
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2488.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Remove String.split in NodeLocation class which is responsible of 4% cpu total usage in WCM

Fix description
How is the problem fixed?
	* Write and use new private method split in NodeLocation instead of String.split

Patch information:
	Patch file: ECMS-2488.patch


Tests to perform

Reproduction test
	* Using String.split in NodeLocation object is responsible of 4% cpu total usage in WCM.

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

h2. Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Is there a performance risk/cost?
	* No

Function or ClassName change : No


Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
