package org.kish.database.table;

public class BambooFacebookPostsTable extends Table{
    public BambooFacebookPostsTable() {
        super("bamboo_facebook_posts",
                "CREATE " +
                        "TABLE `bamboo_facebook_posts` " +
                        "( `bamboo_id` INT NOT NULL , " +
                        "`facebook_id` TINYTEXT NOT NULL , " +
                        "PRIMARY KEY (`bamboo_id`)" +
                        ") ENGINE = InnoDB");
    }
}
