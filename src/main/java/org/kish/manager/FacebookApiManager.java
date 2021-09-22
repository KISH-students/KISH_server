package org.kish.manager;

import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.config.ConfigOption;
import org.kish.utils.WebUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FacebookApiManager {
    public static void writePagePost(String content) {
        final Object pageId = KishServer.CONFIG.get(ConfigOption.FACEBOOT_PAGE_ID);
        final String pageAccessToken = (String) KishServer.CONFIG.get(ConfigOption.FACEBOOT_PAGE_ACCESS_TOKEN);

        if (pageAccessToken.isEmpty() || "".equals(pageId)) {    // 둘 중 하나라도 입력 안 되어 있으면 return
            MainLogger.warn("페이스북 페이지 id 또는 access token이 입력되어있지 않습니다. 글을 게시하지 않습니다!");
            return;
        }

        if (!(pageId instanceof String)) {  // page id에 따옴표 처리가 안 되어있는 경우
            MainLogger.warn("페이스북 페이지 아이디에 따옴표를 붙여주세요. 글을 게시하지 않습니다!");
            return;
        }

        try {
            content = URLEncoder.encode(content, "UTF-8");      // 한글 인코딩
        } catch (UnsupportedEncodingException e) {
            MainLogger.error(e);
            return;
        }

        String url = "https://graph.facebook.com/" + pageId + "/feed";
        String parameters = "message=" + content + "&access_token=" + pageAccessToken;
        WebUtils.postRequest(url, WebUtils.ContentType.JSON, parameters);
    }
}
