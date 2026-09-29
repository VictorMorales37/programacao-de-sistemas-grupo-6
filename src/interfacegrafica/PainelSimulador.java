package interfacegrafica;

import cpu.*;
import cpu.instrucoes.Instrucao;
import entrada.*;
import memoria.Palavra;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Carrega código de máquina, executa o programa e permite inspecionar seu estado. */
public final class PainelSimulador extends JPanel {
    private final JTextArea editor = new JTextArea();
    private final JLabel estado = new JLabel("Pronto");
    private final JLabel proximaInstrucao = new JLabel(" ");
    private final JLabel condicao = new JLabel(" ");
    private final JTextField enderecoMemoria = new JTextField("000000", 8);
    private final JButton botaoPasso = new JButton("Passo");
    private final JButton botaoExecutar = new JButton("Executar");
    private final Timer temporizadorExecucao = new Timer(10, evento -> executarLote());
    private static final int LIMITE_EXECUCAO = 100000;
    private int passosExecucao;
    private final DefaultTableModel modeloRegistradores = criarModeloTabela("Registrador", "Bits", "Hexadecimal", "Com sinal / bruto");
    private final DefaultTableModel modeloMemoria = criarModeloTabela("Endereço da palavra", "Hexadecimal", "Com sinal");
    private ImagemPrograma imagem;
    private CPU maquina;
    private int inicioMemoria;
    private boolean falhou;

    private static DefaultTableModel criarModeloTabela(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
    }

    private static JScrollPane criarTabelaComRolagem(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(24);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setColumnHeaderView(tabela.getTableHeader());
        return rolagem;
    }

    public PainelSimulador() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JButton abrir = new JButton("Abrir");
        JButton carregar = new JButton("Carregar / reiniciar");
        JButton visualizar = new JButton("Visualizar");
        JPanel barraFerramentas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraFerramentas.add(abrir);
        barraFerramentas.add(carregar);
        barraFerramentas.add(botaoPasso);
        barraFerramentas.add(botaoExecutar);

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.add(barraFerramentas, BorderLayout.NORTH);
        cabecalho.add(estado, BorderLayout.SOUTH);
        add(cabecalho, BorderLayout.NORTH);

        JScrollPane painelFonte = new JScrollPane(editor);
        painelFonte.setBorder(BorderFactory.createTitledBorder("Código de máquina"));

        JPanel painelEstado = new JPanel(new BorderLayout(5, 5));
        JScrollPane visaoRegistradores = criarTabelaComRolagem(modeloRegistradores);
        visaoRegistradores.setPreferredSize(new Dimension(450, 250));
        painelEstado.add(visaoRegistradores, BorderLayout.NORTH);

