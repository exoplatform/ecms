package org.exoplatform.services.cms.lock.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.picocontainer.Startable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestLockService  extends BaseWCMTestCase {
  
  private LockService lockService_;
  
  List<LockGroupsOrUsersPlugin> originLockGroupsOrUsersPlugins_;
  
  @SuppressWarnings("unchecked")
  @Override
  protected void afterContainerStart() {
    super.afterContainerStart();
    lockService_ = (LockService)container.getComponentInstanceOfType(LockService.class);
    try {
      Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
      field.setAccessible(true);
      originLockGroupsOrUsersPlugins_ = (List<LockGroupsOrUsersPlugin>)field.get(lockService_);
    } catch (Exception e) {}
  }

  @BeforeMethod
  public void setUp() throws Exception {
    applySystemSession();
  }
  
  @Test
  public void testStartNormal() throws Exception {
    try {
      LockGroupsOrUsersConfig config1 = new LockGroupsOrUsersConfig();
      List<String> lockList1 = new ArrayList<String>();
      lockList1.add("*:/platform/administrators");
      lockList1.add("*:/platform/powers");
      config1.setSettingLockList(lockList1);
      InitParams params = new InitParams();
      ObjectParameter objParam = new ObjectParameter();
      objParam.setObject(config1);
      params.addParameter(objParam);
      LockGroupsOrUsersPlugin plugin1 = new LockGroupsOrUsersPlugin(params);
      List<LockGroupsOrUsersPlugin> lockGroupsOrUsersPlugins = new ArrayList<LockGroupsOrUsersPlugin>();
      lockGroupsOrUsersPlugins.add(plugin1);
      Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
      field.setAccessible(true);
      field.set(lockService_, lockGroupsOrUsersPlugins);
      ((LockServiceImpl)lockService_).addLockGroupsOrUsersPlugin(new AddNodeTypePlugin(new InitParams()));
  
      ((Startable)lockService_).start();
    } catch (Exception e) {
      fail();
    } finally {
      ((Startable)lockService_).stop();
    }
  }
  
  @Test
  public void testStartAbNormal() throws Exception {
    try {
      LockGroupsOrUsersConfig config1 = new LockGroupsOrUsersConfig();
      List<String> lockList1 = new ArrayList<String>();
      lockList1.add("*:/platform/administrators");
      lockList1.add("*:/platform/powers");
      config1.setSettingLockList(lockList1);
      InitParams params = new InitParams();
      ObjectParameter objParam1 = new ObjectParameter();
      objParam1.setObject(config1);
      params.addParameter(objParam1);
      ObjectParameter objParam2 = new ObjectParameter();
      objParam2.setObject("xxx");
      params.addParameter(objParam2);
      LockGroupsOrUsersPlugin plugin1 = new LockGroupsOrUsersPlugin(params);
      List<LockGroupsOrUsersPlugin> lockGroupsOrUsersPlugins = new ArrayList<LockGroupsOrUsersPlugin>();
      lockGroupsOrUsersPlugins.add(plugin1);
      Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
      field.setAccessible(true);
      field.set(lockService_, lockGroupsOrUsersPlugins);
  
      ((Startable)lockService_).start();
    } catch (Exception e) {
      fail();
    }
  }
  
  @Test
  public void testStartAbNormalWithNoLog() throws Exception {
    try {
      LockGroupsOrUsersConfig config1 = new LockGroupsOrUsersConfig();
      List<String> lockList1 = new ArrayList<String>();
      lockList1.add("*:/platform/administrators");
      lockList1.add("*:/platform/powers");
      config1.setSettingLockList(lockList1);
      InitParams params = new InitParams();
      ObjectParameter objParam1 = new ObjectParameter();
      objParam1.setObject(config1);
      params.addParameter(objParam1);
      ObjectParameter objParam2 = new ObjectParameter();
      objParam2.setObject("xxx");
      params.addParameter(objParam2);
      LockGroupsOrUsersPlugin plugin1 = new LockGroupsOrUsersPlugin(params);
      List<LockGroupsOrUsersPlugin> lockGroupsOrUsersPlugins = new ArrayList<LockGroupsOrUsersPlugin>();
      lockGroupsOrUsersPlugins.add(plugin1);
      Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
      field.setAccessible(true);
      field.set(lockService_, lockGroupsOrUsersPlugins);
      // Mock Log
      Log mockLOG = mock(org.exoplatform.services.log.impl.SLF4JExoLog.class);
      when(mockLOG.isErrorEnabled()).thenReturn(false);
      setFinalStatic(LockServiceImpl.class.getDeclaredField("LOG"), mockLOG);
  
      ((Startable)lockService_).start();
    } catch (Exception e) {
      fail();
    }
  }
  
  @Test
  public void testStartAbNormalWithPluginNoParam() throws Exception {
    try {
      InitParams params = new InitParams();
      LockGroupsOrUsersPlugin plugin1 = new LockGroupsOrUsersPlugin(params);
      List<LockGroupsOrUsersPlugin> lockGroupsOrUsersPlugins = new ArrayList<LockGroupsOrUsersPlugin>();
      lockGroupsOrUsersPlugins.add(plugin1);
      Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
      field.setAccessible(true);
      field.set(lockService_, lockGroupsOrUsersPlugins);
      // Mock Log
      Log mockLOG = mock(org.exoplatform.services.log.impl.SLF4JExoLog.class);
      when(mockLOG.isErrorEnabled()).thenReturn(false);
      setFinalStatic(LockServiceImpl.class.getDeclaredField("LOG"), mockLOG);
      
      ((Startable)lockService_).start();
    } catch (Exception e) {
      fail();
    }
  }
  
  @Test
  public void testAddGroupsOrUsersForLock() throws Exception {
    try {
      lockService_.addGroupsOrUsersForLock("*:/platform/powers");
      lockService_.addGroupsOrUsersForLock("*:/platform/powers");
      lockService_.addGroupsOrUsersForLock("*:/platform/suppers");

      assertEquals(1, lockService_.getPreSettingLockList().size());
      assertEquals(3, lockService_.getAllGroupsOrUsersForLock().size());
      assertTrue(lockService_.getPreSettingLockList().contains("*:/platform/administrators"));
    } catch (Exception e) {
      fail();
    }
  }
  
  @Test
  public void testRemoveGroupsOrUsersForLock() throws Exception {
    try {
      lockService_.addGroupsOrUsersForLock("*:/platform/powers");
      lockService_.addGroupsOrUsersForLock("*:/platform/suppers");
      lockService_.removeGroupsOrUsersForLock("*:/platform/administrators"); // remove existing group
      lockService_.removeGroupsOrUsersForLock("*:/platform/administrators1"); // remove not-existing group

      assertEquals(1, lockService_.getPreSettingLockList().size());
      assertEquals(2, lockService_.getAllGroupsOrUsersForLock().size());
      assertTrue(lockService_.getPreSettingLockList().contains("*:/platform/administrators"));
    } catch (Exception e) {
      fail();
    }
  }
  
  static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
 }
  
  @AfterMethod
  public void tearDown() throws Exception {
    Field field = LockServiceImpl.class.getDeclaredField("lockGroupsOrUsersPlugin_");
    field.setAccessible(true);
    field.set(lockService_, originLockGroupsOrUsersPlugins_);
    setFinalStatic(LockServiceImpl.class.getDeclaredField("LOG"), ExoLogger.getLogger(LockServiceImpl.class.getName()));
  }
}
