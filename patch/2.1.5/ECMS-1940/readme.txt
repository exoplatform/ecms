Summary

Status: Unknown error when trying to save html document
CCP Issue: CCP-846, Product Jira Issue: ECMS-1940.
Complexity: Hard
The Proposal

Problem description
What is the problem to fix?

How to reproduce:
login as root
click on content manager
click on drive acme/ acme news 1/ default.html
click on edit document
change something
click on save draft
click on close
->node reflects the change ->Ok

sp is running on default HSQL database

18:10:32,809 ERROR [portal:UIPortalApplication] Error during the processAction phase
javax.jcr.ItemExistsException: [collaboration] ADD PROPERTY. Item already exists. Condition: parent ID, name, index. []:1[]sites content:1[]live:1[]acme:1[]web contents:1[]News:1[]News1:1[]default.html:1http://www.jcp.org/jcr/1.0lockOwner:1, ID: f029625e0a2201f22fb41e6202afb969, ParentID: 39977cd27f0000010018d531d78b4342. Cause >>>> Violation of unique index JCR_IDX_SITEM_PARENT: duplicate value(s) for column(s) CONTAINER_NAME,PARENT_ID,NAME,I_INDEX,I_CLASS,VERSION in statement [insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,2,?,?,?)]: Violation of unique index JCR_IDX_SITEM_PARENT: duplicate value(s) for column(s) CONTAINER_NAME,PARENT_ID,NAME,I_INDEX,I_CLASS,VERSION in statement [insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,2,?,?,?)]: [collaboration] ADD PROPERTY. Item already exists. Condition: parent ID, name, index. []:1[]sites content:1[]live:1[]acme:1[]web contents:1[]News:1[]News1:1[]default.html:1http://www.jcp.org/jcr/1.0lockOwner:1, ID: f029625e0a2201f22fb41e6202afb969, ParentID: 39977cd27f0000010018d531d78b4342. Cause >>>> Violation of unique index JCR_IDX_SITEM_PARENT: duplicate value(s) for column(s) CONTAINER_NAME,PARENT_ID,NAME,I_INDEX,I_CLASS,VERSION in statement [insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,2,?,?,?)]: Violation of unique index JCR_IDX_SITEM_PARENT: duplicate value(s) for column(s) CONTAINER_NAME,PARENT_ID,NAME,I_INDEX,I_CLASS,VERSION in statement [insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,2,?,?,?)]: Violation of unique index JCR_IDX_SITEM_PARENT: duplicate value(s) for column(s) CONTAINER_NAME,PARENT_ID,NAME,I_INDEX,I_CLASS,VERSION in statement [insert into JCR_SITEM(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, P_TYPE, P_MULTIVALUED) VALUES(?,?,?,?,?,2,?,?,?)]
at org.exoplatform.services.jcr.impl.dataflow.persistent.TxIsolatedOperation.perform(TxIsolatedOperation.java:227)
at org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager.save(CacheableWorkspaceDataManager.java:526)
at org.exoplatform.services.jcr.impl.dataflow.persistent.ACLInheritanceSupportedWorkspaceDataManager.save(ACLInheritanceSupportedWorkspaceDataManager.java:225)
at org.exoplatform.services.jcr.impl.dataflow.persistent.VersionableWorkspaceDataManager.save(VersionableWorkspaceDataManager.java:244)
at org.exoplatform.services.jcr.impl.dataflow.session.TransactionableDataManager.save(TransactionableDataManager.java:366)
at org.exoplatform.services.jcr.impl.core.NodeImpl.lock(NodeImpl.java:1564)

Fix description
How is the problem fixed?

remove redundant session.logout() commands to avoid losing changed data.

Patch files: ECMS-1940

Tests to perform
Reproduction test

Tests performed at DevLevel
login as root
click on content manager
click on drive acme/ acme news 1/ default.html
click on edit document
change something
click on save draft

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

Function or ClassName change: No
Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*
