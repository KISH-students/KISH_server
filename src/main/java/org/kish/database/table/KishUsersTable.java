package org.kish.database.table;

public class KishUsersTable extends Table{
    public KishUsersTable() {
        super("kish_users",
                "CREATE TABLE `kish_users`(\n" +
                        "    `id` INT NOT NULL AUTO_INCREMENT COMMENT 'PRIMARY용 id',\n" +
                        "    `user_id` VARCHAR(500) NOT NULL COMMENT '대출증 아이디',\n" +
                        "    `fcm_token` TEXT NOT NULL COMMENT 'FCM 토큰',\n" +
                        "    `bamboo_noti` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '대나무숲 댓글 알림 허용 여부',\n" +
                        "    PRIMARY KEY(`id`)\n" +
                        ") ENGINE = InnoDB;");
    }
}
