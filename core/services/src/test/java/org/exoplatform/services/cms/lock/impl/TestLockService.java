package org.exoplatform.services.cms.lock.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.impl.AddNodeTypePlugin;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.picocontainer.Startable;

public class TestLockService  extends BaseWCMTestCase {

  private LockService lockService_;

  public void setUp() throws Exception {
    super.setUp();
    lockService_ = (LockService)container.getComponentInstanceOfType(LockService.class);
    applySystemSession();
  }

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

  public void tearDown() throws Exception {
    ((Startable)lockService_).stop();
    ((Startable)lockService_).start();
    super.tearDown();
  }
}
