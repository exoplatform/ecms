Summary

    * Status: The name of taxonomy is limited to 30 characters
    * CCP Issue: CCP-621, Product Jira Issue: ECMS-1538.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

When adding a new taxonomy under a tree of taxonomies, its name is limited to 30 characters.
The customer is waiting to increase this limit.

Fix description

How is the problem fixed?

    * The number of characters can be updated in configuration file and gotten by the TaxonomyService.
    * The default length of category to 150 characters.
    * Update the checking condition length of category name when user creates a new category.

Patch file: ECMS-1538.patch

Tests to perform

Reproduction test

    * Login ECMS as root
    * Select "Administration" from "Group" menu
    * Click on Edit icon of a Category tree
    * At "Edit Category Tree" window, click on Add icon to add a Category
    * Enter "Category name" with the length > 30 characters
    * System warning "The name too long" -> not OK

Tests performed at DevLevel

    * cf above

Tests performed at QA/Support Level

    * cf above

Documentation changes

Documentation changes:

    * Added in the patch, add the description of getCategoryNameLength method in Reference guide.

Configuration changes

Configuration changes:

    * Add length limit as init-param in dms-taxonomies-configuration.xml

Will previous configuration continue to work?

    * No

Risks and impacts

Can this bug fix have any side effects on current client projects?
Function or ClassName change: no

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* Patch validated.

Support Comment
* Patch validated

QA Feedbacks
*
