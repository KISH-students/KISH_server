package org.kish.database;

import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.manager.FacebookApiManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.*;

@Repository
public class BambooDao {
    public JdbcTemplate jdbcTemplate;
    @Autowired
    public KishDAO kishDAO;

    public BambooDao(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        KishServer.jdbcTemplate = this.jdbcTemplate;
    }

    public void toggleNotification(boolean enable, String seq, String fcm) {
        String sql = "UPDATE `kish_users` SET `bamboo_noti`=? WHERE `user_id`=? AND `fcm_token`=?;";
        jdbcTemplate.update(sql, enable, seq, fcm);
    }

    /**
     *
     * @param commentId
     * @return seq list
     */
    public List<String> getNotificationReceivers(int commentId) {
        HashSet<String> receivers = new HashSet<>();
        int postId;
        int parentCommentId;
        String commentAuthor;
        boolean isReply;

        String sql = "SELECT `post_id`,`is_reply`,`comment_parent_id`,`comment_author_id` FROM `bamboo_comments` WHERE `comment_id`=?";
        Map<String, Object> comment = jdbcTemplate.queryForList(sql, commentId).get(0);
        if (comment == null) {
            return new ArrayList<>();
        }
        postId = (int) comment.get("post_id");
        isReply = (boolean) comment.get("is_reply");
        commentAuthor = (String) comment.get("comment_author_id");
        parentCommentId = (int) comment.get("comment_parent_id");

        String postAuthor = getPostAuthor(postId);
        if(postAuthor.isEmpty()) {
            return new ArrayList<>();
        }
        if (!commentAuthor.equals(postAuthor)) {
            receivers.add(postAuthor);  // 글 작성자
        }

        if (isReply) {
            HashSet<String> participantSet = (HashSet<String>) getCommentParticipants(parentCommentId, true);
            participantSet.remove(commentAuthor);
            participantSet.remove(postAuthor);
            receivers.addAll(participantSet);
        }
        return new ArrayList<>(receivers);
    }

    public int getParentCommentId(int commentId) {
        String sql = "SELECT `comment_parent_id` FROM `bamboo_comments` WHERE `comment_id`=?";
        return (int) jdbcTemplate.queryForList(sql, commentId).get(0).get("comment_parent_id");
    }

    public String getPostAuthor(int postId) {
        String sql = "SELECT `bamboo_author` FROM `bamboo_posts` WHERE `bamboo_id`=?";
        Map<String, Object> post = jdbcTemplate.queryForList(sql, postId).get(0);
        if (post == null) {
            return "";
        }
        return (String) post.get("bamboo_author");
    }

    public Set<String> getCommentParticipants(int parentCommentId, boolean notRemoved) {
        HashSet<String> participants = new HashSet<>();

        String sql = "SELECT `comment_author_id`, `comment_content` FROM `bamboo_comments` WHERE `comment_parent_id`=?";
        List<Map<String, Object>> replies = jdbcTemplate.queryForList(sql, parentCommentId);
        for (Map<String, Object> reply : replies) {
            String id = (String) reply.get("comment_author_id");
            String content = (String) reply.get("comment_content");
            if (notRemoved && content.isEmpty()) continue;
            participants.add(id);
        }

        return participants;
    }

    public List<String> getFcmTokensBySeqIds(List<String> seqIds, boolean enabledNoti) {
        if (seqIds.isEmpty()) return new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        int i = 0;
        int size = seqIds.size();
        for (String receiver : seqIds) {
            i++;
            sb.append('"').append(receiver).append('"');
            if (i < size) sb.append(",");
        }

        ArrayList<String> fcmTokens = new ArrayList<>();
        String sql = "SELECT `fcm_token` FROM `kish_users` WHERE `user_id` IN (" + sb + ") ";
        if (enabledNoti) {
            sql += "AND `bamboo_noti`=1";
        }
        List<Map<String, Object>> receiverInfo = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> receiver : receiverInfo) {
            fcmTokens.add((String) receiver.get("fcm_token"));
        }

