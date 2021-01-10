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
    }

    public void checkAllTable(){
        for (Table table : tables.values()) {
            this.checkTable(table);
        }
    }

    public void checkTable(Table table){
        MainLogger.info("DB 테이블 확인 중 : " + table.getName());
        String tableExitQuery = "SHOW TABLES LIKE '" + table.getName() + "'";

        if(jdbcTemplate.queryForList(tableExitQuery).size() < 1){
            MainLogger.info("새 table 생성 중 : " + table.getName());
            jdbcTemplate.execute(table.getCreateQuery());
        }
    }

    public void addTable(Table table){
        this.tables.put(table.getName(), table);
    }

    public void removeTable(Table table){
        this.tables.remove(table.getName());
    }
}
