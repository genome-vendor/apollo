package apollo.dataadapter.chado.jdbc;

/**
 * Represents a single column in a relational database table.
 */
public class TableColumn {
  public String tableName;
  public String colName;
  
  public TableColumn(String tn, String cn) {
    this.tableName = tn;
    this.colName = cn;
  }
}
