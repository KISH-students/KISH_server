package org.kish.database.table;

public class BambooPostRecommendsTable extends Table{
    public BambooPostRecommendsTable() {
        super("bamboo_post_recommends",
                "CREATE TABLE `bamboo_post_recommends`(\n" +
                        "    `recommend_id` INT NOT NULL AUTO_INCREMENT,\n" +
                        "    `post_id` INT NOT NULL,\n" +
                        "    `recommend_owner_id` VARCHAR(500) NOT NULL,\n" +
                        "    `recommend_value` BOOLEAN NOT NULL,\n" +
                        "    PRIMARY KEY(`recommend_id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
