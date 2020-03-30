package org.exoplatform.services.wcm.extensions.scheduler.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.cms.queries.impl.NewUserConfig;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.value.DateValue;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.quartz.*;

import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = { ChangeStateCronJobImpl.class, SessionProvider.class, WCMCoreUtils.class })
public class TestChangeStateCronJob {

  @Test
  public void testChangeStateOnContentInTrash() throws Exception {
    PowerMockito.mockStatic(ManagementFactory.class, new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (StringUtils.equals(methodName, "getRuntimeMXBean")) {
          RuntimeMXBean mxBean = Mockito.mock(RuntimeMXBean.class);
          Mockito.when(mxBean.getUptime()).thenReturn(150000L);
          return mxBean;
        }
        return invocation.callRealMethod();
      }
    });

    Session session = Mockito.mock(Session.class);
    PublicationPlugin publicationPlugin = Mockito.mock(PublicationPlugin.class);

    PowerMockito.mockStatic(SessionProvider.class, new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (StringUtils.equals(methodName, "createSystemProvider")) {
          SessionProvider sessionProvider = Mockito.mock(SessionProvider.class);
          Mockito.when(sessionProvider.getSession(Mockito.anyString(), Mockito.any())).thenReturn(session);

          QueryManager queryManager = Mockito.mock(QueryManager.class);
          Workspace workspace = Mockito.mock(Workspace.class);
          Mockito.when(session.getWorkspace()).thenReturn(workspace);
          Mockito.when(session.getWorkspace().getQueryManager()).thenReturn(queryManager);
          Query query = Mockito.mock(Query.class);
          Mockito.when(queryManager.createQuery(Mockito.anyString(), Mockito.eq(Query.SQL))).thenReturn(query);
          QueryResult queryResult = Mockito.mock(QueryResult.class);
          Mockito.when(query.execute()).thenReturn(queryResult);

          Property dateProperty = Mockito.mock(Property.class);
          Mockito.when(dateProperty.getDate()).thenReturn(Calendar.getInstance());

          //Create Node Iterator
          Node nodeInTrash = mock(Node.class);
          when(nodeInTrash.getPath()).thenReturn("/Trash/nodeInTrash");
          when (nodeInTrash.getProperty(Mockito.eq("publication:startPublishedDate"))).thenReturn(dateProperty);

          Node nodeUnderJCRSystem = mock(Node.class);
          when(nodeUnderJCRSystem.getPath()).thenReturn("/jcr:system/nodeUnderJCRSystem");
          when(nodeUnderJCRSystem.getProperty(Mockito.eq("publication:startPublishedDate"))).thenReturn(dateProperty);

          Node nodeToPublish = mock(Node.class);
          when(nodeToPublish.getPath()).thenReturn("/sites/sample/nodeToPublish");
          when (nodeToPublish.getProperty(Mockito.eq("publication:startPublishedDate"))).thenReturn(dateProperty);

          Node nodeToPublishWithoutStartDate = mock(Node.class);
          when(nodeToPublishWithoutStartDate.getPath()).thenReturn("/sites/sample/nodeToPublishWithoutStartDate");
          when (nodeToPublishWithoutStartDate.getProperty(Mockito.eq("publication:startPublishedDate"))).thenReturn(null);
          when (nodeToPublishWithoutStartDate.getProperty(Mockito.eq("publication:endPublishedDate"))).thenReturn(dateProperty);

          Node nodeToPublishWithoutEndDate = mock(Node.class);
          when(nodeToPublishWithoutEndDate.getPath()).thenReturn("/sites/sample/nodeToPublishWithoutEndDate");
          when (nodeToPublishWithoutEndDate.getProperty(Mockito.eq("publication:startPublishedDate"))).thenReturn(dateProperty);
          when (nodeToPublishWithoutEndDate.getProperty(Mockito.eq("publication:endPublishedDate"))).thenReturn(null);

          NodeIterator nodeIterator = Mockito.mock(NodeIterator.class);
          Boolean [] hasNextReturns = new Boolean[] {true, true, true, false, true, true, false};
          Node[] nextReturnedNodes = new Node[] {nodeInTrash, nodeUnderJCRSystem, nodeToPublish, nodeToPublishWithoutStartDate, nodeToPublishWithoutEndDate};
          Mockito.when(nodeIterator.hasNext()).thenReturn(true,hasNextReturns);
          Mockito.when(nodeIterator.nextNode()).thenReturn(nodeInTrash,nextReturnedNodes);


          Mockito.when(queryResult.getNodes()).thenReturn(nodeIterator);
          return sessionProvider;
        }
        return invocation.callRealMethod();
      }
    });

    PowerMockito.mockStatic(WCMCoreUtils.class, new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (StringUtils.equals(methodName, "getService") && invocation.getArgumentAt(0, Class.class) != null) {
          Class<?> serviceClass = invocation.getArgumentAt(0, Class.class);
          if (serviceClass.equals(TrashService.class)) {
            return new TrashService() {
              @Override
              public String moveToTrash(Node node, SessionProvider sessionProvider) throws Exception {
                return null;
              }

              @Override
              public String moveToTrash(Node node, SessionProvider sessionProvider, int deep) throws Exception {
                return null;
              }

              @Override
              public void restoreFromTrash(String trashNodePath, SessionProvider sessionProvider) throws Exception {

              }

              @Override
              public List<Node> getAllNodeInTrash(SessionProvider sessionProvider) throws Exception {
                return null;
              }

              @Override
              public List<Node> getAllNodeInTrashByUser(SessionProvider sessionProvider, String userName) throws Exception {
                return null;
              }

              @Override
              public void removeRelations(Node node, SessionProvider sessionProvider) throws Exception {

              }

              @Override
              public boolean isInTrash(Node node) throws RepositoryException {
                return node.getPath().startsWith("/Trash") && !node.getPath().equals("/Trash");
              }

              @Override
              public Node getTrashHomeNode() {
                return null;
              }

              @Override
              public Node getNodeByTrashId(String trashId) throws InvalidQueryException, RepositoryException {
                return null;
              }
            };
          }
          if (serviceClass.equals(PublicationService.class)) {
            PublicationService publicationService = Mockito.mock(PublicationService.class);
            Map<String,PublicationPlugin> publicationPlugins = new HashMap<>();
            publicationPlugins.put(AuthoringPublicationConstant.LIFECYCLE_NAME, publicationPlugin);
            Mockito.when(publicationService.getPublicationPlugins()).thenReturn(publicationPlugins);

            return publicationService;
          }
          if (serviceClass.equals(RepositoryService.class)) {
            RepositoryService repositoryService = Mockito.mock(RepositoryService.class);
            ManageableRepository manageableRepository = mock(ManageableRepository.class);
            when(repositoryService.getCurrentRepository()).thenReturn(manageableRepository);
            return repositoryService;
          }
        }
        return invocation.callRealMethod();
      }
    });


    JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
    JobDetail jobDetail = Mockito.mock(JobDetail.class);
    JobDataMap jobDataMap = Mockito.mock(JobDataMap.class);

    Mockito.when(context.getJobDetail()).thenReturn(jobDetail);
    Mockito.when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
    Mockito.when(jobDataMap.getString(Mockito.eq("fromState"))).thenReturn("staged");
    Mockito.when(jobDataMap.getString(Mockito.eq("toState"))).thenReturn("published");
    Mockito.when(jobDataMap.getString(Mockito.eq("predefinedPath"))).thenReturn("collaboration:/");

    ChangeStateCronJobImpl cronJobImpl = new ChangeStateCronJobImpl();
    cronJobImpl.execute(context);
    Mockito.verify(session, Mockito.times(0)).save();
    Mockito.verify(publicationPlugin, Mockito.times(1)).
            changeState(Mockito.any(), Mockito.anyString(), Matchers.<HashMap<String, String>>any());
  }
}
