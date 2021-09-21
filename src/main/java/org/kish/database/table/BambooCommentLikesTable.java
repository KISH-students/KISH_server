package org.kish.database.table;

public class BambooCommentLikesTable extends Table{
    public BambooCommentLikesTable() {
        super("bamboo_comment_likes",
                "CREATE TABLE `bamboo_comment_likes`(\n" +
                        "    `like_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `comment_id` INT NOT NULL,\n" +
                        "    `owner_id` VARCHAR(500) NOT NULL,\n" +
                        "    PRIMARY KEY(`like_id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
