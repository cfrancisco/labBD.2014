
package aula05.oracleinterface;

import java.awt.GridLayout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * SCC-0241 - Laboratório de Bases de Dados
 * Exercício Prático 5
 * @author Rodrigo de Freitas Pereira 7573472
 */
public class DBFuncionalidades {

    Connection connection;
    Statement stmt;
    ResultSet rs;
    JTextArea jtAreaDeStatus;

    public DBFuncionalidades(JTextArea jtaTextArea) {
        jtAreaDeStatus = jtaTextArea;
    }

    public boolean conectar() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = DriverManager.getConnection(
                    "jdbc:oracle:thin:@grad.icmc.usp.br:15214:orcl14","7573472","a");
            return true;
        } catch (ClassNotFoundException ex) {
            jtAreaDeStatus.setText("Problema: verifique o driver do banco de dados");
        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Problema: verifique seu usuário e senha");
        }
        return false;
    }

    // Recuperar nomes das tabelas para popular JComboBox
    public void pegarNomesDeTabelas(JComboBox jc) {
        String s = "";
        try {
            s = "SELECT table_name FROM user_tables";
            stmt = connection.createStatement();
            rs = stmt.executeQuery(s);
            while (rs.next()) {
                jc.addItem(rs.getString("table_name"));
            }
            stmt.close();
        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + s + "\"");
        }
    }

    //Popular JTable com dados de uma dada tabela
    public void exibeDados(JTable tATable, String sTableName) {
        // String para armazenar query
        String query = "";

        // Iterador
        int i;

        // TableModel para atualização dos dados na tabela
        DefaultTableModel tm = new DefaultTableModel();

        try {
            // Consultar nomes dos atributos da tabela no dicionário de dados
            stmt = connection.createStatement();
            query = "SELECT * FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + sTableName + "'";
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                tm.addColumn(rs.getString(2));
            }

            // Consultar dados
            query = "SELECT * FROM " + sTableName;
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                // Recuperar linha da tabela
                Vector<String> row = new Vector<String>();
                for (i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    row.add(rs.getString(i));
                }

                // Adicionar linha ao modelo
                tm.addRow(row);
            }

            //Atualizar tabela
            tATable.setModel(tm);

        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: " + query + "");
            ex.printStackTrace();
        }

    }
    
    // Criar campos para inserção de dados de acordo com uma dada tabela
    public void exibeRotulos(JPanel pInsert, String tableName) {
        ResultSet rs2;
        ResultSet rs3;
        String query = "";
        Statement stmt2;
        Statement stmt3;
        String stringCheckConstraint;
        String checkAux;
        String stringCheck[];

        boolean isText;

        try {
            int qtdColumns;

            // Consultar quantidade de atributos
            stmt = connection.createStatement();

            query = "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName + "'";
            rs = stmt.executeQuery(query);
            rs.next();
            qtdColumns = rs.getInt(1);

            // Consultar nomes dos atributos da tabela no dicionário de dados
            //stmt = connection.createStatement();
            query = "SELECT * FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName + "'";
            rs = stmt.executeQuery(query);

            //Limpar area de contexto (JPanel)
            pInsert.removeAll();

            pInsert.setLayout(new GridLayout(qtdColumns, 2, 10, 5));
            stmt2 = connection.createStatement();
            stmt3 = connection.createStatement();
           
            //Para cada coluna, verificar e o atributo em questão
            //é chave estrangeira ou possui restrição CHECK
            while (rs.next()) {
                isText = true;
                pInsert.add(new JLabel(rs.getString(2)));

                // Verificar se o atributo é uma chave estrangeira
                query = "SELECT C1.TABLE_NAME AS TAB_FK,C1.COLUMN_NAME AS FK,C3.TABLE_NAME AS TAB_PK,C3.COLUMN_NAME AS PK FROM  \n"
                        + "  ALL_CONS_COLUMNS C1 JOIN USER_CONSTRAINTS C2 ON C1.CONSTRAINT_NAME = C2.CONSTRAINT_NAME\n"
                        + "  JOIN ALL_CONS_COLUMNS C3 ON C2.R_CONSTRAINT_NAME = C3.CONSTRAINT_NAME \n"
                        + "  WHERE C1.POSITION = C3.POSITION AND C1.COLUMN_NAME = '" + rs.getString(2) + "' AND C1.TABLE_NAME = '" + tableName + "'";
                rs2 = stmt2.executeQuery(query);

                //Se atributo é uma FK, então criar um JComboBox populado
                //valores da respectiva PK
                if (rs2.next()) {
                    query = "SELECT  DISTINCT " + rs2.getString(4) + " FROM " + rs2.getString(3);

                    rs3 = stmt3.executeQuery(query);
                    JComboBox jcFk = new JComboBox();
                    jcFk.setName(rs.getString(2));
                    pInsert.add(jcFk);

                    while (rs3.next()) {
                        jcFk.addItem(rs3.getString(rs2.getString("PK")));
                    }
                    isText = false;
                }

                // Verificar se o atributo possui restricao CHECK 
                query = "SELECT CONSTRAINT_NAME,SEARCH_CONDITION  FROM  USER_CONSTRAINTS WHERE OWNER = '7573472'"
                        + "AND CONSTRAINT_NAME LIKE '%CHECK" + rs.getString(2) + "%' AND TABLE_NAME = '" + tableName + "'";
                rs2 = stmt2.executeQuery(query);

                if (rs2.next()) {
                    stringCheckConstraint = rs2.getString(2);
                    if (stringCheckConstraint.contains("IN")) {
                        checkAux = stringCheckConstraint.substring(stringCheckConstraint.indexOf("(") + 1,
                                stringCheckConstraint.indexOf(")"));
                        //extrair possiveis valores 
                        checkAux = checkAux.replaceAll("'", "");
                        stringCheck = checkAux.split(",");

                        //Popular JComboBox com os possíveis valores
                        JComboBox jcCheck = new JComboBox();
                        jcCheck.setName(rs.getString(2));
                        pInsert.add(jcCheck);

                        for (String stringCheck1 : stringCheck) {
                            jcCheck.addItem(stringCheck1);
                        }

                        isText = false;
                    }
                }

                // Inserir JTextField caso atributo não for chave estrangeira ou possuir restricao  CHECK
                // Mas se for BLOB deixar desabilitado
                if (isText) {
                    JTextField tAttri = new JTextField();
                    tAttri.setName(rs.getString(2) + "_" + rs.getString(3));

                    if (rs.getString(3).equals("BLOB")) {
                        tAttri.setEnabled(false);
                    }

                    pInsert.add(tAttri);
                }
            }

        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: " + query + "");
            ex.printStackTrace();
        }
    }

    //Mostrar metadados de uma data tabela
    public void mostrarMetaDados(String tableName) {
        // jtAreaDeStatus
        String query = "SELECT * FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName + "'";
        String msg = "\t\t" + tableName;
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(query);

            while (rs.next()) {
                msg += "\n" + rs.getString("COLUMN_ID") + "\t" + rs.getString("COLUMN_NAME") + "\t\t\t" + rs.getString("DATA_TYPE") + "\t" + rs.getString("NULLABLE");
            }

            jtAreaDeStatus.setText(msg);

        } catch (SQLException ex) {
            jtAreaDeStatus.setText(ex.toString());
            ex.printStackTrace();
        }
    }

    public void insertSQL(String insert) {
        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(insert);
        } catch (SQLException ex) {
            ex.printStackTrace();
            jtAreaDeStatus.setText(ex.toString());
        }
    }

    public void gerarDDL(JTextArea jta, String user, String pass) {
        String query;
        jta.setText("");
        Connection conn;
        Statement stmtAux;
        ResultSet rsAux;
        
        try {

            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@grad.icmc.usp.br:15212:orcl",user, pass);

            //Definir parâmetros para geração do DDL, desabilitar comandos sobre
            //armazenamento, chave estrangeira, e adicionar ';' depois de cada
            //comando
            CallableStatement cs = conn.prepareCall("{call dbms_metadata.set_transform_param(dbms_metadata.session_transform,'STORAGE',false)}");
            cs.execute();
            cs = conn.prepareCall("{call dbms_metadata.set_transform_param(dbms_metadata.session_transform,'SEGMENT_ATTRIBUTES',false)}");
            cs.execute();
            cs = conn.prepareCall("{call dbms_metadata.set_transform_param(dbms_metadata.session_transform,'SQLTERMINATOR',true)}");
            cs.execute();
            cs = conn.prepareCall("{call dbms_metadata.set_transform_param(dbms_metadata.session_transform,'REF_CONSTRAINTS',false)}");
            cs.execute();

            //Criar DDL de cada tabela
            stmt = conn.createStatement();
            stmtAux = conn.createStatement();
            query = "SELECT table_name FROM user_tables";
            rs = stmt.executeQuery(query);
            while(rs.next()){
                 query = "SELECT dbms_metadata.get_ddl('TABLE', '"+rs.getString(1)+"','"+user+"') FROM DUAL";
                 rsAux = stmtAux.executeQuery(query);
                 rsAux.next();
                 jta.append(rsAux.getString(1));
            }
            
 
            // Adicionar DDL das FKs
            query = "SELECT dbms_metadata.get_ddl('REF_CONSTRAINT', c.constraint_name) FROM USER_CONSTRAINTS c WHERE c.constraint_type = 'R'";
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                jta.append(rs.getString(1));
            }

            jtAreaDeStatus.setText("DDL Gerado");
            
        } catch (SQLException ex) {
            jtAreaDeStatus.setText(ex.toString());
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
               jtAreaDeStatus.setText("Problema: verifique o driver do banco de dados");
           }
    }

}
