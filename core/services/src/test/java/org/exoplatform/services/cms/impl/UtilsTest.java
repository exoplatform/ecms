package org.exoplatform.services.cms.impl;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {

  public void testCleanName() {
    // Given
    String title1 = "test|test";
    String title3 = "test&test";
    String title4 = "test@test";
    String title5 = "test$test";
    String title6 = "test/test";
    String title7 = "test[test";
    String title8 = "test]test";
    String title9 = "test:test";
    String title10 = "test;test";
    String title11 = "test\\test";
    String title12 = "test\ttest";
    String title13 = "test\"test";
    String title14 = "test\ntest";
    String title15 = "test\rtest";
    String title16 = "test>test";
    String title17 = "test<test";
    String title18 = "test#test";
    String title19 = "test*test";
    String title20 = "test.test";
    String title21 = "test.test.pdf";

    // When
    String titleClean1 = Utils.cleanName(title1);
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
    String titleClean21 = Utils.cleanName(title21);

    // Then
    assertEquals(titleClean1,"test_test");
    assertEquals(titleClean3,"test_test");
    assertEquals(titleClean4,"test_test");
    assertEquals(titleClean5,"test_test");
    assertEquals(titleClean6,"test_test");
    assertEquals(titleClean7,"test_test");
    assertEquals(titleClean8,"test_test");
    assertEquals(titleClean9,"test_test");
    assertEquals(titleClean10,"test_test");
    assertEquals(titleClean11,"test_test");
    assertEquals(titleClean12,"test_test");
    assertEquals(titleClean13,"test_test");
    assertEquals(titleClean14,"test_test");
    assertEquals(titleClean15,"test_test");
    assertEquals(titleClean16,"test_test");
    assertEquals(titleClean17,"test_test");
    assertEquals(titleClean18,"test_test");
    assertEquals(titleClean19,"test_test");
    assertEquals(titleClean20,"test.test");
    assertEquals(titleClean21,"test_test.pdf");
  }
}
