package org.kish.database.table;

public class Table {
    private final String tableName, createQuery;

    public Table(){
        this.tableName = "";
        this.createQuery = "";
    }

    public Table(String tableName, String createQuery){
        this.tableName = tableName;
        this.createQuery = createQuery;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCreateQuery() {
        return createQuery;
    }
}
