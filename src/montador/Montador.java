package montador;

import java.util.ArrayList;
import java.util.List;

/**
 * Montador de dois passos para SIC/XE.
 * <p>
 * Passo 1: percorre o fonte, mantém o contador de posição (LC, em palavras de 3 bytes)
 * e constrói a tabela de símbolos. Passo 2: gera o código objeto (bytes) e a listagem.
 * Todos os endereços aqui são lógicos (em palavras); a conversão para endereço em bytes
 * (multiplicar por 3) é responsabilidade de quem consome (ex.: o carregador).
 * <p>
 * Unidades: instruções de formato 2 e 3 ocupam 1 palavra (a de formato 2 grava os 2 bytes
 * de código; o terceiro byte da palavra fica zero), a de formato 4 (prefixo '+') ocupa
 * 2 palavras (4 bytes; os bytes excedentes ficam zero), WORD/RESW ocupam
 * 1 palavra e RESB reserva ceil(n/3) palavras (n é contado em bytes e arredondado para cima).
 * <p>
 * Erros de montagem são ACUMULADOS em {@link ProgramaMontado#erros()}; nada é lançado.
 * O símbolo implícito STOP é sempre definido com a primeira palavra livre após o fim
 * (a LC final), de modo que programas são encerrados com {@code J STOP}.
 */
public final class Montador {

    private final List<ProgramaMontado.ErroMontagem> erros = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();
    private final TabelaSimbolos simbolos = new TabelaSimbolos();
    private final List<ProgramaMontado.LinhaListagem> listagem = new ArrayList<>();
    private final List<ProgramaMontado.Entrada> entradas = new ArrayList<>();
    private final List<LinhaProcessada> linhas = new ArrayList<>();

    private final record LinhaProcessada(Linha linha, String fonte, int endereco, int palavras, byte[] dadosByte) {}

    private int lc;
    private int lcInicial = 0;
    private boolean startPresente;
    private boolean endPresente;
    private Integer enderecoEntrada = null;
    private Integer baseAtual = null; // valor do BASE em vigor no passo 2 (null: nenhum BASE ativo)

    private Montador() {}

    /** Monta o fonte completo (linhas separadas por '\n'). */
    public static ProgramaMontado montar(String fonte) {
        Montador montador = new Montador();
        try {
            montador.executar(fonte);
        } catch (Exception interna) {
            montador.erros.add(new ProgramaMontado.ErroMontagem(0, "Falha interna de montagem: " + interna));
        }
        return new ProgramaMontado(montador.entradas, montador.enderecoEntradaPadrao(),
            montador.lc, montador.simbolos.listar(), montador.listagem, montador.erros, montador.avisos);
    }

    private int enderecoEntradaPadrao() {
        if (enderecoEntrada != null) return enderecoEntrada; // definido em END
        if (startPresente) return lcInicial;                 // padrão de START
        return 0;
    }

    // ------------------------------------------------------------------ passo 1

    private void executar(String fonte) {
        String[] textoLinhas = fonte.split("\n", -1);
        // Passo 1: parsing, LC e tabela de símbolos.
        for (int indice = 0; indice < textoLinhas.length; indice++) {
            Linha linha = Linha.ler(indice + 1, textoLinhas[indice]);
            if (linha.operador() == null) continue; // linha vazia ou só comentário: não entra na listagem
            if (endPresente) continue;              // tudo após END é ignorado
            processarPasso1(linha, textoLinhas[indice]);
        }
        if (linhas.isEmpty())
            erros.add(new ProgramaMontado.ErroMontagem(1, "Programa sem instruções ou diretivas"));
        // Símbolo implícito STOP: primeira palavra livre após o fim do programa (LC final).
        if (!simbolos.contem("STOP")) simbolos.inserir("STOP", lc);
        // Passo 2: geração de código e listagem.
        for (LinhaProcessada processada : linhas) gerar(processada);
    }

