package org.kish.database.table;

public class BambooPostsTable extends Table{
    public BambooPostsTable() {
        super("bamboo_posts",
                "CREATE TABLE `bamboo_posts`(\n" +
                        "    `bamboo_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `bamboo_title` TEXT NOT NULL,\n" +
                        "    `bamboo_content` MEDIUMTEXT NOT NULL,\n" +
                        "    `bamboo_author` VARCHAR(500) NOT NULL,\n" +
                        "    `bamboo_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    PRIMARY KEY(`bamboo_id`)\n" +
                        ") ENGINE = InnoDB CHARSET=utf8mb4 COLLATE utf8mb4_general_ci;");
    }
}
