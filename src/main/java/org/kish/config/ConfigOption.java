package org.kish.config;

import lombok.Getter;

@Getter
public enum ConfigOption {
    MYSQL_HOST("mysql_host", "localhost"),
    MYSQL_PORT("mysql_port", 3306),
    MYSQL_USER("mysql_user", "userName"),
    MYSQL_PW("mysql_pw", "password"),
    MYSQL_DB("mysql_db", "db name"),

    FB_DB_URL("firebase_DatabaseUrl", "Firebase DB주소. ex) https://DB이름.firebaseio.com"),
    FB_ACCOUNT_KEY("firebase_path_serviceAccountKey", "serviceAccountKey.json 파일 경로"),

    GET_ALL_POSTS_ON_BOOT("get_all_posts_on_boot",
            "on으로 설정할경우 서버 시작시 홈페이지의 모든 게시물을 조회하고 가져옵니다. (최초실행용)"),
    SPRING_SERVER_PORT("spring_server_port", 9533),
    SPRING_AJP_PORT("tomcat_apj_port", 8009),
    JOD_PORT("jod_converter_port", 2002L),

    RESOURCE_PATH("resource_path", "./resource/"),
    DOWNLAOD_BASE_URL("download_base_url", "http://hanoischool.net/"),

    FACEBOOT_PAGE_ID("facebook_page_id", ""),
    FACEBOOT_PAGE_ACCESS_TOKEN("facebook_page_access_token", "");

    private final String key;
    private final Object defValue;

    ConfigOption(String key, Object defValue){
        this.key = key;
        this.defValue = defValue;
    }

    @Override
    public String toString() {
        return key;
    }

    public boolean isChanged(Config config){
        return config.get(key).equals(defValue);
    }
}