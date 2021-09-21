package org.kish.database.table;

public class BambooPostBackupTable extends Table{
    public BambooPostBackupTable() {
        super("bamboo_post_backup",
                "CREATE TABLE `bamboo_post_backup`(\n" +
                        "    `bamboo_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `bamboo_content` MEDIUMTEXT NOT NULL,\n" +
                        "    `bamboo_author` VARCHAR(500) NOT NULL,\n" +
                        "    `bamboo_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                        "    PRIMARY KEY(`bamboo_id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
