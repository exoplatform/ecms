/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.GregorianCalendar ;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;

import javax.jcr.Node;
//import javax.jcr.Session;

import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository ;

import org.exoplatform.services.cms.scripts.CmsScript;

/*
* Will need to get The MailService when it has been moved to exo-platform
*/
public class GetMailScript implements CmsScript {
  
  private RepositoryService repositoryService_;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;
  
  public GetMailScript(RepositoryService repositoryService, NodeHierarchyCreator nodeHierarchyCreator) {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;
    
    String protocol = (String)context.get("exo:protocol") ;
    String host = (String)context.get("exo:host") ;
    String port = (String)context.get("exo:port") ;
    String box = (String)context.get("exo:folder") ;
    String userName = (String)context.get("exo:userName") ;
    String password = (String)context.get("exo:password") ;
    String storePath = (String)context.get("exo:storePath") ;     
    GregorianCalendar gc = new GregorianCalendar() ;
    println("\n\n " + gc.getTime());
    println("\n ### Getting mail from " + host + " ... !");
    
    try{
      Properties props = System.getProperties();
      if(protocol.equals("pop3")) {
        props.setProperty("mail.pop3.socketFactory.fallback", "false");
        props.setProperty( "mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      }else {
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty( "mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      }
      
      Session session = Session.getDefaultInstance(props);
      URLName url = new URLName(protocol, host, Integer.valueOf(port), box, userName, password) ;
      Store store = session.getStore(url) ;
      store.connect();
      Folder folder = store.getFolder(box);
      folder.open(Folder.READ_ONLY);
      Message[] mess = folder.getMessages() ;
      int totalMess = mess.length ;
      System.out.println("\n Total: " + mess.length + " message(s)") ;
      if(totalMess > 0) {
        Node storeNode = createStoreNode(storePath + "/" + box) ;
        int i = 0 ;
        while(i < totalMess){       
          Message mes = mess[i] ;
          Node newMail = storeNode.addNode(getMD5MsgId(mes.getAllHeaders()), "exo:mail") ;
          newMail.setProperty("exo:from", getAddress(mes.getFrom())) ;
          newMail.setProperty("exo:to", getAddress(mes.getRecipients(Message.RecipientType.TO))) ;
          newMail.setProperty("exo:cc", getAddress(mes.getRecipients(Message.RecipientType.CC))) ;
          newMail.setProperty("exo:bcc", getAddress(mes.getRecipients(Message.RecipientType.BCC))) ;
          newMail.setProperty("exo:subject", mes.getSubject()) ;
          if(mes.getSentDate() != null) {
            gc.setTime(mes.getSentDate()) ;
            newMail.setProperty("exo:sendDate", gc.getInstance()) ;
          }
          if(mes.getReceivedDate() != null) {
            gc.setTime(mes.getReceivedDate()) ;
            newMail.setProperty("exo:receivedDate", gc.getInstance()) ;
          } 
          Object obj = mes.getContent() ;                 
          if (obj instanceof Multipart) {
            saveMultipartMail((Multipart)obj, newMail);
          } else {
            saveMail(mes, newMail);
          }
          
          i ++ ;
        } 
        storeNode.save() ;
        storeNode.getSession().save() ;         
      }      
      folder.close(false);
      store.close();      
    }catch (Exception e) {
      e.printStackTrace() ;
    }
    println("\n ### Finished ");
  }

  public void setParams(String[] params) {}
  
  private void saveMultipartMail(Multipart multipart, Node newMail){
    try {
      int i = 0 ;
      int n = multipart.getCount() ;
      while( i < n) {
        saveMail(multipart.getBodyPart(i), newMail);
        i++ ;
      }     
    }catch(Exception e) {
      e.printStackTrace() ;
    }   
    
  }
  
  private void saveMail(Part part, Node newMail){
    try {     
      String disposition = part.getDisposition();
      String contentType = part.getContentType();
      if (disposition == null) {        
        if(part.isMimeType("text/plain")){
          newMail.setProperty("exo:content", (String)part.getContent());
        }else if(!part.isMimeType("text/html")){
          MimeMultipart mimeMultiPart = (MimeMultipart)part.getContent() ;
          newMail.setProperty("exo:content", (String)mimeMultiPart.getBodyPart(0).getContent());        
        }       
      } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE)) {
        Node attachment = newMail.addNode(getMD5MsgId(part.getAllHeaders()), "nt:file") ;
        Node content = attachment.addNode("jcr:content", "nt:resource") ;
        content.setProperty("jcr:encoding", "UTF-8") ;
        if(contentType.indexOf(";") > 0) {
          String[] type = contentType.split(";") ;
          content.setProperty("jcr:mimeType", type[0]) ;          
        }else {
          content.setProperty("jcr:mimeType", contentType) ;
        }
        GregorianCalendar gc = new GregorianCalendar() ;
        content.setProperty("jcr:lastModified", gc.getInstance()) ;
        InputStream is = part.getInputStream();
        byte[] buf = new byte[is.available()];
        is.read(buf);
        content.setProperty("jcr:data", new ByteArrayInputStream(buf)) ;              
        
      }
    
    }catch(Exception e) {
      e.printStackTrace() ;
    }
  }
  
  private Node createStoreNode(String storePath) {
    try{
      ManageableRepository manaRepository = repositoryService_.getDefaultRepository() ;
      Node rootNode = manaRepository.getSystemSession(manaRepository.getConfiguration().getDefaultWorkspaceName()).getRootNode();
      //Node rootNode = session.getRootNode();
      String[] array = storePath.split("/") ;
      int i = 0 ;
      int n = array.length ;
      while(i < n) {
        if(array[i] != null && array[i].trim().length() > 0) {
          if(rootNode.hasNode(array[i].trim())) {
            rootNode = rootNode.getNode(array[i].trim()) ;
          }else {
            rootNode.addNode(array[i].trim(),"nt:unstructured") ;
            rootNode.save() ;
            rootNode = rootNode.getNode(array[i].trim()) ;
          }
        }
        i++ ;
      }
      rootNode.getSession().save() ;
      return rootNode ;
    }catch(Exception e) {
      e.printStackTrace() ;
    }
    return null ;
  }
  
  private String getAddress(Address[] addr) {
    String str = "" ;
    int i = 0
    if(addr != null && addr.length > 0) {
      while (i < addr.length) {
        if(str.length() < 1)  {
          str = addr[i].toString() ;              
        }else {
          str = str + ", " + addr[i].toString() ;
        }           
        i++ ;
      }
    }   
    return str ;
  }
  
  /**
   * @return a MD5 string
   */
  private String getMD5MsgId(Enumeration enu) throws Exception {
    // first construct a key by joining all headers
    String key = "";
    long t1 = System.currentTimeMillis();
    while (enu.hasMoreElements()) {
      Header header = (Header)enu.nextElement() ;
      key += header.getValue() ;
    }
    String md5 = getMD5(key);
    long t2 = System.currentTimeMillis();
    return md5;
  }
 
  /**
   * separated getMD5 method ... for a general use.
   * @param s
   * @return a MD5 string
   */
  private String getMD5(String s) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      m.update(s.getBytes(), 0, s.length());
      return "" + new BigInteger(1, m.digest()).toString(16);
    } catch (NoSuchAlgorithmException e) {
      // almost never ... but the idea is we should control all exceptions
      System.out.println("MD5 is not supported !!!");
    }
    return s;
  }

}