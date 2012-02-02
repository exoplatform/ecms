Summary
	* Status: Setting a name to taxonomyAction causes NullPointerException in PostCreateNodeTypeEventListener.onEvent()
	* CCP Issue: CCP-1150, Product Jira Issue: ECMS-3057.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Getting action taxonomyAction of a node bases on getting fixed id name taxonomyAction

Fix description

How is the problem fixed?
	* Getting taxonomyAction by using node type exo:taxonomyAction instead of fixed name taxonomyAction

Patch information:

Patch files: ECMS-3057.patch

Tests to perform

Reproduction test
	1. Go to 'Administration' -> 'Manage Categories' -> Click 'Add Taxonomy Tree'
	2. Fill the fields and proceed to 'Add an action to the taxonomy tree'
	3. Set Name to 'test' and fill other fields and its permission
	4. Click 'Save' and 'Next'
	5. The Taxonomy Tree will be created successfully
	6. Go to 'Form Builder'
	7. Fill 'Name' and 'Description'
	8. Click 'Save' -> You will see NullPointerException.

Tests performed at DevLevel
	* c/f above

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

    Function or ClassName change
	*  core/publication/src/main/java/org/exoplatform/services/wcm/publication/listener/post/PostCreateNodeTypeEventListener.java

Is there a performance risk/cost?
	*  No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

