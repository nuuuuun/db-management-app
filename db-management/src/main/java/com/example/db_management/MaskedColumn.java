package com.example.db_management;

import jakarta.persistence.*;

@Entity
@Table(name = "masked_columns")
public class MaskedColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tableName;
    private String columnName;
    private String role;

    public Long getId() { return id; }
    public String getTableName() { return tableName; }
    public String getColumnName() { return columnName; }
    public String getRole() { return role; }

    public void setTableName(String tableName) { this.tableName = tableName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public void setRole(String role) { this.role = role; }
}
