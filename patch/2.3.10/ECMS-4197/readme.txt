Summary
	* Issue title: Performance issue relate to org.exoplatform.services.wcm.navigation.NavigationUtils.getNavigationAsJSON need to be fixed 
	* CCP Issue:  N/A
	* Product Jira Issue: ECMS-4197 ECMS-4102 ECMS-4120.
	* Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?
	* Performance issue relates to org.exoplatform.services.wcm.navigation.NavigationUtils.getNavigationAsJSON need to be fixed 

Fix description

Problem analysis
	* To get a user portal Navigation, we can use function getNavigation from UserPortal object. It requires to call all navigations of a portal to get user navigation.

How is the problem fixed?
	* ECMS-4197 provides 2 fixes but incomplete. ECMS-4102 ECMS-4120 are 2 its regression.
		- Instead of using a ThreadLocal boolean variable, ECMS has used a HashMap of Navigation Data to mark if Navigation Data is loaded.
        	- Get directly userNavigation by calling its constructor
	* For feature 1: It causes ECMS-4102.
        	- Navigation in ECMS, it's SCV. It uses eXo.env.portal.navigations variable to render Navigation
		- If a page containing at least a SCV is loaded the first time. In server side, navigation is marked as loaded and  eXo.env.portal.navigations in SCV portlet is set correct value.
		- Once this page is reloaded, browser resets eXo.env.portal.navigations to null. We expect this value will get correct navigation from server side. But server still marks navigation data as loaded. Therefore, eXo.env.portal.navigations is still null.
		- This regression is fixed by reseting Navigation Data in server side after reloading a page.
	*For feature 2: It causes ECMS-4120.
		- Constructor of UserNavigation in ECMS makes a hardcoded on modifiable variable which is used to store user's edit permission in user navigation.
		- This regression is fixed by indicating correct user's edit permission on constructor of UserNavigation class.

Tests to perform

Reproduction test
	*  By profiling the PLF with large data, there are 2 problems:
		1. org.exoplatform.services.wcm.navigation.NavigationUtils.getNavigationAsJSON which depends on org.exoplatform.services.organization.idm.GroupDAOImpl.getAllGroups took 3,752 seconds for just 2 invocations.
    		2. Hotspots found for org.exoplatform.services.wcm.extensions.scheduler.impl.ChangeStateCronJobImpl which call org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager$DataRequest.await 7,838,860 times and consumes 133,833 seconds

Tests performed at DevLevel
	* Functional test
	
Tests performed at Support Level
	* Functional test

Tests performed at QA
	* Performance test

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
	* No

Changes in Selenium scripts 
	* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Any change in API (name, signature, annotation of a class/method)?: No 
	* Data (template, node type) migration/upgrade: No 

Is there a performance risk/cost?
	* Performance is improved

