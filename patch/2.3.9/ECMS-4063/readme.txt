Summary

    * Issue title: Document keeps locked after editing it 
    * CCP Issue: N/A
    * Product Jira Issue: ECMS-4063.
    * Complexity: N/A

Proposal


Problem description

What is the problem to fix?

    * Document keeps locked after editing it 

Fix description

Problem analysis

    * The root cause is the eviction and the invalidation of JMX of the entries of the cache org.exoplatform.services.jcr.impl.core.lock.LockManagerImp
    * We should avoid to use an eXo Cache instance to manage the lock tokens or should use an internal implementation of an eXo cache to ensure that no eviction algorithm is set and that it cannot be cleared using JMX.

How is the problem fixed?

    * We create an internal memory Map to store all lock-tokens inside instead of eXo Cache.
    * The Locking behavior, it is nearly the same as before
   1. User edit document -> Document locked -> Save -> Release lock
   2. User use lock function to lock node \-> Handled by Unlock function or it will be unlocked automatically when session is destroyed (Logged out)
   3. In the case nodes were locked by any reason (e.g.: data corrupted while editing documents and nodes are still locked): Superuser or any member belongs to granted group (pre-defined: *:/platform/administrator) will be allowed to unlock the locked nodes on Site Explorer or go to ECM Admin application to unlock them.
   4. In the case server crashes or restarts then all the locked nodes will be released during server startup.(Just a bit different as before)


Tests to perform

Reproduction test

    * Add gatein.cache.default.livetime=30 into configuration.properties ($CATALINA_HOME/gatein/conf/configuration.properties or $JBOSS_HOME/server/default/conf/gatein/configuration.properties)
    * Create new article
    * Save and close it
    * Edit this document
    * Wait for 30s then Save it
      -> Document is still locked and cannot unlock it anymore.

Tests performed at DevLevel

    * Cf. above

Tests performed at Support Level

    * Cf. above

Tests performed at QA

    * 

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
* N/A

Configuration changes

Configuration changes:
* No

Will previous configuration continue to work?
* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: 
    * Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment

    * Validated

Support Comment

    * Validated

QA Feedbacks

    * 
