IMPORTANT NOTE : Please insure you're using the latest maven settings to build a branch.
http://svn.exoplatform.org/exo-int/software-factory/reference/maven/settings.xml

To Build the product from the branch, add -Pexo-staging,jboss-staging :
# mvn clean install -Pexo-staging,jboss-staging

To Build ECMS with all profiles :
# mvn clean install -Pwcm,wkf,dms


