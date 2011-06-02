Summary

    * Status: NewFolksonomyServiceImpl service should not save the SessionProvider as a local variable
    * CCP Issue: N/A, Product Jira Issue: ECMS-2203.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
The org.exoplatform.services.cms.folksonomy.impl.NewFolksonomyServiceImpl service stores a SessionProvider as a local variable, which is initialized during the service instanciation thanks to the SessionProviderService. So the same SessionProvider is used for all HTTP requests.

A SessionProvider is automatically created (and removed) for each HTTP request. It must be used.
So the SessionProvider should not be stored as a local variable but instead be retrieved at each call to the JCR with
?
sessionProviderService.getSystemSessionProvider(null)
Fix description

How is the problem fixed?

    * remove SessionProvider local variable, replace by using SessionProviderService to get SessionProvider.

Patch files:ECMS-2203.patch

Tests to perform

Reproduction test

    * see NewFolksonomyServiceImpl class, sessionProvider variable is stored as a local variable.

Tests performed at DevLevel

    * see NewFolksonomyServiceImpl class, sessionProvider local variable is removed and replaced by SessionProviderService.

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

    * Function or ClassName change : None

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
*

Support Comment
*

QA Feedbacks
*

