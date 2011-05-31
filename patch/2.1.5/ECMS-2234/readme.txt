Summary

    * Status: Date time format should be with correct time zone.
    * CCP Issue: N/A, Product Jira Issue: ECMS-2234.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * The server on which the xCMIS is deployed is in GMT+1 but I have these kinds of date in xCMIS

      <cmis:value>2011-04-29T11:15:31.590Z</cmis:value>

      The date should be in that case 2011-04-29T11:15:31.590+01:00 or 2011-04-29T10:15:31.590Z

Fix description

How is the problem fixed?

    * Modified BaseJcrStorage#createJcrDate(Calendar c)
    *  
Patch files:ECMS-2234.patch

Tests to perform

Reproduction test
* StorageTest

QueryUsecasesTest#testSearchOnDate()

Tests performed at DevLevel

* StorageTest

QueryUsecasesTest#testSearchOnDate()

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* no
Configuration changes

Configuration changes:
* no

Will previous configuration continue to work?
* yes
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * no

Is there a performance risk/cost?
* no
Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

