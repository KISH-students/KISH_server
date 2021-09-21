package org.kish.database.table;

public class BambooPostLikesTable extends Table{
    public BambooPostLikesTable() {
        super("bamboo_post_likes",
                "CREATE TABLE `bamboo_post_likes`(\n" +
                        "    `like_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `post_id` INT NOT NULL,\n" +
                        "    `owner_id` VARCHAR(500) NOT NULL,\n" +
                        "    PRIMARY KEY(`like_id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