        return fcmTokens;
    }

    public void addNotification(String type, String seq, int post, int comment, String title, String content) {
        HashSet<String> tmp = new HashSet<>();
        tmp.add(seq);
        addNotificationToUsers(type, tmp, post, comment,title, content);
    }

    public void addNotificationToUsers(String type, Set<String> userSet, int post, int comment, String title, String content) {
        String sql = "INSERT INTO `bamboo_notification` (`post_id`, `comment_id`, `type`, `title`, `content`, `user`) " +
                "VALUES (?,?,?,?,?,?);";
        ArrayList<Object[]> args = new ArrayList<>();
        for (String user : userSet) {
            args.add(new Object[]{post, comment, type, title, content, user});
        }

        jdbcTemplate.batchUpdate(sql, args);
    }

    public String getDisplayName(String seq, int postId) {
        final String AUTHOR = "작성자";

        String sql = "SELECT `bamboo_author` FROM `bamboo_posts` WHERE bamboo_id = ?";
        Map<String, Object> post = jdbcTemplate.queryForList(sql, postId).get(0);
        if (post != null) {
            if (seq.equals(post.get("bamboo_author"))) {
                return AUTHOR;
            }
        }

        HashSet<String> writers = new HashSet<>();  // 댓글 작성자 set
        sql = "SELECT `comment_author_id`, `comment_author_displayname` FROM `bamboo_comments` WHERE `post_id` = ?";
        List<Map<String, Object>> comments = jdbcTemplate.queryForList(sql, postId);    // 게시물 댓글 쿼리

        for (Map<String, Object> comment : comments) {  // 해당 게시물에 쓰여진 모든 댓글 확인
            String displayName = (String) comment.get("comment_author_displayname");    // 캐시된 이름
            String authorId = (String) comment.get("comment_author_id");    // 작성자 seq

            if(seq.equals(authorId)) {  // 이미 번호를 부여 받은 경우
                return displayName;
            }
            if (!AUTHOR.equals(displayName)) {  // 작성자가 아닌 경우에만 카운트
                writers.add(authorId);
            }
        }

        return "익명 " + writers.size();
    }

    public List<Map<String, Object>> getPosts(int page) {
        String sql = "SELECT * FROM `bamboo_posts` ORDER BY `bamboo_date` DESC LIMIT ?, 10";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, page * 10);

        for (Map<String, Object> map : result) {
            map.remove("bamboo_author");

            String content = (String) map.get("bamboo_content");
            if (content.length() > 30) {
                map.put("bamboo_content", content.substring(0, 30) + "...");
            }
            int postId = (Integer) map.get("bamboo_id");
            int likes = getPostLikeCount(postId);
            int comments = getCommentCount(postId);

            map.put("like_count", likes);
            map.put("comment_count", comments);
        }

        return result;
    }

    public List<Map<String, Object>> getMyPosts(int page, String seq) {
        String sql = "SELECT * FROM `bamboo_posts` WHERE `bamboo_author`=? ORDER BY `bamboo_date` DESC LIMIT ?, 10";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, seq, page * 10);

        for (Map<String, Object> map : result) {
            map.remove("bamboo_author");

            String content = (String) map.get("bamboo_content");
            if (content.length() > 30) {
                map.put("bamboo_content", content.substring(0, 30) + "...");
            }
            int postId = (Integer) map.get("bamboo_id");
            int likes = getPostLikeCount(postId);
            int comments = getCommentCount(postId);

            map.put("like_count", likes);
            map.put("comment_count", comments);
        }

        return result;
    }

    public int writePost(String author, String title, String content) {
        String sql = "INSERT INTO `bamboo_posts` (`bamboo_id`, `bamboo_title`, `bamboo_content`, `bamboo_author`, `bamboo_date`) VALUES (NULL, ?, ?, ?, CURRENT_TIMESTAMP);";
        try {
            jdbcTemplate.update(sql, title, content, author);
            String tmp = String.valueOf(jdbcTemplate.queryForList("SELECT LAST_INSERT_ID();").get(0).get("LAST_INSERT_ID()"));
            return Integer.parseInt(tmp);
        } catch (Exception e) {
            MainLogger.error(e);
            return -1;
        }
    }

    public void registerFacebookPost(int bambooId, String fbPostId) {
        String sql = "INSERT INTO `bamboo_facebook_posts` (`bamboo_id`, `facebook_id`) VALUES (?, ?);";
        jdbcTemplate.update(sql, bambooId, fbPostId);
    }

    @Deprecated
    public int getPostId (String seq, String content) {
        String sql = "SELECT `bamboo_id` FROM `bamboo_posts` WHERE `bamboo_author`=? AND `bamboo_content`=?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, seq, content);
        return (int) result.get(result.size() - 1).get("bamboo_id");      // 가장 최근 글
    }

    public Map<String, Object> getPost(String seq, int postId) {
        String sql = "SELECT `bamboo_author`, bamboo_id, bamboo_title, bamboo_content, bamboo_date FROM `bamboo_posts` WHERE bamboo_id = ?";
        Set<String> likers = getPostLikers(postId);
        Map<String, Object> post = jdbcTemplate.queryForList(sql, postId).get(0);
        String author = (String) post.get("bamboo_author");

        post.put("liked", likers.contains(seq));
        post.put("likeCount", likers.size());
        post.remove("bamboo_author");
        post.put("IAmAuthor", seq.equals(author));
        return post;
    }

    public void deletePost(String seq, int postId) {
        String sql = "SELECT `facebook_id` FROM `bamboo_facebook_posts` WHERE `bamboo_id` = ?";
        List<Map<String, Object>> facebookPostList = jdbcTemplate.queryForList(sql, postId);
        if (facebookPostList.size() > 0) {
            FacebookApiManager.deletePost((String) facebookPostList.get(0).get("facebook_id"));
        }

        sql = "SELECT `bamboo_id` FROM `bamboo_posts` WHERE `bamboo_author`=? AND `bamboo_id`=?";
        if (!kishDAO.isAdmin(seq)) {
            List temp = jdbcTemplate.queryForList(sql, seq, postId);
            if (temp.isEmpty()) return;
        }

        sql = "INSERT INTO bamboo_post_backup SELECT * FROM bamboo_posts WHERE `bamboo_id`=?";
        jdbcTemplate.update(sql, postId);

        sql = "DELETE FROM `bamboo_posts` WHERE `bamboo_id`=?";
        jdbcTemplate.update(sql, postId);
    }

    public void deleteComment(String seq, int commentId) {
        String sql = "SELECT `comment_id` FROM `bamboo_comments` WHERE `comment_author_id`=? AND `comment_id`=?";
        if (!kishDAO.isAdmin(seq)) {
            List temp = jdbcTemplate.queryForList(sql, seq, commentId);
            if (temp.isEmpty()) return;
        }

        sql = "INSERT INTO bamboo_comment_backup SELECT * FROM bamboo_comments WHERE `comment_id`=?";
        jdbcTemplate.update(sql, commentId);

        sql = "UPDATE `bamboo_comments` SET `comment_content` = '' WHERE `comment_id`=?";
        jdbcTemplate.update(sql, commentId);
    }

    public List<Map<String, Object>> getComments(String seq, int postId) {
        String sql = "SELECT comment_author_id, comment_id, comment_content, comment_author_displayname, is_facebook_user FROM `bamboo_comments` WHERE post_id=? AND is_reply=0";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, postId);
        Set<Integer> liked = getLikedComments(seq);

        if (!result.isEmpty()) {
            for (Map<String, Object> comment : result) {
                String author = (String) comment.get("comment_author_id");
                int commentId = (int) comment.get("comment_id");

                comment.put("liked", liked.contains(commentId));
                comment.put("likes", getCommentLikeCount(commentId));
                comment.put("IAmAuthor", seq.equals(author));
                comment.remove("comment_author_id");

                List<Map<String, Object>> replies = getReplies(liked, commentId, seq);
                comment.put("replies", replies);
            }
        }

        return result;
    }

    public List<Map<String, Object>> getMyComments(int page, String seq) {
        String sql = "SELECT `post_id`, comment_id, comment_content FROM `bamboo_comments` WHERE comment_author_id=? AND NOT comment_content = '' ORDER BY `comment_date` DESC LIMIT ?, 10";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, seq, page);

        StringBuilder sb = new StringBuilder();
        HashSet<Integer> postIdSet = new HashSet<>();
        result.forEach((comment) -> postIdSet.add((int) comment.get("post_id")));

        HashMap<Integer, String> postTitleMap = new HashMap<>();
        if (postIdSet.size() > 0) {
            int i = 0;
            int size = postIdSet.size();
            for (Integer commentId : postIdSet) {
                i++;
                sb.append(commentId);
                if (i < size) {
                    sb.append(",");
                }
            }

            sql = "SELECT `bamboo_id`, `bamboo_title` FROM `bamboo_posts` WHERE `bamboo_id` IN (" + sb + ");";
            jdbcTemplate.queryForList(sql).forEach((post) -> {
                postTitleMap.put((int) post.get("bamboo_id"), (String) post.get("bamboo_title"));
            });
        }

        Set<Integer> liked = getLikedComments(seq);

        if (!result.isEmpty()) {
            for (Map<String, Object> comment : result) {
                int commentId = (int) comment.get("comment_id");
                int postId = (int) comment.get("post_id");
                comment.put("liked", liked.contains(commentId));
                comment.put("likes", getCommentLikeCount(commentId));
                comment.put("post_title", postTitleMap.getOrDefault(postId, ""));
            }
        }

        return result;
    }

    public List<Map<String, Object>> getReplies(int commentId, String seq) {
        return getReplies(getLikedComments(seq), commentId, seq);
    }

    public List<Map<String, Object>> getReplies(Set<Integer> likedComments, int parentCommentId, String seq) {
        String sql = "SELECT comment_author_id, comment_id, comment_content, comment_author_displayname, is_facebook_user FROM `bamboo_comments` WHERE comment_parent_id=? AND is_reply=1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, parentCommentId);
        for (Map<String, Object> comment : result) {
            String author = (String) comment.get("comment_author_id");
            int id = (Integer) comment.get("comment_id");

            comment.put("liked", likedComments.contains(id));
            comment.put("IAmAuthor", seq.equals(author));
            comment.put("likes", getCommentLikeCount(id));
            comment.remove("comment_author_id");
        }

        return result;
    }

    public int getCommentCount(int postId) {
        String sql = "SELECT comment_id FROM `bamboo_comments` WHERE post_id = ?";
        return jdbcTemplate.queryForList(sql, postId).size();
    }

    public int addComment(String seq, int postId, String content, boolean facebook) {
        try {
            String sql = "INSERT INTO `bamboo_comments`(\n" +
                    "    `comment_id`,\n" +
                    "    `post_id`,\n" +
                    "    `comment_content`,\n" +
                    "    `comment_parent_id`,\n" +
                    "    `comment_author_id`,\n" +
                    "    `comment_author_displayname`,\n" +
                    "    `comment_date`,\n" +
                    "    `is_reply`,\n" +
                    "    `is_facebook_user`\n" +
                    ")\n" +
                    "VALUES(\n" +
                    "    NULL,\n" + //comment id
                    "    ?,\n" + //post id
                    "    ?,\n" + //content
                    "    '-1',\n" + //parent id
                    "    ?,\n" + //작성자 seq
                    "    ?,\n" + //표시될 이름
                    "    CURRENT_TIMESTAMP,\n" + //comment date
                    "    '0',\n" + //답장 여부
                    "    ?\n" + //페이스북 유저 여부
                    ");";

            String displayName = getDisplayName(seq, postId);
            jdbcTemplate.update(sql, postId, content, seq, displayName, facebook);
            String tmp = String.valueOf(jdbcTemplate.queryForList("SELECT LAST_INSERT_ID();").get(0).get("LAST_INSERT_ID()"));
            return Integer.parseInt(tmp);
        } catch (Exception e) {
            return -1;
        }
    }

    public int addReply(String seq, int postId, int parentId, String content, boolean facebook) {
        try {
            String sql = "INSERT INTO `bamboo_comments`(\n" +
                    "    `comment_id`,\n" +
                    "    `post_id`,\n" +
                    "    `comment_content`,\n" +
                    "    `comment_parent_id`,\n" +
                    "    `comment_author_id`,\n" +
                    "    `comment_author_displayname`,\n" +
                    "    `comment_date`,\n" +
                    "    `is_reply`,\n" +
                    "    `is_facebook_user`\n" +
                    ")\n" +
                    "VALUES(\n" +
                    "    NULL,\n" + //comment id
                    "    ?,\n" + //post id
                    "    ?,\n" + //content
                    "    ?,\n" + //parent id
                    "    ?,\n" + //작성자 seq
                    "    ?,\n" + //표시될 이름
                    "    CURRENT_TIMESTAMP,\n" + //comment date
                    "    '1',\n" + //답장 여부
                    "    ?\n" + //페이스북 유저 여부
                    ");";

            String displayName = getDisplayName(seq, postId);
            jdbcTemplate.update(sql, postId, content, parentId, seq, displayName, facebook);
            String tmp = String.valueOf(jdbcTemplate.queryForList("SELECT LAST_INSERT_ID();").get(0).get("LAST_INSERT_ID()"));
            return Integer.parseInt(tmp);
        } catch (Exception e) {
            return -1;
        }
    }

    public int addPostLike(String seq, int postId) {
        String sql = "INSERT INTO `bamboo_post_likes` SELECT NULL, ?, ? FROM DUAL" +
                " WHERE NOT EXISTS (SELECT * FROM `bamboo_post_likes` WHERE `post_id` = ? AND `owner_id` = ?);";
        jdbcTemplate.update(sql, postId, seq, postId, seq);
        return getPostLikeCount(postId);
    }

    public int addCommentLike(String seq, int commentId) {
        String sql = "INSERT INTO `bamboo_comment_likes` SELECT NULL, ?, ? FROM DUAL" +
                " WHERE NOT EXISTS (SELECT * FROM `bamboo_comment_likes` WHERE `comment_id` = ? AND `owner_id` = ?);";
        jdbcTemplate.update(sql, commentId, seq, commentId, seq);
        return getCommentLikeCount(commentId);
    }

    public int removePostLike(String seq, int postId) {
        String sql = "DELETE FROM `bamboo_post_likes` WHERE `post_id`=? AND `owner_id`=?";
        jdbcTemplate.update(sql, postId, seq);
        return getCommentLikeCount(postId);
    }

    public int removeCommentLike(String seq, int commentId) {
        String sql = "DELETE FROM `bamboo_comment_likes` WHERE `comment_id`=? AND `owner_id`=?";
        jdbcTemplate.update(sql, commentId, seq);
        return getCommentLikeCount(commentId);
    }

    public Set<String> getPostLikers(int postId) {
        String sql = "SELECT `owner_id` FROM `bamboo_post_likes` WHERE `post_id`=?";
        List<Map<String, Object>> comments = jdbcTemplate.queryForList(sql, postId);

        HashSet<String> likers = new HashSet<>();
        for (Map<String, Object> comment : comments) {
            likers.add((String) comment.get("owner_id"));
        }

        return likers;
    }

    public int getPostLikeCount(int postId) {
        String sql = "SELECT `like_id` FROM `bamboo_post_likes` WHERE `post_id` = ?";
        return jdbcTemplate.queryForList(sql, postId).size();
    }

    public int getCommentLikeCount(int commentId) {
        String sql = "SELECT `like_id` FROM `bamboo_comment_likes` WHERE `comment_id` = ?";
        return jdbcTemplate.queryForList(sql, commentId).size();
    }

    public Set<Integer> getLikedPosts(String seq) {
        if ("".equals(seq)) {
            return new HashSet<>();
        }

        String sql = "SELECT `like_id` FROM `bamboo_post_likes` WHERE `owner_id` = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, seq);
        HashSet<Integer> posts = new HashSet<>();

        for (Map<String, Object> map : result) {
            posts.add((Integer) map.get("like_id"));
        }

        return posts;
    }

    public Set<Integer> getLikedComments(String seq) {
        if ("".equals(seq)) {
            return new HashSet<>();
        }

        String sql = "SELECT `comment_id` FROM `bamboo_comment_likes` WHERE `owner_id` = ?";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, seq);
        HashSet<Integer> comments = new HashSet<>();

        for (Map<String, Object> map : result) {
            comments.add((Integer) map.get("comment_id"));
        }

        return comments;
    }
}
