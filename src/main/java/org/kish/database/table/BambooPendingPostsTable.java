package org.kish.database.table;

// 아직 사용 안 합니다.
public class BambooPendingPostsTable extends Table{
    public BambooPendingPostsTable() {
        super("bamboo_pending_posts",
                "CREATE TABLE `bamboo_pending_posts`(\n" +
                        "    `bamboo_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `bamboo_title` TEXT NOT NULL,\n" +
                        "    `bamboo_content` MEDIUMTEXT NOT NULL,\n" +
                        "    `bamboo_author` VARCHAR(500) NOT NULL,\n" +
                        "    `bamboo_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    PRIMARY KEY(`bamboo_id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
