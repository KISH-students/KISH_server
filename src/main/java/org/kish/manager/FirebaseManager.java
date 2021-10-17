package org.kish.manager;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.messaging.*;
import org.kish.config.Config;
import org.kish.config.ConfigOption;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.database.KishDAO;
import org.kish.entity.Noti;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FirebaseManager {
    public FirebaseApp firebaseApp;
    public boolean isReady = false;

    private KishDAO kishDao;

    public FirebaseManager(){
        Config config = KishServer.CONFIG;

        String jsonPath = (String) config.get(ConfigOption.FB_ACCOUNT_KEY);
        File file = new File(jsonPath);
        if(!file.exists()){
            MainLogger.error("Firebase의 serviceAccountKey.json 파일을 찾지 못했습니다.");
            MainLogger.error("현재 설정된 경로는 \"" + jsonPath + "\" 입니다.");
            MainLogger.error("kish2020.json 파일에서 해당 경로를 수정해주세요.");
            MainLogger.error("FirebaseManager 비활성화됨");
            return;
        }

        FileInputStream serviceAccount;
        try {
            serviceAccount = new FileInputStream(jsonPath);
            FirebaseOptions options;
            options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl((String) config.get(ConfigOption.FB_DB_URL))
                    .build();
            this.firebaseApp = FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            MainLogger.error("FirebaseManager 초기화 중 오류 발생", e);
        }
        this.kishDao = KishServer.CAC.getBean(KishDAO.class);
        this.isReady = true;
    }

    public boolean isExistUser(String uid){
        if(this.firebaseApp == null) return false;
        try {
            FirebaseAuth.getInstance().getUser(uid);
        } catch (FirebaseAuthException ignore) {
            return false;
        }
        return true;
    }

/*    public void sendFCMToAdmin(String title, String content, Map<String, String> data){
        this.sendFCM("admin", title, content, data);
    }*/

    public void sendFcmWithTopic(Noti notification) {
        if(!isReady) {
            MainLogger.warn("Firebase is not ready.");
            return;
        }

        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

        Message message = Message.builder()
                .setAndroidConfig(AndroidConfig.builder()
                        .setTtl(3600 * 1000)
                        .setPriority(notification.getPriority())
                        .setNotification(AndroidNotification.builder()
                                .setColor(notification.getColor())
                                .build())
                        .build())
                .setNotification(
                        new Notification(notification.getTitle()
                                , notification.getContent()))
                .putAllData(notification.getData())
                .setTopic(notification.getTopic())
                .build();
        firebaseMessaging.sendAsync(message);
    }

    /**
     *
     * @param notification
     * @param receivers(fcm)
     */
    public void sendFcmForSpecificReceivers(Noti notification, List<String> receivers) {
        if(!isReady) {
            MainLogger.warn("Firebase is not ready.");
            return;
        }
        for (String receiver : receivers) {
            MainLogger.info(receiver);
        }
        if (receivers.isEmpty()) return;
        MainLogger.info(receivers.size() + "명에게 메세지 보냄.");

        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();

        MulticastMessage message = MulticastMessage.builder()
                .setAndroidConfig(AndroidConfig.builder()
                        .setTtl(3600 * 1000)
                        .setPriority(notification.getPriority())
                        .setNotification(AndroidNotification.builder()
                                .setColor(notification.getColor())
                                .build())
                        .build())
                .setNotification(
                        new Notification(notification.getTitle()
                                , notification.getContent()))
                .putAllData(notification.getData())
                .addAllTokens(receivers)
                .build();
        try {
            firebaseMessaging.sendMulticast(message);
        } catch (Exception e) {
            MainLogger.error(e);
        }
    }

    @Deprecated
    public void sendFcm(Noti notification) {
        if(!isReady) {
            MainLogger.warn("Firebase is not ready.");
            return;
        }

        Thread thread = new Thread(() -> {
            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            ArrayList<String> users = (ArrayList) kishDao.getDeviceIdByTopic(notification.getTopic());
            if (users.size() < 1) return;

            MulticastMessage message = MulticastMessage.builder()
                    .setAndroidConfig(AndroidConfig.builder()
                            .setTtl(3600 * 1000)
                            .setPriority(notification.getPriority())
                            .setNotification(AndroidNotification.builder()
                                    .setColor(notification.getColor())
                                    .build())
                            .build())
                    .setNotification(
                            new Notification(notification.getTitle()
                                    , notification.getContent()))
                    .putAllData(notification.getData())
                    .addAllTokens(users)
                    .build();
            ArrayList<String> needToRemove = new ArrayList<>();

            try {
                BatchResponse response = firebaseMessaging.sendMulticast(message);
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int i = 0; i < responses.size(); i++) {
                        if (!responses.get(i).isSuccessful()) {
                            if (responses.get(i).getException().getErrorCode().equals("invalid-argument"))
                                needToRemove.add(users.get(i));
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                MainLogger.error(e);
            } finally {
                kishDao.removeUsersFromAllTopics(needToRemove);
            }
        });
        thread.start();
    }

    public int addNotificationUser(String topic, String userToken){
        return kishDao.addUserToTopic(topic, userToken);
    }

    public int removeNotificationUser(String topic, String userToken){
        return kishDao.removeUserFromTopic(topic, userToken);
    }

    public boolean isUserInTopic(String topic, String userToken){
        return kishDao.isUserInTopic(topic, userToken);
    }
}
