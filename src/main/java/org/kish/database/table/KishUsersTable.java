package org.kish.database.table;

public class KishUsersTable extends Table{
    public KishUsersTable() {
        super("kish_users",
                "CREATE TABLE `kish_users`(\n" +
                        "    `id` INT NOT NULL AUTO_INCREMENT COMMENT 'PRIMARY용 id',\n" +
                        "    `user_id` VARCHAR(500) NOT NULL COMMENT '대출증 아이디',\n" +
                        "    `session` INT NOT NULL COMMENT '세션 키',\n" +
                        "    PRIMARY KEY(`id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
