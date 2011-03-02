package org.exoplatform.ecm;

public class ProductVersions {

  public final static String WCM_2_0_0 = "WCM-2.0.0";
  public final static String WCM_2_1_0 = "WCM-2.1.0";
  public final static String WCM_2_1_2 = "WCM-2.1.2";
  public final static String WCM_2_1_3 = "WCM-2.1.3";

  public final static int WCM_2_0_0_NUM = 200;
  public final static int WCM_2_1_0_NUM = 210;
  public final static int WCM_2_1_2_NUM = 212;
  public final static int WCM_2_1_3_NUM = 213;

  public static String getCurrentVersion() {
    return WCM_2_1_3;
  }

  public static int getCurrentVersionAsInt() {
    return WCM_2_1_3_NUM;
  }

}