        JPanel visaoMemoria = new JPanel(new BorderLayout());
        JPanel barraEndereco = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barraEndereco.add(new JLabel("Endereço de memória (hexadecimal):"));
        barraEndereco.add(enderecoMemoria);
        barraEndereco.add(visualizar);
        visaoMemoria.add(barraEndereco, BorderLayout.NORTH);
        visaoMemoria.add(criarTabelaComRolagem(modeloMemoria), BorderLayout.CENTER);
        painelEstado.add(visaoMemoria, BorderLayout.CENTER);

        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelFonte, painelEstado);
        divisor.setDividerLocation(510);
        divisor.setResizeWeight(0.5);
        add(divisor, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new GridLayout(2, 1, 0, 5));
        rodape.add(proximaInstrucao);
        rodape.add(condicao);
        add(rodape, BorderLayout.SOUTH);

        carregar.addActionListener(evento -> carregarFonte(editor.getText()));
        botaoPasso.addActionListener(evento -> executarPasso());
        botaoExecutar.addActionListener(evento -> {
            if (temporizadorExecucao.isRunning()) {
                temporizadorExecucao.stop();
                estado.setText("Pausado");
                atualizar();
            } else {
                executarTudo();
            }
        });

        abrir.addActionListener(evento -> {
            if (temporizadorExecucao.isRunning()) {
                temporizadorExecucao.stop();
                estado.setText("Pausado");
                atualizar();
            }
            JFileChooser seletor = new JFileChooser();
            if (seletor.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    carregarFonte(Files.readString(seletor.getSelectedFile().toPath()));
                } catch (Exception erro) {
                    estado.setText("Erro: " + erro.getMessage());
                }
            }
        });

        visualizar.addActionListener(evento -> {
            try {
                int endereco = CarregadorPrograma.endereco(enderecoMemoria.getText().trim());
                maquina.validarEndereco(endereco);
                inicioMemoria = endereco;
                atualizar();
            } catch (Exception erro) {
                estado.setText("Erro: " + erro.getMessage());
            }
        });

        try (var fluxo = getClass().getResourceAsStream("/example.sic")) {
            if (fluxo == null) throw new IllegalStateException("Recurso example.sic não encontrado");
            carregarFonte(new String(fluxo.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception erro) {
            estado.setText("Erro: " + erro.getMessage());
            botaoPasso.setEnabled(false);
            botaoExecutar.setEnabled(false);
        }
    }

    public void carregarFonte(String fonte) {
        temporizadorExecucao.stop();
        try {
            ImagemPrograma imagemCarregada = new CarregadorPrograma().analisar(fonte);
            CPU maquinaCarregada = imagemCarregada.criarCPU();
            imagem = imagemCarregada;
            maquina = maquinaCarregada;
            falhou = false;
            inicioMemoria = 0;
            editor.setText(fonte);
            editor.setCaretPosition(0);
            estado.setText("Carregado");
            atualizar();
        } catch (Exception erro) {
            estado.setText("Erro: " + erro.getMessage());
            if (maquina != null) atualizar();
        }
    }

    public void executarPasso() {
        if (temporizadorExecucao.isRunning() || !podeExecutar()) return;
        executarInstrucao();
        atualizar();
    }

    private boolean podeExecutar() {
        return maquina != null && !falhou && !imagem.estaConcluido(maquina);
    }

    private void executarInstrucao() {
        try {
            imagem.validarProximaInstrucao(maquina);
            Instrucao instrucao = maquina.executarPasso();
            estado.setText(imagem.estaConcluido(maquina)
                ? "Concluído em STOP"
                : "Executada " + instrucao.codigoOperacao());
        } catch (Exception erro) {
            falhou = true;
            estado.setText("Erro: " + erro.getMessage());
        }
    }

    public void executarTudo() {
        if (temporizadorExecucao.isRunning() || !podeExecutar()) return;
        passosExecucao = 0;
        estado.setText("Em execução");
        temporizadorExecucao.start();
        atualizar();
    }

    private void executarLote() {
        // Lotes limitados mantêm a interface responsiva durante a execução contínua.
        for (int indice = 0; indice < 100 && podeExecutar(); indice++) {
            if (passosExecucao >= LIMITE_EXECUCAO) {
                temporizadorExecucao.stop();
                estado.setText("Pausado: limite de 100000 instruções atingido. Verifique se há um laço infinito.");
                break;
            }
            executarInstrucao();
            passosExecucao++;
        }
        if (!podeExecutar()) temporizadorExecucao.stop();
        atualizar();
    }

    private void atualizar() {
        modeloRegistradores.setRowCount(0);
        for (Registrador registrador : maquina.obterRegistradores().todos()) {
            modeloRegistradores.addRow(new Object[]{registrador.obterIdentificador(), registrador.obterLarguraBits(),
                String.format("%0" + registrador.obterLarguraBits() / 4 + "X", registrador.obterValor()),
                registrador.obterLarguraBits() == 24 ? Palavra.comSinal(registrador.obterValor()) : registrador.obterValor()});
        }

        modeloMemoria.setRowCount(0);
        enderecoMemoria.setText(String.format("%06X", inicioMemoria));
        for (int endereco = inicioMemoria; endereco < inicioMemoria + 32; endereco++) {
            if (maquina.enderecoEmBytes(endereco) > maquina.obterMemoria().obterTamanho() - 3) break;
            int valor = maquina.lerPalavra(endereco);
            modeloMemoria.addRow(new Object[]{String.format("%06X", endereco),
                String.format("%06X", valor), Palavra.comSinal(valor)});
        }

        if (imagem.estaConcluido(maquina)) {
            proximaInstrucao.setText("Próxima: STOP");
        } else {
            try {
                Instrucao instrucao = maquina.consultarInstrucao();
                proximaInstrucao.setText(String.format("Próxima: %06X  %s  %s  formato %d  %s",
                    instrucao.endereco(), instrucao.hexadecimal(), instrucao.codigoOperacao(), instrucao.formato(), instrucao.indicadores()));
            } catch (Exception erro) {
                proximaInstrucao.setText("Próxima: " + erro.getMessage());
            }
        }

        String codigoCondicao;
        try {
            codigoCondicao = maquina.obterRegistradores().obterSW().obterCodigoCondicao().name();
        } catch (IllegalStateException erro) {
            codigoCondicao = "RESERVADO";
        }
        condicao.setText("CC: " + codigoCondicao + "   |   Ciclos: " + maquina.obterCiclos());
        botaoPasso.setEnabled(podeExecutar() && !temporizadorExecucao.isRunning());
        botaoExecutar.setText(temporizadorExecucao.isRunning() ? "Pausar" : "Executar");
        botaoExecutar.setEnabled(podeExecutar());
    }

    public CPU obterCPU() { return maquina; }
    public JButton obterBotaoPasso() { return botaoPasso; }
    public JButton obterBotaoExecutar() { return botaoExecutar; }
    public boolean estaExecutando() { return temporizadorExecucao.isRunning(); }
    public String obterEstado() { return estado.getText(); }
}
