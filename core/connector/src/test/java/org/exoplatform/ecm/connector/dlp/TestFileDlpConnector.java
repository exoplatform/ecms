package org.exoplatform.ecm.connector.dlp;

import javax.jcr.Node;
import javax.jcr.Workspace;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.dlp.queue.QueueDlpService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.search.connector.FileSearchServiceConnector;
import org.junit.Test;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WCMCoreUtils.class)
@PowerMockIgnore({"javax.management.*","jdk.internal.reflect.*"})
public class TestFileDlpConnector {

  private FileDlpConnector     fileDlpConnector;
  
  @Mock
  private RepositoryService repositoryService;
  
  @Mock
  private IndexingService      indexingService;
  
  @Mock
  private FileSearchServiceConnector fileSearchServiceConnector;
  
  @Mock
  private QueueDlpService queueDlpService;
  
  @Test
  public void testProcessItemWhenIsIndexed() throws Exception {

    // Given
    InitParams initParams = new InitParams();
    ValueParam dlpKeywordsParam = new ValueParam();
    dlpKeywordsParam.setName("dlp.keywords");
    dlpKeywordsParam.setValue("keyword1,keyword2");
    initParams.addParameter(dlpKeywordsParam);
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("enable", "true");
    constructorParams.setProperty("displayName", "file");
    constructorParams.setProperty("type", "file");
    initParams.addParameter(constructorParams);
    String uuid = "123456789";
    String nodeName="nodeName";
    String path = "/folder1/folder2/"+nodeName;
  
    // When
    
    when(fileSearchServiceConnector.isIndexed(Mockito.any(),Mockito.eq(uuid))).thenReturn(true);
    
    List<SearchResult> results = new ArrayList<>();
    results.add(new SearchResult("url","title","excerpt","detail", "imageUrl",5,4));
    when(fileSearchServiceConnector.dlpSearch(Mockito.any(),Mockito.eq("keyword1 keyword2"),Mockito.eq(uuid))).thenReturn(results);
  
    fileDlpConnector = new FileDlpConnector(initParams, fileSearchServiceConnector, repositoryService, indexingService,queueDlpService);
    FileDlpConnector fileDlpConnectorSpy = Mockito.spy(fileDlpConnector);
  
    Workspace workspace = mock(Workspace.class);
    
    Node node = mock(Node.class);
    when(node.getName()).thenReturn(nodeName);
    when(node.getPath()).thenReturn(path);
    
    Node dlpSecurityNode=mock(Node.class);
    when(dlpSecurityNode.hasNode(nodeName)).thenReturn(false);
    
    ExtendedSession session = mock(ExtendedSession.class);
    when(session.getWorkspace()).thenReturn(workspace);
    when(session.getNodeByIdentifier(uuid)).thenReturn(node);
    when(session.getItem("/"+fileDlpConnector.DLP_SECURITY_FOLDER)).thenReturn(dlpSecurityNode);
    
    PowerMockito.mockStatic(WCMCoreUtils.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    when(sessionProvider.getSession(Mockito.any(), Mockito.any())).thenReturn(session);
    when(WCMCoreUtils.getSystemSessionProvider()).thenReturn(sessionProvider);
  
  
    fileDlpConnectorSpy.processItem(uuid);
  
    // Then
    Mockito.verify(fileDlpConnectorSpy,Mockito.times(1)).treatItem(Mockito.eq(uuid),Mockito.any());
    Mockito.verify(workspace, Mockito.times(1)).move(path,"/"+fileDlpConnector.DLP_SECURITY_FOLDER+"/"+nodeName);
    Mockito.verify(indexingService,Mockito.times(1)).unindex(fileDlpConnector.TYPE,uuid);
  }
  
  @Test
  public void testProcessItemWhenNotIndexed() throws Exception {
    
    // Given
    InitParams initParams = new InitParams();
    ValueParam dlpKeywordsParam = new ValueParam();
    dlpKeywordsParam.setName("dlp.keywords");
    dlpKeywordsParam.setValue("keyword1,keyword2");
    initParams.addParameter(dlpKeywordsParam);
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("enable", "true");
    constructorParams.setProperty("displayName", "file");
    constructorParams.setProperty("type", "file");
    initParams.addParameter(constructorParams);
    String uuid = "123456789";
    String nodeName="nodeName";
    String path = "/folder1/folder2/"+nodeName;
    
    // When
    when(fileSearchServiceConnector.isIndexed(Mockito.any(),Mockito.eq(uuid))).thenReturn(false);
    
    fileDlpConnector = new FileDlpConnector(initParams, fileSearchServiceConnector, repositoryService, indexingService,queueDlpService);
    FileDlpConnector fileDlpConnectorSpy = Mockito.spy(fileDlpConnector);
 
    Workspace workspace = mock(Workspace.class);
    
    Node node = mock(Node.class);
    when(node.getName()).thenReturn(nodeName);
    when(node.getPath()).thenReturn(path);
    
    Node dlpSecurityNode=mock(Node.class);
    when(dlpSecurityNode.hasNode(nodeName)).thenReturn(false);
    
    ExtendedSession session = mock(ExtendedSession.class);
    when(session.getWorkspace()).thenReturn(workspace);
    when(session.getNodeByIdentifier(uuid)).thenReturn(node);
    when(session.getItem("/"+fileDlpConnector.DLP_SECURITY_FOLDER)).thenReturn(dlpSecurityNode);
    
    PowerMockito.mockStatic(WCMCoreUtils.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    when(sessionProvider.getSession(Mockito.any(), Mockito.any())).thenReturn(session);
    when(WCMCoreUtils.getSystemSessionProvider()).thenReturn(sessionProvider);
    
    
    fileDlpConnectorSpy.processItem(uuid);
    
    // Then
    Mockito.verify(fileDlpConnectorSpy,Mockito.times(0)).treatItem(Mockito.eq(uuid), Mockito.any());
  }
  
  @Test
  public void testGetDetectedKeywords() throws Exception {
    // Given
    InitParams initParams = new InitParams();
    ValueParam dlpKeywordsParam = new ValueParam();
    dlpKeywordsParam.setName("dlp.keywords");
    dlpKeywordsParam.setValue("keyword1,keyword2");
    initParams.addParameter(dlpKeywordsParam);
    PropertiesParam constructorParams = new PropertiesParam();
    constructorParams.setName("constructor.params");
    constructorParams.setProperty("enable", "true");
    constructorParams.setProperty("displayName", "file");
    constructorParams.setProperty("type", "file");
    initParams.addParameter(constructorParams);
    fileDlpConnector = new FileDlpConnector(initParams, fileSearchServiceConnector, repositoryService, indexingService, queueDlpService);
    Method getDetectedKeywords = fileDlpConnector.getClass().getDeclaredMethod("getDetectedKeywords", Collection.class, String.class);
    getDetectedKeywords.setAccessible(true);

    // When
    String dlpKeywords = "one two three";
    ArrayList<SearchResult> searchResults = new ArrayList<>();
    searchResults.add(new SearchResult("url", "title", "excerpt", "detail", "imageUrl", 5, 4));

    Map<String, List<String>> excerpts = new HashMap<>();
    List<String> strings = new ArrayList<>();
    strings.add("<em>one</em> <em>one</em>");

    List<String> strings1 = new ArrayList<>();
    strings1.add("<em>one</em> <em>two</em>");
    excerpts.put("test", strings);
    excerpts.put("test1", strings1);
    searchResults.get(0).setExcerpts(excerpts);

    // then
    String result = (String) getDetectedKeywords.invoke(fileDlpConnector, searchResults, dlpKeywords);
    assertEquals("one, two", result);
    
    // when
    List<String> strings2 = new ArrayList<>();
    strings2.add("<em>one</em> <em>two</em> <em>two</em> <em>thRee</em>");  
    
    List<String> strings3 = new ArrayList<>();
    strings3.add("<em>Three</em> <em>Three</em> <em>threes</em>");

    excerpts.put("test", strings2);
    excerpts.put("test1", strings3);
    searchResults.get(0).setExcerpts(excerpts);

    // then
    String result1 = (String) getDetectedKeywords.invoke(fileDlpConnector, searchResults, dlpKeywords);
    assertEquals("one, two, three", result1);
  }
}
