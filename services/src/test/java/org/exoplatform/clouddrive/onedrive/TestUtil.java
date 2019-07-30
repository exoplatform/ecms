//package org.exoplatform.clouddrive.onedrive;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.UnsupportedEncodingException;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.StringJoiner;
//
//import org.apache.commons.io.IOUtils;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.NameValuePair;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.message.BasicNameValuePair;
//
//import com.google.gson.Gson;
//
//public class TestUtil {
//
//    private static OneDriveTokenResponse retrieveAccessTokenByRefreshToken(String refreshToken) throws IOException {
//        return retrieveAccessToken("", "", null, refreshToken, "refresh_token");
//    }
//
//    public static String getRefreshToken() {
//        try(InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("refresh.token");){
//            return IOUtils.toString(inputStream, "UTF-8").trim();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static String retrieveAccessToken() {
//        try(InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("refresh.token");){
//            String refreshToken = IOUtils.toString(inputStream, "UTF-8").trim();
//            OneDriveTokenResponse oneDriveTokenResponse = retrieveAccessTokenByRefreshToken(refreshToken);
//            return oneDriveTokenResponse.getToken();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//    private static final HttpClient httpclient = HttpClients.createDefault();
//    private static final Gson gson = new Gson();
//    class Scopes {
//        static final String FilesReadAll            = "https://graph.microsoft.com/Files.Read.All";
//
//        static final String FilesRead               = "https://graph.microsoft.com/Files.Read";
//
//        static final String FilesReadSelected       = "https://graph.microsoft.com/Files.Read.Selected";
//
//        static final String FilesReadWriteSelected  = "https://graph.microsoft.com/Files.ReadWrite.Selected";
//
//        static final String FilesReadWrite          = "https://graph.microsoft.com/Files.ReadWrite";
//
//        static final String FilesReadWriteAll       = "https://graph.microsoft.com/Files.ReadWrite.All";
//
//        static final String FilesReadWriteAppFolder = "https://graph.microsoft.com/Files.ReadWrite.AppFolder";
//
//        static final String UserRead                = "https://graph.microsoft.com/User.Read";
//
//        static final String UserReadWrite           = "https://graph.microsoft.com/User.ReadWrite";
//
//        static final String offlineAccess           = "offline_access";
//
//        static final String UserReadWriteAll        = "https://graph.microsoft.com/User.ReadWrite.All";
//    }
//
//    public final static String SCOPES = scopes();
//
//    private static String scopes() {
//        StringJoiner scopes = new StringJoiner(" ");
//        scopes.add(org.exoplatform.clouddrive.onedrive.Scopes.FilesReadWriteAll)
//          .add(Scopes.FilesRead)
//          .add(Scopes.FilesReadWrite)
//          .add(Scopes.FilesReadAll)
////          .add(Scopes.FilesReadSelected)
////          .add(Scopes.UserReadWriteAll)
//                .add(Scopes.UserRead)
////          .add(Scopes.UserReadWrite)
//                .add(Scopes.offlineAccess);
////          .add(Scopes.FilesReadWriteAppFolder)
////          .add(Scopes.FilesReadWriteSelected);
//        return scopes.toString();
//    }
//    private static OneDriveTokenResponse retrieveAccessToken(String clientId,
//                                                      String clientSecret,
//                                                      String code,
//                                                      String refreshToken,
//                                                      String grantType) throws IOException {
//        HttpPost httppost = new HttpPost("https://login.microsoftonline.com/common/oauth2/v2.0/token");
//        List<NameValuePair> params = new ArrayList<>(5);
//        if (grantType.equals("refresh_token")) {
//            params.add(new BasicNameValuePair("refresh_token", refreshToken));
//        } else if (grantType.equals("authorization_code")) {
//            params.add(new BasicNameValuePair("code", code));
//        } else {
//            return null;
//        }
//        params.add(new BasicNameValuePair("grant_type", grantType));
//        params.add(new BasicNameValuePair("client_secret", clientSecret));
//        params.add(new BasicNameValuePair("client_id", clientId));
//        params.add(new BasicNameValuePair("scope", SCOPES));
//        try {
//            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//
//        }
//
//        HttpResponse response = httpclient.execute(httppost);
//
//        HttpEntity entity = response.getEntity();
//        if (entity != null) {
//            try (InputStream inputStream = entity.getContent()) {
//                String responseBody = IOUtils.toString(inputStream, Charset.forName("UTF-8"));
//                return gson.fromJson(responseBody, OneDriveTokenResponse.class);
//            }
//        }
//        return null;
//    }
//
//
//
//
//}