    private void processarPasso1(Linha linha, String fonte) {
        String operador = linha.operador();
        byte[] dadosByte = null;
        int palavras = 1; // avanço padrão para continuar a montagem quando houver erro
        TabelaDiretivas diretiva = TabelaDiretivas.de(operador);
        TabelaInstrucoes instrucao = TabelaInstrucoes.de(operador);

        if (diretiva != null) {
            if (!validarQuantidadeOperandos(linha, diretiva)) return;
            palavras = avancarDiretiva(linha, diretiva);
        } else {
            if (instrucao == null) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "Operador inválido: '" + operador + "' não é mnemônico nem diretiva"));
            } else {
                if (!validarOperandosInstrucao(linha, instrucao)) return;
                palavras = linha.formato4() ? 2 : 1; // formato 4 ocupa 2 palavras (4 bytes)
            }
        }
        if (diretiva == TabelaDiretivas.BYTE) {
            dadosByte = lerByte(linha, linha.operandos().get(0)); // parse único, reutilizado no passo 2
            if (dadosByte != null) palavras = (dadosByte.length + 2) / 3;
        }
        if (diretiva == TabelaDiretivas.START || diretiva == TabelaDiretivas.END)
            return; // START/END não ocupam espaço de código e não entram na listagem
        boolean semEspaco = diretiva != null
            && (diretiva == TabelaDiretivas.EQU || diretiva == TabelaDiretivas.ORG
                || diretiva == TabelaDiretivas.BASE);
        if (semEspaco) {
            // EQU/ORG/BASE não ocupam memória: entram na listagem só com o texto da linha.
            linhas.add(new LinhaProcessada(linha, fonte.trim(), -1, 0, null));
        } else {
            int enderecoDaLinha = lc;
            if (linha.rotulo() != null && !simbolos.inserir(linha.rotulo(), enderecoDaLinha))
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "Símbolo duplicado: '" + linha.rotulo() + "' já definido (novo endereço " + hex(enderecoDaLinha) + ")"));
            linhas.add(new LinhaProcessada(linha, fonte.trim(), enderecoDaLinha, palavras, dadosByte));
        }
        lc += palavras;
    }

    private boolean validarQuantidadeOperandos(Linha linha, TabelaDiretivas diretiva) {
        if (diretiva.aceitaQuantidade(linha.operandos().size())) return true;
        erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), diretiva.name() + " espera " +
            (diretiva.minOperandos() == diretiva.maxOperandos()
                ? diretiva.minOperandos() + " operando(s)"
                : "entre " + diretiva.minOperandos() + " e " + diretiva.maxOperandos() + " operando(s)") +
            "; recebeu " + linha.operandos().size()));
        return false;
    }

    /** Consumo de LC da diretiva; devolve o avanço (0 para START/END, 1 para WORD/RESW). */
    private int avancarDiretiva(Linha linha, TabelaDiretivas diretiva) {
        return switch (diretiva) {
            case START -> {
                if (startPresente || endPresente || !linhas.isEmpty()) {
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "START deve ser a primeira linha com operador do programa"));
                    yield 0;
                }
                if (linha.operandos().size() != 1) yield 0;
                Integer end = lerNumero(linha, linha.operandos().get(0), 0, 0xFFFFFF, "operando de START");
                if (end == null) yield 0;
                lc = end;
                lcInicial = end;
                startPresente = true;
                yield 0;
            }
            case END -> {
                endPresente = true;
                if (!linha.operandos().isEmpty())
                    enderecoEntrada = resolverEnderecoEntrada(linha, linha.operandos().get(0));
                yield 0;
            }
            case EQU -> {
                // Rótulo obrigatório; valor = número ou símbolo já definido (passo 1).
                if (linha.rotulo() == null) {
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "EQU exige um rótulo que recebe o valor do operando"));
                    yield 0;
                }
                Integer valor = resolverValor(linha, linha.operandos().get(0),
                    "Símbolo indefinido em EQU: '" + linha.operandos().get(0) +
                    "' (o valor de EQU pode ser número ou símbolo já definido)");
                if (valor != null && !simbolos.inserir(linha.rotulo(), valor))
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "Símbolo duplicado: '" + linha.rotulo() + "' já definido"));
                yield 0;
            }
            case ORG -> {
                Integer valor = resolverValor(linha, linha.operandos().get(0),
                    "Símbolo indefinido em ORG: '" + linha.operandos().get(0) + "'");
                if (valor != null) lc = valor;
                yield 0;
            }
            case BASE -> { /* o valor é avaliado no passo 2, quando a tabela está completa */ yield 0; }
            case WORD -> 1;
            // RESW reserva n palavras; RESB reserva n bytes: ceil(n/3) palavras.
            case RESW, RESB -> {
                Integer n = lerNumero(linha, linha.operandos().get(0), 0, 0xFFFFFF, "tamanho de " + diretiva.name());
                if (n == null) yield 1;
                if (diretiva == TabelaDiretivas.RESB && n % 3 != 0)
                    avisos.add("Linha " + linha.numeroLinha() + ": RESB " + linha.operandos().get(0) +
                        " reserva " + ((n + 2) / 3) + " palavras; " + (((n + 2) / 3) * 3 - n) +
                        " byte(s) não utilizados por causa do alinhamento em palavras");
                yield diretiva == TabelaDiretivas.RESW ? n : (n + 2) / 3;
            }
            case BYTE -> 1; // o avanço real é corrigido após o parse dos bytes
        };
    }

    private Integer resolverEnderecoEntrada(Linha linha, String operando) {
        Integer numero = tentarNumero(operando, 0, 0xFFFFFF);
        if (numero != null) return numero;
        Integer endereco = simbolos.consultar(operando);
        if (endereco == null) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                "Símbolo indefinido em END: '" + operando + "'"));
            return null;
        }
        return endereco;
    }

    /** Valor absoluto: número hexadecimal (0..0xFFFFFF) ou símbolo já definido; null com erro acumulado. */
    private Integer resolverValor(Linha linha, String texto, String mensagemIndefinido) {
        Integer numero = tentarNumero(texto, 0, 0xFFFFFF);
        if (numero != null) return numero;
        Integer endereco = simbolos.consultar(texto);
        if (endereco == null) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), mensagemIndefinido));
            return null;
        }
        return endereco;
    }

    /**
     * Operando de BYTE: X'...' (dígitos hexadecimais em pares) ou C'...' (caracteres UTF-8).
     * Devolve os bytes alinhados em palavras de 3 bytes (preenchimento com zero); null com erro.
     */
    private byte[] lerByte(Linha linha, String operando) {
        var formatador = java.util.regex.Pattern.compile("(?i)^([CX])'([^']*)'$");
        var ajustador = formatador.matcher(operando == null ? "" : operando);
        if (!ajustador.matches()) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                "BYTE espera X'0F1A' (hexadecimal em pares de dígitos) ou C'texto'; recebeu '" + operando + "'"));
            return null;
        }
        byte[] bytes;
        if (ajustador.group(1).equalsIgnoreCase("X")) {
            String hexa = ajustador.group(2);
            if (hexa.isEmpty() || hexa.length() % 2 != 0) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "BYTE X exige uma quantidade par de dígitos hexadecimais; recebeu '" + hexa + "'"));
                return null;
            }
            bytes = java.util.HexFormat.of().parseHex(hexa);
        } else {
            String texto = ajustador.group(2);
            if (texto.isEmpty()) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), "BYTE C não pode ser vazio"));
                return null;
            }
            bytes = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (bytes.length > 0xFFFF) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                "BYTE suporta no máximo 0xFFFF bytes; recebeu " + bytes.length));
            return null;
        }
        byte[] alinhados = new byte[((bytes.length + 2) / 3) * 3];
        System.arraycopy(bytes, 0, alinhados, 0, bytes.length);
        return alinhados;
    }

    // Operandos de formato 2: CLEAR e TIXR aceitam 1 registrador; SHIFTL/SHIFTR aceitam
    // registrador + deslocamento (0..7); os demais exigem 2 registradores. RSUB exige 0.
    private boolean validarOperandosInstrucao(Linha linha, TabelaInstrucoes instrucao) {
        if (instrucao.formato() != 2) {
            if (instrucao == TabelaInstrucoes.RSUB) {
                if (!linha.operandos().isEmpty())
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), "RSUB não aceita operandos"));
                return linha.operandos().isEmpty();
            }
            if (linha.operandos().size() != 1) {
                return erroOperandosInstrucao(linha, instrucao, linha.operandos().size());
            }
            return true;
        }
        int quantidade = linha.operandos().size();
        if (quantidade < 1 || quantidade > 2)
            return erroOperandosInstrucao(linha, instrucao, quantidade);
        if (quantidade == 1 && !aceitaUmOperandoFormato2(instrucao)) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                instrucao.name() + " (formato 2) exige 2 registradores como operandos"));
            return false;
        }
        return true;
    }

    private boolean erroOperandosInstrucao(Linha linha, TabelaInstrucoes instrucao, int quantidade) {
        erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
            (instrucao == TabelaInstrucoes.RSUB
                ? "RSUB não aceita operandos; recebeu " + quantidade
                : instrucao.name() + " exige 1 operando; recebeu " + quantidade)));
        return false;
    }

    private boolean aceitaUmOperandoFormato2(TabelaInstrucoes instrucao) {
        return instrucao == TabelaInstrucoes.CLEAR || instrucao == TabelaInstrucoes.TIXR
            || instrucao == TabelaInstrucoes.SHIFTL || instrucao == TabelaInstrucoes.SHIFTR;
    }

    // ------------------------------------------------------------------ passo 2

    /** Gera bytes/listagem para a linha processada no passo 1. */
    private void gerar(LinhaProcessada processada) {
        Linha linha = processada.linha();
        String codigo = "";
        byte[] dados = null;
        TabelaDiretivas diretiva = TabelaDiretivas.de(linha.operador());
        TabelaInstrucoes instrucao = TabelaInstrucoes.de(linha.operador());

        if (diretiva != null) {
            switch (diretiva) {
                case START, END -> { /* nunca chegam aqui: não entram na fila de linhas */ }
                case EQU, ORG -> { /* nada a gerar: EQU definiu símbolo, ORG só moveu a LC */ }
                case BASE -> {
                    Integer valor = resolverValor(linha, linha.operandos().get(0),
                        "Símbolo indefinido em BASE: '" + linha.operandos().get(0) + "'");
                    if (valor != null) baseAtual = valor;
                }
                case WORD -> {
                    Integer valor = lerNumero(linha, linha.operandos().get(0), 0, 0xFFFFFF, "valor de WORD");
                    if (valor != null) dados = new byte[] {
                        (byte) (valor >>> 16 & 0xFF), (byte) (valor >>> 8 & 0xFF), (byte) (valor & 0xFF) };
                }
                case BYTE -> dados = processada.dadosByte(); // bytes já parseados no passo 1
                // RESW e RESB apenas reservam: a memória da máquina já começa zerada.
                case RESW, RESB -> { }
            }
        } else if (instrucao == null) {
            return; // erro já acumulado no passo 1
        } else if (instrucao.formato() == 2) {
            dados = gerarFormato2(linha, instrucao);
        } else if (instrucao == TabelaInstrucoes.RSUB) {
            dados = new byte[] { 0x4C, 0, 0 }; // sem operando: ni = 00
        } else {
            dados = gerarFormato34(processada, instrucao);
        }
        if (dados != null) codigo = hexBytes(dados);
        // EQU/ORG/BASE entram na listagem com endereço -1 (passo 1 já definiu isso em LinhaProcessada).
        int enderecoListagem = processada.endereco();
        listagem.add(new ProgramaMontado.LinhaListagem(linha.numeroLinha(), enderecoListagem,
            codigo, processada.fonte()));
        if (dados != null) {
            // WORD é dado: não é instrução e não deve entrar na lista de endereços executáveis.
            boolean executavel = diretiva == null;
            entradas.add(new ProgramaMontado.Entrada(processada.endereco(), dados, executavel));
        }
    }

    /** Byte único de registradores: byte0 = código, byte1 = (reg1 &lt;&lt; 4) | reg2. */
    private byte[] gerarFormato2(Linha linha, TabelaInstrucoes instrucao) {
        List<String> operandos = linha.operandos();
        Integer reg1 = lerRegistrador(linha, operandos.get(0));
        if (reg1 == null) return null;
        int nibble2 = 0;
        if (operandos.size() == 2) {
            if (instrucao == TabelaInstrucoes.SHIFTL || instrucao == TabelaInstrucoes.SHIFTR) {
                Integer deslocamento = lerNumero(linha, operandos.get(1), 0, 7, "deslocamento de " + instrucao.name());
                if (deslocamento == null) return null;
                nibble2 = deslocamento; // o executor usa quantidade = n + 1
            } else {
                Integer reg2 = lerRegistrador(linha, operandos.get(1));
                if (reg2 == null) return null;
                nibble2 = reg2;
            }
        }
        return new byte[] { (byte) instrucao.codigo(), (byte) (reg1 << 4 | nibble2) };
    }

    private Integer lerRegistrador(Linha linha, String nome) {
        Integer numero = TabelaInstrucoes.codigoRegistrador(nome);
        if (numero == null)
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), "Registrador inválido: '" + nome + "'"));
        return numero;
    }

    private byte[] gerarFormato34(LinhaProcessada processada, TabelaInstrucoes instrucao) {
        Linha linha = processada.linha();
        boolean formato4 = linha.formato4();
        List<String> operandos = linha.operandos();
        String primeiro = operandos.get(0);
        boolean pRelativo = false; // 0x1000 = bit p; 0x2000 = bit b (codificado em campoDeFormato3)
        boolean bRelativo = false;
        boolean imediato = primeiro.startsWith("#");
        boolean indireto = primeiro.startsWith("@");
        String alvoTexto = imediato || indireto ? primeiro.substring(1) : primeiro;
        if (alvoTexto.isEmpty() || (imediato && indireto)) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(), "Operando de " + instrucao.name() + " vazio ou ambíguo"));
            return null;
        }
        // Segundo operando 'X': bit de indexação. Exige endereçamento simples (n=1, i=1).
        boolean x = operandos.size() == 2;
        if (x) {
            if (imediato || indireto) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "Indexação (,X) não é admitida com operandos imediato (#) ou indireto (@)"));
                return null;
            }
            if (!"X".equalsIgnoreCase(operandos.get(1))) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "Segundo operando de " + instrucao.name() + " deve ser 'X', recebeu '" + operandos.get(1) + "'"));
                return null;
            }
        }
        // Bits n/i do primeiro byte.
        int bitsNi;
        int campo;
        if (imediato) {
            bitsNi = 1; // n=0, i=1
            // Imediato negativo não é codificável: o executor lê o campo sem sinal.
            if (alvoTexto.startsWith("-")) {
                erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                    "Imediato negativo não é suportado (o campo da instrução não tem sinal); " +
                    "guarde o valor negativo em um WORD/SOMA e use o endereço"));
                return null;
            }
            int maximoCampo = formato4 ? 0xFFFFF : 0xFFF;
            Integer valor;
            if (alvoTexto.matches("(?i)[0-9A-F]{1,6}")) {
                valor = lerNumero(linha, alvoTexto, 0, maximoCampo,
                    "valor imediato de " + linha.operador());
                if (valor == null) return null;
            } else {
                valor = simbolos.consultar(alvoTexto); // imediato com símbolo: valor simbólico
                if (valor == null) {
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "Imediato '" + alvoTexto + "' não é número hexadecimal nem símbolo definido"));
                    return null;
                }
                if (valor > maximoCampo) {
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "Símbolo '" + alvoTexto + "' = " + hex(valor) + " excede o campo imediato de " +
                        (formato4 ? 20 : 12) + " bits; use formato 4 (prefixo '+')"));
                    return null;
                }
            }
            campo = valor; // imediato: o valor entra no campo, não um endereço
        } else {
            bitsNi = indireto ? 2 : 3; // n=1,i=0 → 2; n=1,i=1 → 3
            Integer alvo = resolverAlvo(linha, alvoTexto);
            if (alvo == null) return null;
            if (formato4) {
                if (alvo > 0xFFFFF) {
                    erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                        "Endereço " + hex(alvo) + " excede os 20 bits do campo de formato 4"));
                    return null;
                }
                campo = alvo; // formato 4: endereço absoluto direto (20 bits), sem geração de erro de faixa
            } else {
                int codificado = campoDeFormato3(linha, processada.endereco(), alvo);
                if (codificado < 0) return null; // erro acumulado
                pRelativo = (codificado & 0x1000) != 0;
                bRelativo = (codificado & 0x2000) != 0;
                campo = codificado & 0xFFF;
            }
        }
        int campoUmaPalavra = campo & 0xFFF;
        int campoDuasPalavras = campo & 0xFFFFF;
        byte byte0 = (byte) (instrucao.codigo() | bitsNi);
        if (formato4) {
            byte byte1 = (byte) ((x ? 0x80 : 0) | 0x10 | campoDuasPalavras >>> 16 & 0xF); // bit e (16)
            byte byte2 = (byte) (campoDuasPalavras >>> 8 & 0xFF);
            byte byte3 = (byte) (campoDuasPalavras & 0xFF);
            return new byte[] { byte0, byte1, byte2, byte3 };
        }
        byte byte1 = (byte) ((x ? 0x80 : 0) | (pRelativo ? 0x20 : 0) | (bRelativo ? 0x40 : 0) | campoUmaPalavra >>> 8 & 0xF);
        byte byte2 = (byte) (campoUmaPalavra & 0xFF);
        return new byte[] { byte0, byte1, byte2 };
    }

    /**
     * Campo de formato 3: direto se alvo ≤ 0xFFF; senão PC-relativo (± uma janela de palavras);
     * senão base-relativo (se BASE ativo e deslocamento 0..0xFFF); senão erro.
     */
    private int campoDeFormato3(Linha linha, int enderecoDaLinha, Integer alvo) {
        if (alvo <= 0xFFF) return alvo; // b=p=e=0
        int proximoEndereco = enderecoDaLinha + 1; // PC-relativo é medido em palavras (LC + 1)
        int deslocamento = alvo - proximoEndereco;
        if (deslocamento >= -0x800 && deslocamento <= 0x7FF)
            return 0x1000 | ((deslocamento + 0x1000) & 0xFFF); // bit p = 1; deslocamento em complemento de dois
        if (baseAtual != null) {
            int deslocamentoDaBase = alvo - baseAtual;
            if (deslocamentoDaBase >= 0 && deslocamentoDaBase <= 0xFFF)
                return 0x2000 | deslocamentoDaBase; // bit b = 1
        }
        erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
            "Endereço " + hex(alvo) + " não alcançável no formato 3; use formato 4 (prefixo '+') " +
            "ou defina BASE com o mesmo valor que B terá na execução"));
        return -1;
    }

    /** Alvo (não-imediato): número hexadecimal direto ou símbolo da tabela (erro se indefinido). */
    private Integer resolverAlvo(Linha linha, String alvoTexto) {
        Integer numero = tentarNumero(alvoTexto, 0, 0xFFFFFF);
        if (numero != null) return numero;
        Integer endereco = simbolos.consultar(alvoTexto);
        if (endereco == null) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                "Símbolo indefinido em " + linha.operador() + ": '" + alvoTexto + "'"));
            return null;
        }
        return endereco;
    }

    // ------------------------------------------------------------------ utilidades

    /** Número hexadecimal de 1 a 6 dígitos, na faixa [minimo, maximo]; null com erro acumulado. */
    private Integer lerNumero(Linha linha, String texto, int minimo, int maximo, String qual) {
        if (texto == null || !texto.matches("(?i)[0-9A-F]{1,6}")) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                qual + " inválido: '" + texto + "' (esperado número hexadecimal de 1 a 6 dígitos)"));
            return null;
        }
        long valor = Long.parseLong(texto, 16);
        if (valor < minimo || valor > maximo) {
            erros.add(new ProgramaMontado.ErroMontagem(linha.numeroLinha(),
                qual + " = " + hex((int) valor) + " está fora da faixa permitida [" + hex(minimo) + ", " + hex(maximo) + "]"));
            return null;
        }
        return (int) valor;
    }

    /** Tenta ler número hexadecimal de 1 a 6 dígitos, na faixa; null sem efeito colateral. */
    private static Integer tentarNumero(String texto, int minimo, int maximo) {
        if (texto == null || !texto.matches("(?i)[0-9A-F]{1,6}")) return null;
        long valor = Long.parseLong(texto, 16);
        if (valor < minimo || valor > maximo) return null;
        return (int) valor;
    }

    private static String hex(int valor) {
        return "%06X".formatted(valor);
    }

    private static String hexBytes(byte[] dados) {
        StringBuilder texto = new StringBuilder();
        for (byte dado : dados) texto.append("%02X".formatted(dado));
        return texto.toString();
    }
}
