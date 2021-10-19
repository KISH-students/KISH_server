package org.kish.database.table;

public class BambooNotificationTable extends Table{
    public BambooNotificationTable() {
        super("bamboo_notification", "CREATE TABLE `bamboo_notification`(\n" +
                "    `id` INT NOT NULL AUTO_INCREMENT,\n" +
                "    `post_id` INT NOT NULL,\n" +
                "    `comment_id` INT NOT NULL,\n" +
                "    `type` TEXT NOT NULL,\n" +
                "    `user` VARCHAR(500) NOT NULL,\n" +
                "    `title` TEXT NOT NULL,\n" +
                "    `content` TEXT NOT NULL,\n" +
                "    `date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, \n" +
                "    PRIMARY KEY(`id`)\n" +
                ") ENGINE = InnoDB;");
    }
}
