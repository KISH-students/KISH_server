package org.kish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jodconverter.core.office.OfficeException;
import org.kish.config.Config;
import org.kish.config.ConfigOption;
import org.kish.manager.JodManager;
import org.kish.manager.TableManager;
import org.kish.manager.FirebaseManager;
import org.kish.web.KishMagazineApiController;
import org.kish.web.PostApiController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.support.RequestHandledEvent;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.io.File;
import java.util.Properties;

@EnableCaching
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"org.kish", "org.kish.web", "org.kish.database"})
public class KishServer {
    public static final String KISH_WEB_ROOT = "http://hanoischool.net";
    public static final Gson GSON = new Gson();
    public static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config CONFIG = null;
    public static ConfigurableApplicationContext CAC = null;

    public static TableManager tableManager = null;
    public static FirebaseManager firebaseManager = null;
    public static JodManager jodManager = null;

    public static JdbcTemplate jdbcTemplate = null;
    public static File RESOURCE_PATH = null;

    public static boolean silent = false;

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--silent".equals(arg)) {
                silent = true;
                MainLogger.warn("새 글 알림을 보내지 않습니다!");
            }
        }

        /* Kish server configuration */
        CONFIG = new Config("kish_config.json");

        if(ConfigOption.MYSQL_DB.isChanged(CONFIG)){
            shutdown(true, "mysql_db is not configured");
        }
        RESOURCE_PATH = new File((String) CONFIG.get(ConfigOption.RESOURCE_PATH));

        /* spring boot 실행*/
        SpringApplication application = new SpringApplication(KishServer.class);
        Properties props = new Properties();

        String host = "jdbc:mysql://%host%:%port%/%db%?useUnicode=true&characterEncoding=utf8&useSSL=false";
        host = host.replace("%host%", (String) CONFIG.get("mysql_host"));
        host = host.replace("%port%", Long.toString(CONFIG.getLong("mysql_port")));
        host = host.replace("%db%", (String) CONFIG.get("mysql_db"));

        props.put("spring.datasource.url", host);
        props.put("spring.datasource.username", CONFIG.get("mysql_user"));
        props.put("spring.datasource.password", CONFIG.get("mysql_pw"));

        props.put("server.port", CONFIG.get(ConfigOption.SPRING_SERVER_PORT));
        props.put("ajp.port", CONFIG.get(ConfigOption.SPRING_AJP_PORT));
        application.setDefaultProperties(props);
        CAC = application.run(args);

        /* 매니저 초기화 */
        tableManager.checkAllTable();
        firebaseManager = new FirebaseManager();
        try {
            jodManager = new JodManager();
        } catch (OfficeException e) {
            shutdown(false, e);
        }

        /* config에서 parse all posts 확인 */
        PostApiController pac = CAC.getBean(PostApiController.class);
        if(CONFIG.get(ConfigOption.GET_ALL_POSTS_ON_BOOT).equals("on")){
            pac.checkingNewPostLock = true;
            pac.parseAllPosts();
        }

        CAC.getBean(KishMagazineApiController.class).updateArticles();
        CAC.getBean(KishMagazineApiController.class).cacheHome(true);

        MainLogger.info("kish server가 준비되었음.");
    }

    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
    }

    public static void shutdown(boolean force, Object reason){
        if(reason instanceof Exception) MainLogger.LOGGER.fatal(reason);
        else MainLogger.warn(reason);

        MainLogger.warn("Kish server is shutting down now ...");

        if(!force){
            // something to do ...
        }

        System.exit(0);
    }

    public Config getConfig(){
        return CONFIG;
    }

    public FirebaseManager getFirebaseManager(){
        return firebaseManager;
    }
}
