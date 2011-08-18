Summary

	* Status: PERF : getMemberships using Identity
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2491.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* In order to avoid a lot of access to LDAP in LDAP environment, get the current user groups and memberships from his Identity

Fix description

How is the problem fixed?
	* Get memberships and groups of the current user having the given id using the IdentityRegistry service instead of the Organization service to allow JAAS based authorization

Patch information:
	Patch file: ECMS-2491.patch

Tests to perform

Reproduction test
	*

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

h2. Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

h2. Risks and impacts

Can this bug fix have any side effects on current client projects?
	* No

Function or ClassName change: 
	* core/webui/src/main/java/org/exoplatform/ecm/webui/utils/Utils.java

Is there a performance risk/cost?
	* No


Validation (PM/Support/QA)

PM Comment
	* Patch validated

Support Comment
	*

QA Feedbacks
	*
