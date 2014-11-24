package aula05.oracleinterface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * SCC-0241 - Laboratório de Bases de Dados
 * Exercício Prático 5
 * @author Rodrigo de Freitas Pereira 7573472
 */
public class JanelaPrincipal {

    //Janela principal
    JFrame j;

    //Painel para mostrar comboBox
    JPanel pPainelDeCima;

    //Painel para mostrar área de status
    JPanel pPainelDeBaixo;

    //ComboBox para seleção de tabelas
    JComboBox jc;

    //Área de status(Mostrar exceções SQL quando ocorrerem)
    JTextArea jtAreaDeStatus;

    //Painel tabular
    JTabbedPane tabbedPane;

    //Painel para exibição dos dados na tabela
    JPanel pPainelDeExibicaoDeDados;

    //Painel com scroll para exibição de dados na tabela
    JScrollPane jpPaineldeExibicaoDedados;

    //Tabela
    JTable jt;

    //Painel para inserção de dados
    JPanel pPainelDeInsercaoDeDados;
    JPanel pPainelGridInsert;

    //Botão para inserção de dados
    JButton btInsert;

    //Senha
    JPasswordField jpPassword;

    //Usuário
    JTextField jtUsuario;

    //JLabels para usuário e senha
    JLabel lPass;
    JLabel lUser;

    //JTextArea para mostrar DDL
    JTextArea jtaDDL;
    JScrollPane jspTA;
    
    //Butão para geração de DDL
    JButton btnDDL;
    
    //Painel para geração de DDL
    JPanel jpDDLlabelText;
    JPanel jpDDL;

    //Funcionalidades do banco
    DBFuncionalidades bd;

    public void ExibeJanelaPrincipal() {
        /*Janela*/
        j = new JFrame("ICMC-USP - SCC0241 - Pratica 5");
        j.setSize(700, 500);
        j.setLayout(new BorderLayout());
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /*Painel da parte superior (north) - com combobox e outras informações*/
        pPainelDeCima = new JPanel();
        j.add(pPainelDeCima, BorderLayout.NORTH);
        jc = new JComboBox();
        pPainelDeCima.add(jc);

        /*Painel da parte inferior (south) - com área de status*/
        pPainelDeBaixo = new JPanel();
        j.add(pPainelDeBaixo, BorderLayout.SOUTH);
        jtAreaDeStatus = new JTextArea();
        jtAreaDeStatus.setText("Aqui é sua área de status");
        pPainelDeBaixo.add(jtAreaDeStatus);

        /*Painel tabulado na parte central (CENTER)*/
        tabbedPane = new JTabbedPane();
        j.add(tabbedPane, BorderLayout.CENTER);

        /*Tab de exibicao*/
        pPainelDeExibicaoDeDados = new JPanel();
        pPainelDeExibicaoDeDados.setLayout(new GridLayout(1, 1));
        tabbedPane.add(pPainelDeExibicaoDeDados, "Exibição");

        /*Tabela de dados*/
        jt = new JTable();
        jpPaineldeExibicaoDedados = new JScrollPane(jt);
        pPainelDeExibicaoDeDados.add(jpPaineldeExibicaoDedados);

        /*Tab de inserção*/
        btInsert = new JButton("Inserir");
        pPainelGridInsert = new JPanel();
        pPainelGridInsert.setLayout(new GridLayout(1, 1));
        pPainelGridInsert.add(new JLabel("Selecione uma tabela para inserção"));

        pPainelDeInsercaoDeDados = new JPanel();
        pPainelDeInsercaoDeDados.setLayout(new BorderLayout());
        pPainelDeInsercaoDeDados.add(pPainelGridInsert, BorderLayout.NORTH);
        pPainelDeInsercaoDeDados.add(btInsert, BorderLayout.SOUTH);

        tabbedPane.add(pPainelDeInsercaoDeDados, "Inserção");

        //Componentes para geração de dll
        jpDDLlabelText = new JPanel();
        jpDDLlabelText.setLayout(new GridLayout(3, 2));

        lUser = new JLabel("Usuário");
        jpDDLlabelText.add(lUser);
        jtUsuario = new JTextField();
        jpDDLlabelText.add(jtUsuario);
        lPass = new JLabel("Senha");
        jpDDLlabelText.add(lPass);
        jpPassword = new JPasswordField(20);
        jpDDLlabelText.add(jpPassword);
        btnDDL = new JButton("Gerar DDL");
        jpDDLlabelText.add(btnDDL);

        jpDDL = new JPanel();
        jpDDL.setLayout(new BorderLayout());

        jtaDDL = new JTextArea();
        jpDDL.add(jpDDLlabelText, BorderLayout.NORTH);
        jspTA = new JScrollPane(jtaDDL);
        jpDDL.add(jspTA, BorderLayout.CENTER);

        tabbedPane.add(jpDDL, "Recuperar DDL");

        //Carregar nome das tabelas no JCombobox
        bd = new DBFuncionalidades(jtAreaDeStatus);
        if (bd.conectar()) {
            bd.pegarNomesDeTabelas(jc);
            bd.pegarNomesDeTabelas(jc);
        }

        //Definir eventos
        this.DefineEventos();
        j.setVisible(true);

    }

    private void DefineEventos() {
        //Carregar tabela com dados e criar campos para inserção
        jc.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JComboBox jcTemp = (JComboBox) e.getSource();

                        bd.mostrarMetaDados((String) jcTemp.getSelectedItem());
                        // Carregar tabela
                        bd.exibeDados(jt, (String) jcTemp.getSelectedItem());

                        // Carregar atributos para inserção de dados
                        bd.exibeRotulos(pPainelGridInsert, (String) jcTemp.getSelectedItem());
                    }
                });

        // Realizar INSERT
        btInsert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String componentType;
                String componentSplit[];

                String insert = "INSERT INTO " + jc.getSelectedItem() + " VALUES(";

                // Percorrer todos os componentes do JPanel para recuperar as entradas
                for (Component c : pPainelGridInsert.getComponents()) {
                    // System.out.println(c.getName()+" "+c.getClass().getName());
                    componentSplit = c.getClass().getName().split("\\.");
                    //System.out.println(componentSplit[componentSplit.length-1]);
                    componentType = componentSplit[componentSplit.length - 1];

                    switch (componentType) {
                        case "JComboBox":
                            JComboBox jc = (JComboBox) c;
                            insert += "'" + jc.getSelectedItem() + "',";
                            //System.out.println(jc.getSelectedItem());
                            break;

                        case "JTextField":
                            JTextField jt = (JTextField) c;

                            //se for BLOB entao jt vai estar disabled
                            if (jt.isEnabled()) {
                                if (jt.getName().contains("DATE")) {
                                    insert += "TO_DATE('" + jt.getText() + "','dd/mm/yyyy'),";
                                } else {
                                    insert += "'" + jt.getText() + "',";
                                }
                            } else {
                                insert += "EMPTY_BLOB(),";
                            }

                            break;
                    }

                }

                //substituir a  última vírgula por )
                insert = insert.substring(0, insert.length() - 1) + ")";
                bd.insertSQL(insert);
                bd.exibeDados(jt, (String) jc.getSelectedItem());
                System.out.println(insert);
            }
        });
    
         //Gerar DDL 
         btnDDL.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bd.gerarDDL(jtaDDL,jtUsuario.getText(),String.valueOf(jpPassword.getPassword()));
            }
         });
    }
    
    
}