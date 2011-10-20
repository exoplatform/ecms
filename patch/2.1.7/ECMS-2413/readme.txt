Summary
	* Status: Bug in UIWorkingWorkspace.gtmpl when HTTP session timeout set 1 minute
	* CCP Issue: CCP-1004, Product Jira Issue: ECMS-2413.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Bug in UIWorkingWorkspace.gtmpl when HTTP session timeout set 1 minute

Fix description

How is the problem fixed?
	* Check if and ONLY if the session.getAttribute("DefaultMaxInactiveInterval") return NOT-NULL then call session.setMaxInactiveInterval(interval)

Patch information:
	* Patch files: ECMS-2413.patch

Tests to perform

Reproduction test
	Make the change in \gatein-eppspdemo.ear\ecmdemo.war\WEB-INF\web.xml. Change the value of 15 to 1

		<session-config>
		<session-timeout>1</session-timeout>
		</session-config>

	Error on accessing pages in auhtenticated context (/ecmdemo/private/*)

Tests performed at DevLevel
	Make the change in \gatein-eppspdemo.ear\ecmdemo.war\WEB-INF\web.xml. Change the value of 15 to 1

		<session-config>
		<session-timeout>1</session-timeout>
		</session-config>

	No Error on accessing pages in auhtenticated context (/ecmdemo/private/*)

Tests performed at QA/Support Level


Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* YES

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
