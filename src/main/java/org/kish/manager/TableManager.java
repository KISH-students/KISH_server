package org.kish.manager;

import org.kish.KishServer;
import org.kish.MainLogger;
import org.kish.database.table.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.LinkedHashMap;

@Component
public class TableManager{
    private LinkedHashMap<String, org.kish.database.table.Table> tables = new LinkedHashMap<>();
    private JdbcTemplate jdbcTemplate;

    public TableManager(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        KishServer.tableManager = this;

        this.addTable(new PostTable());
        this.addTable(new AdminTable());
        this.addTable(new NotificationTable());
        this.addTable(new CalendarTable());
        this.addTable(new ExamTable());
        this.addTable(new LunchTable());
        this.addTable(new KishMenuInfoTable());
        this.addTable(new KishUsersTable());
        this.addTable(new BambooPostsTable());
        this.addTable(new BambooCommentsTable());
        this.addTable(new BambooPostLikesTable());
        this.addTable(new BambooCommentLikesTable());
        this.addTable(new BambooPostBackupTable());
        this.addTable(new BambooCommentBackupTable());
    }

    public void checkAllTable(){
        for (Table table : tables.values()) {
            this.checkTable(table);
        }
    }

    public void checkTable(Table table){
        MainLogger.info("DB 테이블 확인 중 : " + table.getTableName());
        String tableExitQuery = "SHOW TABLES LIKE '" + table.getTableName() + "'";

        if(jdbcTemplate.queryForList(tableExitQuery).size() < 1){
            MainLogger.info("새 table 생성 중 : " + table.getTableName());
            jdbcTemplate.execute(table.getCreateQuery());
        }
    }

    public void addTable(Table table){
        this.tables.put(table.getTableName(), table);
    }

    public void removeTable(Table table){
        this.tables.remove(table.getTableName());
    }
}
