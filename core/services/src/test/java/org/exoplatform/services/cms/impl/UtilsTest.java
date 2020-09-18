package org.exoplatform.services.cms.impl;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

  public void testCleanName() {
    // Given
    String title1 = "test | test";
    String title2 = "test % test";
    String title3 = "test & test";
    String title4 = "test . test";
    String title5 = "test $ test";
    String title6 = "test / test";
    String title7 = "test [ test";
    String title8 = "test ] test";
    String title9 = "test : test";
    String title10 = "test ; test";
    String title11 = "test \\ test";
    String title12 = "test \t test";
    String title13 = "test \" test";
    String title14 = "test \n test";
    String title15 = "test \r test";
    String title16 = "test > test";
    String title17 = "test < test";
    String title18 = "test # test";
    String title19 = "test * test";
    String title20 = "test @ test";

    // When
    String titleClean1 = Utils.cleanName(title1);
    String titleClean2 = Utils.cleanName(title2);
    String titleClean3 = Utils.cleanName(title3);
    String titleClean4 = Utils.cleanName(title4);
    String titleClean5 = Utils.cleanName(title5);
    String titleClean6 = Utils.cleanName(title6);
    String titleClean7 = Utils.cleanName(title7);
    String titleClean8 = Utils.cleanName(title8);
    String titleClean9 = Utils.cleanName(title9);
    String titleClean10 = Utils.cleanName(title10);
    String titleClean11 = Utils.cleanName(title11);
    String titleClean12 = Utils.cleanName(title12);
    String titleClean13 = Utils.cleanName(title13);
    String titleClean14 = Utils.cleanName(title14);
    String titleClean15 = Utils.cleanName(title15);
    String titleClean16 = Utils.cleanName(title16);
    String titleClean17 = Utils.cleanName(title17);
    String titleClean18 = Utils.cleanName(title18);
    String titleClean19 = Utils.cleanName(title19);
    String titleClean20 = Utils.cleanName(title20);

    // Then
    assertEquals(titleClean20,"test-test");
    assertEquals(titleClean1,"test-test");
    assertEquals(titleClean2,"test-test");
    assertEquals(titleClean3,"test-test");
    assertEquals(titleClean4,"test-test");
    assertEquals(titleClean5,"test-test");
    assertEquals(titleClean6,"test-test");
    assertEquals(titleClean7,"test-test");
    assertEquals(titleClean8,"test-test");
    assertEquals(titleClean9,"test-test");
    assertEquals(titleClean10,"test-test");
    assertEquals(titleClean11,"test-test");
    assertEquals(titleClean12,"test-test");
    assertEquals(titleClean13,"test-test");
    assertEquals(titleClean14,"test-test");
    assertEquals(titleClean15,"test-test");
    assertEquals(titleClean16,"test-test");
    assertEquals(titleClean17,"test-test");
    assertEquals(titleClean18,"test-test");
    assertEquals(titleClean19,"test-test");
  }
}