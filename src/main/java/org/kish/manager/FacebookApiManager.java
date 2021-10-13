package org.kish.manager;

import com.google.gson.Gson;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.config.ConfigOption;
import org.kish.utils.WebUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class FacebookApiManager {
    private static final Gson gson = new Gson();

    public static String writePagePost(String content) {
        final Object pageId = KishServer.CONFIG.get(ConfigOption.FACEBOOT_PAGE_ID);
        final String pageAccessToken = (String) KishServer.CONFIG.get(ConfigOption.FACEBOOT_PAGE_ACCESS_TOKEN);

        if (pageAccessToken.isEmpty() || "".equals(pageId)) {    // 둘 중 하나라도 입력 안 되어 있으면 return
            MainLogger.warn("페이스북 페이지 id 또는 access token이 입력되어있지 않습니다. 글을 게시하지 않습니다!");
            return "";
        }

        if (!(pageId instanceof String)) {  // page id에 따옴표 처리가 안 되어있는 경우
            MainLogger.warn("페이스북 페이지 아이디에 따옴표를 붙여주세요. 글을 게시하지 않습니다!");
            return "";
        }

        try {
            content = URLEncoder.encode(content, "UTF-8");      // 한글 인코딩
        } catch (UnsupportedEncodingException e) {
            MainLogger.error(e);
            return "";
        }

        String url = "https://graph.facebook.com/" + pageId + "/feed";
        String parameters = "message=" + content + "&access_token=" + pageAccessToken;
        String response = WebUtils.postRequest(url, WebUtils.ContentType.JSON, parameters).getResponse();
        return (String) gson.fromJson(response , HashMap.class).get("id");
    }

    public static void deletePost(String facebookPostId) {
        URL url;
        try {
            final String pageAccessToken = (String) KishServer.CONFIG.get(ConfigOption.FACEBOOT_PAGE_ACCESS_TOKEN);
            url = new URL("https://graph.facebook.com/"+ facebookPostId +"?access_token="+pageAccessToken);

            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("DELETE");
            con.setRequestProperty("Accept-Charset", "UTF-8");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while(bufferedReader.readLine() != null);
            bufferedReader.close();
            con.disconnect();
        } catch (IOException e) {
            MainLogger.error(e);
        }
    }
}
