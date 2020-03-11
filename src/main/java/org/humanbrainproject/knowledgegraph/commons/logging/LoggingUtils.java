package org.humanbrainproject.knowledgegraph.commons.logging;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoggingUtils {
    private static MessageDigest md = null;

    public static final String hashUserId(String userId){
        String userHashedId = "";
        try{
            md = MessageDigest.getInstance("MD5");
        }catch(NoSuchAlgorithmException e){}finally {
            if(md != null){
                md.update(userId.getBytes());
                byte[] digest = md.digest();
                userHashedId = new String(digest).toUpperCase();
            }
        }
        return userHashedId;
    }
}
