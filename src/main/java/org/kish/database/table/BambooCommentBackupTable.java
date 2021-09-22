package org.kish.database.table;

public class BambooCommentBackupTable extends Table{
    public BambooCommentBackupTable() {
        super("bamboo_comment_backup",
                "CREATE TABLE `bamboo_comment_backup`(\n" +
                        "    `comment_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `post_id` INT NOT NULL COMMENT '댓글이 달린 게시글의 id',\n" +
                        "    `comment_content` MEDIUMTEXT NOT NULL COMMENT '댓글 내용',\n" +
                        "    `comment_parent_id` INT NOT NULL COMMENT '답글인 경우 답글의 부모 댓글 아이디',\n" +
                        "    `comment_author_id` VARCHAR(500) NOT NULL COMMENT '댓글 작성자',\n" +
                        "    `comment_author_displayname` VARCHAR(500) NOT NULL COMMENT '표시될 댓글 작성자 이름',\n" +
                        "    `comment_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    `is_reply` BOOLEAN NOT NULL,\n" +
                        "    `is_facebook_user` BOOLEAN NOT NULL,\n" +
                        "    PRIMARY KEY(`comment_id`)\n" +
                        ") ENGINE = InnoDB CHARSET=utf8mb4 COLLATE utf8mb4_general_ci;");
    }
}
