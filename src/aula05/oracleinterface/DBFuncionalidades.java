package aula05.oracleinterface;

import java.awt.GridLayout;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import oracle.jdbc.OracleTypes;
import oracle.sql.ArrayDescriptor;
import oracle.sql.ARRAY;

/**
 * SCC-0241 - Laboratório de Bases de Dados Projeto Final LAB BD
 *
 * @author Rodrigo de Freitas Pereira 7573472
 * @author Francisco Cabelo 7277652
 */
public class DBFuncionalidades {

    Connection connection;
    Statement stmt;
    ResultSet rs;
    JTextArea jtAreaDeStatus;
    private CallableStatement comando;

    public DBFuncionalidades(JTextArea jtaTextArea) {
        jtAreaDeStatus = jtaTextArea;
    }

    public boolean conectar() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = DriverManager.getConnection(
                    "jdbc:oracle:thin:@grad.icmc.usp.br:15214:orcl14", "7573472", "a");
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
        try {
            ResultSet cursor = SelectGenerico("user_tables", null, null, null);
            while (cursor.next()) {
                jc.addItem(cursor.getString("table_name"));
            }
        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
        }
    }

    //Mostrar metadados de uma tabela no jtAreaDeStatus
    public void mostrarMetaDados(String tableName) {
        String[] wcampos = new String[1];
        String[] wvalores = new String[1];
        wcampos[0] = "TABLE_NAME";
        wvalores[0] = tableName;
        String msg = "\t\t" + tableName;
        try {
            ResultSet rs = SelectGenerico("USER_TAB_COLUMNS", null, wcampos, wvalores);
            while (rs.next()) {
                msg += "\n" + rs.getString("COLUMN_ID") + "\t" + rs.getString("COLUMN_NAME") + "\t\t\t" + rs.getString("DATA_TYPE") + "\t" + rs.getString("NULLABLE");
            }
            jtAreaDeStatus.setText(msg);
        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
        }
    }

    public String[] nomeColunas(String sTableName) {
        String[] wcampos = new String[1];
        String[] wvalores = new String[1];
        String[] ncampos = new String[1];
        wcampos[0] = "TABLE_NAME";
        wvalores[0] = sTableName;
        ncampos[0] = "COLUMN_NAME";

        List<String> campos = new ArrayList();

        ResultSet rs = SelectGenerico("USER_TAB_COLUMNS", ncampos, wcampos, wvalores);

        try {
            while (rs.next()) {
                 campos.add(rs.getString(1));
            }

        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: " + ex.getMessage());
            ex.printStackTrace();
        }
        return campos.toArray(new String[campos.size()]);
    }

    //Popular JTable com dados de uma dada tabela
    public void exibeDados(JTable tATable, String sTableName) {
        int i;
        // TableModel para atualização dos dados na tabela
        DefaultTableModel tm = new DefaultTableModel();
        try {
            String[] wcampos = new String[1];
            String[] wvalores = new String[1];
            wcampos[0] = "TABLE_NAME";
            wvalores[0] = sTableName;
            // Consultar nomes dos atributos da tabela no dicionário de dados
            ResultSet rs = SelectGenerico("USER_TAB_COLUMNS", null, wcampos, wvalores);
            while (rs.next()) {
                tm.addColumn(rs.getString(2));
            }

            // Consultar dados da tabela
            rs = SelectGenerico(sTableName, null, null, null);
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
            jtAreaDeStatus.setText("Erro na consulta: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Criar campos para inserção de dados de acordo com uma dada tabela
    public void exibeRotulos(JPanel pInsert, String tableName) {
        ResultSet rs, rs2, rs3;
        String stringCheckConstraint, checkAux, stringCheck[];
        String[] campos = new String[1];
        String[] wcampos = new String[1];
        String[] wvalores = new String[1];
        boolean isText;
        int qtdColumns;

        try {
            campos[0] = "COUNT(*)";
            wcampos[0] = "TABLE_NAME";
            wvalores[0] = tableName;

            // Consultar quantidade de atributos
            rs = SelectGenerico("USER_TAB_COLUMNS", campos, wcampos, wvalores);
            rs.next();
            qtdColumns = rs.getInt(1);

            // Consultar nomes dos atributos da tabela no dicionário de dados
            rs = SelectGenerico("USER_TAB_COLUMNS", null, wcampos, wvalores);

            //Limpar area de contexto (JPanel)
            pInsert.removeAll();

            pInsert.setLayout(new GridLayout(qtdColumns, 2, 10, 5));

            //Para cada coluna, verificar e o atributo em questão
            //é chave estrangeira ou possui restrição CHECK
            while (rs.next()) {
                isText = true;
                pInsert.add(new JLabel(rs.getString(2)));

                // Verificar se o atributo é uma chave estrangeira
                rs2 = SelectVerificaFK(rs.getString(2), tableName);
                // System.out.println(rs.getString(2)+ "  " +tableName);
                //Se atributo é uma FK, então criar um JComboBox populado
                //valores da respectiva PK
                if (rs2 == null) {
                    return;
                }
                if (rs2.next()) {
                    campos[0] = "DISTINCT " + rs2.getString(4);
                    rs3 = SelectGenerico(rs2.getString(3), campos, null, null);
                    JComboBox jcFk = new JComboBox();
                    jcFk.setName(rs.getString(2));
                    pInsert.add(jcFk);

                    while (rs3.next()) {
                        jcFk.addItem(rs3.getString(rs2.getString("PK")));
                    }
                    isText = false;
                }

                // Verificar se o atributo possui restricao CHECK 
                rs2 = SelectVerificaCheck(rs.getString(2), tableName);
                if (rs2 == null) {
                    return;
                }
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
                // Mas se for BLOB deixar desabilitado - BLOB = JFileChooser
                if (isText) {
                    JTextField tAttri = new JTextField();
                    tAttri.setName(rs.getString(2));
                    // System.out.println(rs.getString(2) + "_" + rs.getString(3));
                    if (rs.getString(3).equals("BLOB")) {
                        tAttri.setEnabled(false);
                    }

                    pInsert.add(tAttri);
                }
            }

        } catch (SQLException ex) {
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
        }
    }

    public void gerarDDL(JTextArea jta, String user, String pass) {
        String query;
        jta.setText("");
        Connection conn;
        Statement stmtAux;
        ResultSet rsAux;
        String[] wcampos = new String[1];
        String[] wvalores = new String[1];

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@grad.icmc.usp.br:15212:orcl", user, pass);

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
            String[] campos = new String[1];
            campos[0] = "table_name";
            rs = SelectGenerico("user_tables", campos, null, null);

            while (rs.next()) {
                campos[0] = "dbms_metadata.get_ddl('TABLE', '" + rs.getString(1) + "','" + user + "')";
                rsAux = SelectGenerico("DUAL", campos, null, null);
                rsAux.next();
                jta.append(rsAux.getString(1));
            }

            // Adicionar DDL das FKs
            campos[0] = "dbms_metadata.get_ddl('REF_CONSTRAINT', c.constraint_name)";
            wcampos[0] = "c.constraint_type";
            wvalores[0] = "R";
            rs = SelectGenerico("USER_CONSTRAINTS c", campos, null, null);
//            query = "SELECT dbms_metadata.get_ddl('REF_CONSTRAINT', c.constraint_name) FROM USER_CONSTRAINTS c WHERE c.constraint_type = 'R'";
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

    int InsertGenerico(String nomeTabela, String[] campos, String[] valores) {
        try {
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor("T_ATRIBUTO", connection);
            ARRAY wcampos = new ARRAY(descriptor, connection, campos);
            ARRAY wvalores = new ARRAY(descriptor, connection, valores);
            // 1. Tabela alvo
            // 2. Vetor de campos a ser inseridos
            // 3. Vetor de valores a ser inseridos
            comando = connection.prepareCall("{ call projeto_api.insercao_tabela(?,?,?) }");
            comando.setString(1, nomeTabela);
            comando.setArray(2, wcampos);
            comando.setArray(3, wvalores);
            comando.execute();

        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na inserção: \"" + ex.getMessage() + "\"");
            return Mensagens.INSERT_ERROR;
        }
        return Mensagens.INSERT_SUCCESS;

    }
    
    int AtualizarGenerico(String nomeTabela,String[] campos, String[] valoresAntigos, String[] valoresNovos){
        
        try{
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor("T_ATRIBUTO", connection);
            ARRAY wcampos = new ARRAY(descriptor, connection, campos);
            ARRAY wvalores = new ARRAY(descriptor, connection, valoresAntigos);
            ARRAY uvalores = new ARRAY(descriptor, connection, valoresNovos);
            
            comando = connection.prepareCall("{ call projeto_api.update_tabela(?,?,?,?,?) }");
            comando.setString(1, nomeTabela);
            comando.setArray(2, wcampos);
            comando.setArray(3, uvalores);
            comando.setArray(4, wcampos);
            comando.setArray(5, wvalores);
            comando.execute();
            
        }catch(SQLException ex){
            jtAreaDeStatus.setText("Erro na atualização: \"" + ex.getMessage() + "\"");
            return Mensagens.UPDATE_ERROR;
        }
        
        return Mensagens.UPDATE_SUCCESS;
    }

    int DeleteGenerico(String nomeTabela, String[] campos, String[] valores) {
        try {
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor("T_ATRIBUTO", connection);
            ARRAY wcampos = new ARRAY(descriptor, connection, campos);
            ARRAY wvalores = new ARRAY(descriptor, connection, valores);
            // 1. Tabela alvo
            // 2. Vetor de campos a ser inseridos
            // 3. Vetor de valores a ser inseridos
            comando = connection.prepareCall("{ call projeto_api.delete_tabela(?,?,?) }");
            comando.setString(1, nomeTabela);
            comando.setArray(2, wcampos);
            comando.setArray(3, wvalores);
            comando.execute();

        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
            return Mensagens.INSERT_ERROR;
        }
        return Mensagens.INSERT_SUCCESS;

    }
    
    public ResultSet SelectGenerico(String nomeTabela, String[] t_campos, String[] tw_campos, String[] tw_valores) {
        try {
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor("T_ATRIBUTO", connection);
            ARRAY campos = new ARRAY(descriptor, connection, t_campos);
            ARRAY wcampos = new ARRAY(descriptor, connection, tw_campos);
            ARRAY wvalores = new ARRAY(descriptor, connection, tw_valores);

            // 1. cursor de retorno do PLSQL
            // 2. Tabela alvo
            // 3. Vetor de campos dos parametros Where
            // 4. Vetor de valores do where
            comando = connection.prepareCall("{ call projeto_api.select_tabela(?,?,?,?,?) }");
            comando.registerOutParameter(1, OracleTypes.CURSOR);
            comando.setString(2, nomeTabela);
            comando.setArray(3, campos);
            comando.setArray(4, wcampos);
            comando.setArray(5, wvalores);
            comando.execute();

            ResultSet cursor = (ResultSet) comando.getObject(1);
            return cursor;
        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
            return null;
        }
    }

    public ResultSet SelectVerificaFK(String nomeColuna, String nomeTabela) {
        try {
            // 1. cursor de retorno do PLSQL
            comando = connection.prepareCall("{ call projeto_api.verificar_fk(?,?,?) }");
            comando.registerOutParameter(1, OracleTypes.CURSOR);
            comando.setString(2, nomeTabela);
            comando.setString(3, nomeColuna);
            comando.execute();
            ResultSet cursor = (ResultSet) comando.getObject(1);
            return cursor;
        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
            return null;
        }
    }

    public ResultSet SelectVerificaCheck(String nomeColuna, String nomeTabela) {
        try {
            // 1. cursor de retorno do PLSQL
            comando = connection.prepareCall("{ call projeto_api.verificar_check(?,?,?) }");
            comando.registerOutParameter(1, OracleTypes.CURSOR);
            comando.setString(2, nomeTabela);
            comando.setString(3, nomeColuna);
            comando.execute();
            ResultSet cursor = (ResultSet) comando.getObject(1);
            return cursor;
        } catch (SQLException ex) {
            System.out.print(ex.getMessage());
            jtAreaDeStatus.setText("Erro na consulta: \"" + ex.getMessage() + "\"");
            return null;
        }
    }

}
