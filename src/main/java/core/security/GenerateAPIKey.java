package core.security;

import core.entities.ApiKey;
import core.entities.ApiKeyDAO;
import org.springframework.util.DigestUtils;

public class GenerateAPIKey {

    public static String generateGenericKey() {
        return generateKey(" "+System.currentTimeMillis());
    }

    public static String generateSafeKey(ApiKeyDAO apiKeyDAO, String accountName) {
        String key = "";
        boolean foundNewKey = false;
        while(!foundNewKey) {
            key = generateKey(accountName);
            if(!keyExists(key,apiKeyDAO)) {
                foundNewKey = true;
            }
        }
        return key;
    }

    public static String generateKey(String accountName){
        accountName +=System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(accountName.getBytes());
    }

    private static boolean keyExists(String key,ApiKeyDAO apiKeyDAO) {
        boolean doesKeyExist;
        ApiKey apiKey = apiKeyDAO.findByApiKey(key);
        if(apiKey == null || apiKey.getAccountId() < 1) {
            doesKeyExist = false;
        } else {
            System.out.println("------------- APIKey Exists: "+apiKey+"    AccountID: "+apiKey.getAccountId());
            doesKeyExist = true;
        }
        return doesKeyExist;
    }

}
