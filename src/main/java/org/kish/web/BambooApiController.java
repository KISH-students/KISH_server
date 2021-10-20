package org.kish.web;

import com.google.gson.Gson;
import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.database.BambooDao;
import org.kish.database.KishDAO;
import org.kish.entity.Noti;
import org.kish.manager.FacebookApiManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/api/bamboo")
public class BambooApiController {
    private static final Gson gson = new Gson();
    private final KishServer main;
    @Autowired private BambooDao bambooDao;
    @Autowired private KishDAO kishDAO;

    public BambooApiController(KishServer kishServer){
        this.main = kishServer;
    }

    @ResponseBody
    @RequestMapping("/posts")
    public String getPosts(@RequestParam int page) {
        // page는 0부터 시작

        return gson.toJson(bambooDao.getPosts(page));
    }

    @ResponseBody
    @RequestMapping("/myNotification")
    public String getMyNotification(@RequestParam int page, @RequestParam String seq, @RequestParam String fcm) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }
        response.put("success", true);
        response.put("notification_list", bambooDao.getMyNotification(page, seq));
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/myPosts")
    public String getMyPosts(@RequestParam int page, @RequestParam String seq, @RequestParam String fcm) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }
        response.put("success", true);
        response.put("posts", bambooDao.getMyPosts(page, seq));
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/myComments")
    public String getMyComments(@RequestParam int page, @RequestParam String seq, @RequestParam String fcm) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }
        response.put("success", true);
        response.put("comments", bambooDao.getMyComments(page, seq));
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/notification")
    public String enableNotification(@RequestParam String seq, @RequestParam String fcm, @RequestParam boolean enable) {
        bambooDao.toggleNotification(enable, seq, fcm);
        return "";
    }

    /**
     * 현재는 댓글을 포함한 결과를 전송합니다.
     * 추후 댓글 요청 API를 추가 할 예정입니다.
     */
    @ResponseBody
    @RequestMapping("/post")
    public String viewPost(@RequestParam String seq,
                           int postId) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        result.put("post", bambooDao.getPost(seq, postId));
        result.put("comment", bambooDao.getComments(seq, postId));

        return gson.toJson(result);
    }

    @ResponseBody
    @RequestMapping("/writePost")
    public String writePost(@RequestParam String seq, @RequestParam String fcm,
                            @RequestParam String title, @RequestParam String content,
                            @RequestParam(required = false, defaultValue = "true") boolean fb) {
        // seq = 대출증 id
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }

        if (content.length() < 3 || content.length() > 99999999) {
            response.put("success", false);
            response.put("message", "너무 짧거나 깁니다");
        }
        int bambooPostId = bambooDao.writePost(seq, title, content);
        if (bambooPostId != -1) {
            response.put("success", true);
            response.put("message", "성공적으로 글을 게시하였습니다.");

            HashMap<String, String> data = new HashMap<>();
            data.put("type", "newBambooPost");
            data.put("post_id", Integer.toString(bambooPostId));
            Noti noti
                    = new Noti("newBambooPost"
                    , title + " - 새 익명글"
                    , content.substring(0, Math.min(content.length(), 32)));
            noti.setData(data);
            main.getFirebaseManager().sendFcmWithTopic(noti);

            if (fb) {
                StringBuilder sb = new StringBuilder();
                sb.append(title).append("\n\n")
                        .append(content).append("\n.\n.\n------------------------\n")
                        .append("하노이한국국제학교 앱에서 \"익명\" 댓글과 글을 확인할 수 있어요👻");
                Runnable runnable = () -> {
                    String postId = FacebookApiManager.writePagePost(sb.toString());
                    if (postId.length() > 4) {
                        bambooDao.registerFacebookPost(bambooPostId, postId);
                    }
                };
                runnable.run();     // 다른 스레드에서 실행합니다.
            }
        } else {
            response.put("success", false);
            response.put("message", "서버 오류입니다.");
        }

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/deletePost")
    public String deletePost(@RequestParam String seq, @RequestParam String fcm,
                             @RequestParam int postId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }

        try {
            bambooDao.deletePost(seq, postId);
        } catch(Exception ignore) {
        }
        response.put("success", true);
        response.put("message", "요청이 처리되었습니다");
        MainLogger.info("게시글이 삭제됨 : " + postId);
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/deleteComment")
    public String deleteComment(@RequestParam String seq, @RequestParam String fcm,
                             @RequestParam int commentId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }

        try {
            bambooDao.deleteComment(seq, commentId);
        } catch (Exception ignore) {
        }
        response.put("success", true);
        response.put("message", "요청이 처리되었습니다");
        MainLogger.info("댓글에 삭제됨 : " + commentId);
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/writeComment")
    public String writeComment(@RequestParam String seq,  @RequestParam String fcm,
                               @RequestParam int postId, @RequestParam String content) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }

        if (content.length() < 3 || content.length() > 99999) {
            response.put("success", false);
            response.put("message", "너무 짧거나 깁니다");
        }
        int commentId = bambooDao.addComment(seq, postId, content, false);
        if (commentId != -1) {
            response.put("success", true);
            response.put("message", "성공적으로 댓글을 게시하였습니다.");

            Runnable r = () -> processCommentNotification(false, postId, commentId, seq, content);
            r.run();
        } else {
            response.put("success", false);
            response.put("message", "서버 오류입니다.");
        }

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/reply")
    public String reply(@RequestParam String seq, @RequestParam String fcm,
                        @RequestParam int postId, @RequestParam int parentId,
                        @RequestParam String content) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("success", false);
            response.put("message", "계정을 확인할 수 없습니다.");
            return gson.toJson(response);
        }

        if (content.length() < 3 || content.length() > 99999) {
            response.put("success", false);
            response.put("message", "너무 짧거나 깁니다");
        }
        int commentId = bambooDao.addReply(seq, postId, parentId, content, false);
        if (commentId != -1) {
            response.put("success", true);
            response.put("message", "성공적으로 글을 게시하였습니다.");
            Runnable r = () -> processCommentNotification(true, postId, commentId, seq, content);
            r.run();
        } else {
            response.put("success", false);
            response.put("message", "서버 오류입니다.");
        }

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/getReplies")
    public String getReplies(@RequestParam String seq, @RequestParam String fcm,
                                                @RequestParam int commentId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        List<Map<String, Object>> replies;
        if (!kishDAO.isValidUser(seq, fcm)) {
            replies = bambooDao.getReplies(new HashSet<>(), commentId, seq);
        } else {
            replies = bambooDao.getReplies(commentId, seq);
        }
        response.put("replies", replies);
        response.put("success", true);
        response.put("message", "성공적으로 불러왔습니다");

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/likePost")
    public String likePost(@RequestParam String seq, @RequestParam String fcm,
                           @RequestParam int postId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("message", "계정을 확인할 수 없습니다.");
        } else {
            response.put("count", bambooDao.addPostLike(seq, postId));
        }
        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/likeComment")
    public String likeComment(@RequestParam String seq, @RequestParam String fcm,
                              @RequestParam int commentId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("message", "계정을 확인할 수 없습니다.");
        } else {
            response.put("count", bambooDao.addCommentLike(seq, commentId));
        }

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/unlikePost")
    public String unlikePost(@RequestParam String seq, @RequestParam String fcm,
                             @RequestParam int postId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("message", "계정을 확인할 수 없습니다.");
        } else {
            response.put("count", bambooDao.removePostLike(seq, postId));
        }

        return gson.toJson(response);
    }

    @ResponseBody
    @RequestMapping("/unlikeComment")
    public String unlikeComment(@RequestParam String seq, @RequestParam String fcm,
                                @RequestParam int commentId) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        if (!kishDAO.isValidUser(seq, fcm)) {
            response.put("message", "계정을 확인할 수 없습니다.");
        } else {
            response.put("count", bambooDao.removeCommentLike(seq, commentId));
        }

        return gson.toJson(response);
    }

    public void processCommentNotification(boolean isReply, int postId, int commentId, String seq, String content) {
        String postAuthor = bambooDao.getPostAuthor(postId);
        String commentAuthorName = bambooDao.getDisplayName(seq, postId);

        if (!postAuthor.equals(seq)) {
            bambooDao.addNotification("comment_to_my_post", postAuthor, postId, commentId, "작성한 글에 댓글이 달렸어요.", content);
        }

        if (isReply) {
            Set<String> participants = bambooDao.getCommentParticipants(bambooDao.getParentCommentId(commentId), true);
            participants.remove(postAuthor);
            bambooDao.addNotificationToUsers("replied", participants, postId, commentId, commentAuthorName + "님의 답글", content);
        }

        List<String> list = bambooDao.getNotificationReceivers(commentId);
        List<String> fcmList = bambooDao.getFcmTokensBySeqIds(list, true);
        String authorName = bambooDao.getDisplayName(seq, postId);

        if (seq.equals(postAuthor)) {
            list.remove(seq);
        }

        HashMap<String, String> data = new HashMap<>();
        data.put("type", "newBambooComment");
        data.put("comment_id", Integer.toString(commentId));
        data.put("post_id", Integer.toString(postId));

        Noti noti = new Noti("", authorName + "의 댓글", content.substring(0, Math.min(content.length(), 35)));
        noti.setData(data);
        main.getFirebaseManager().sendFcmForSpecificReceivers(noti, fcmList);
    }
}