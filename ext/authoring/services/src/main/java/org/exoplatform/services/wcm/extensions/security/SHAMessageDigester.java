package org.exoplatform.services.wcm.extensions.security;

import java.security.MessageDigest;
/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class SHAMessageDigester {
    public static String getHash(String message) throws Exception {
  MessageDigest msgDigest = MessageDigest.getInstance("SHA-1");
  msgDigest.update(message.getBytes());
  byte[] aMessageDigest = msgDigest.digest();
  StringBuffer ticket = new StringBuffer();
  String tmp = null;
  for (int i = 0; i < aMessageDigest.length; i++) {
      tmp = Integer.toHexString(0xFF & aMessageDigest[i]);
      if (tmp.length() == 2) {
    ticket.append(tmp);
      } else {
    ticket.append("0");
    ticket.append(tmp);
      }
  }
  return ticket.toString();
    }

}
