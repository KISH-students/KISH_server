package org.kish.database.table;

public class AdminTable extends Table{
    public AdminTable() {
        super("kish_admin",
                "CREATE TABLE `kish_admin` (" +
                        "  `id` INT NOT NULL AUTO_INCREMENT, " +
                        "  `seq` VARCHAR(500) NOT NULL, " +
                        "  PRIMARY KEY (`id`)" +
                        ") ENGINE = InnoDB;");
    }
}
