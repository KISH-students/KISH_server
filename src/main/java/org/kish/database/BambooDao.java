package org.kish.database;

import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.manager.FacebookApiManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigInteger;
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

        sql = "UPDATE `bamboo_comments` SET `comment_content` = '삭제된 댓글입니다.' WHERE `comment_id`=?";
        jdbcTemplate.update(sql, commentId);
    }

    public List<Map<String, Object>> getComments(String seq, int postId) {
        String sql = "SELECT comment_author_id, comment_id, comment_content, comment_author_displayname, is_facebook_user FROM `bamboo_comments` WHERE post_id=? AND is_reply=0";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, postId);
        Set<Integer> liked = getLikedComments(seq);
        Set<Integer> likedComments;

        if (!result.isEmpty()) {
            likedComments = getLikedComments(seq);
            for (Map<String, Object> comment : result) {
                String author = (String) comment.get("comment_author_id");
                int commentId = (int) comment.get("comment_id");

                comment.put("liked", liked.contains(commentId));
                comment.put("likes", getCommentLikeCount(commentId));
                comment.put("IAmAuthor", seq.equals(author));
                comment.remove("comment_author_id");

                List<Map<String, Object>> replies = getReplies(likedComments, commentId, seq);
                comment.put("replies", replies);
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

    public boolean addComment(String seq, int postId, String content, boolean facebook) {
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
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean addReply(String seq, int postId, int parentId, String content, boolean facebook) {
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
            return true;
        } catch (Exception e) {
            return false;
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
