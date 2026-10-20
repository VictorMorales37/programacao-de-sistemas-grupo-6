package testes;

import cpu.CPU;
import entrada.ImagemPrograma;
import montador.Montador;
import montador.ProgramaMontado;

import static testes.Asserts.*;

import java.util.List;

public final class TesteMontador {

    /** Soma incrementos em X até 5, no estilo do example.sic (símbolo implícito STOP = 8). */
    private static final String EXEMPLO_SOMA = """
            SOMA    START 0
                    LDA #0
                    LDX #1
            CICLO   COMP #5
                    JGT FIM
                    ADDR X,A
                    TIX CICLO
                    J CICLO
            FIM     J STOP
                    END
            """;

    /** Byte do código objeto no endereço lógico + deslocamento em bytes (0..2 por palavra). */
    private static byte byteDe(ProgramaMontado pm, int endereco, int deslocamento) {
        int alvo = endereco * 3 + deslocamento;
        for (ProgramaMontado.Entrada entrada : pm.entradas()) {
            int inicio = entrada.endereco() * 3;
            int fim = inicio + entrada.dados().length;
            if (alvo >= inicio && alvo < fim) return entrada.dados()[alvo - inicio];
        }
        throw new AssertionError("não há objeto montado em " + endereco + "+" + deslocamento);
    }

    void testeMontaExemploSimples() {
        ProgramaMontado pm = Montador.montar(EXEMPLO_SOMA);
        verdadeiro(pm.validada(), "o exemplo simples deveria montar sem erros: " + pm.erros());
        igual(0, pm.enderecoEntrada(), "START 0 deveria definir entrada = 0");
        igual(8, pm.enderecoParada(), "STOP implícito = primeira palavra livre (LC final)");
        // LDA #0 → 01 00 00 ; LDX #1 → 05 00 01
        igual(0x01, byteDe(pm, 0, 0) & 0xFF, "LDA #0: bit i=1");
        igual(0x00, byteDe(pm, 0, 2) & 0xFF, "LDA #0: campo 0");
        igual(0x05, byteDe(pm, 1, 0) & 0xFF, "LDX #1: byte 0 = 04|3");
        igual(0x01, byteDe(pm, 1, 2) & 0xFF, "LDX #1: campo 1");
        // CICLO COMP #5 em LC 2 → 29 00 05
        igual(0x29, byteDe(pm, 2, 0) & 0xFF, "COMP #5: 28|3 = 0x29");
        igual(0x05, byteDe(pm, 2, 2) & 0xFF, "COMP #5: campo 5");
        // JGT FIM em LC 3 (FIM=7): n=i=1 e, por alvo ≤ 0xFFF, e=p=b=0 → 37 00 07
        igual(0x37, byteDe(pm, 3, 0) & 0xFF, "JGT FIM: 34|3 = 0x37");
        igual(0x00, byteDe(pm, 3, 1) & 0xFF, "JGT FIM: byte de controle");
        igual(0x07, byteDe(pm, 3, 2) & 0xFF, "JGT FIM: campo = endereço de FIM");
        // ADDR X,A → 90 10 (X=1 << 4 | A=0)
        igual(0x90, byteDe(pm, 4, 0) & 0xFF, "ADDR código");
        igual(0x10, byteDe(pm, 4, 1) & 0xFF, "ADDR X,A: byte de registradores 0x10");
        // TIX CICLO em LC 5 (CICLO=2) → 2F 00 02
        igual(0x2F, byteDe(pm, 5, 0) & 0xFF, "TIX CICLO: 2C|3");
        igual(0x02, byteDe(pm, 5, 2) & 0xFF, "TIX CICLO: campo 2");
        // J CICLO = J STOP: J em LC 6 e LC 7
        igual(0x3F, byteDe(pm, 6, 0) & 0xFF, "J CICLO: 3C|3");
        igual(0x02, byteDe(pm, 6, 2) & 0xFF, "J CICLO: campo 2");
        igual(0x3F, byteDe(pm, 7, 0) & 0xFF, "J STOP: byte 0");
        igual(0x08, byteDe(pm, 7, 2) & 0xFF, "J STOP: campo = STOP");
    }

    void testeReferenciasAdiante() {
        ProgramaMontado pm = Montador.montar("""
                JEQ FIM
                J STOP
                FIM RESW 1
                END
                """);
        verdadeiro(pm.validada(), "referência adiante deveria montar: " + pm.erros());
        List<ProgramaMontado.Simbolo> tabela = pm.tabelaSimbolos();
        boolean fim = false, stop = false;
        for (ProgramaMontado.Simbolo simbolo : tabela) {
            if (simbolo.nome().equals("FIM")) { fim = true; igual(2, simbolo.endereco(), "FIM deveria valer 2"); }
            if (simbolo.nome().equals("STOP")) { stop = true; igual(3, simbolo.endereco(), "STOP implícito = LC final"); }
        }
        verdadeiro(fim && stop, "a tabela deveria conter FIM e STOP");
        igual(0x33, byteDe(pm, 0, 0) & 0xFF, "JEQ FIM: 30|3");
        igual(0x02, byteDe(pm, 0, 2) & 0xFF, "JEQ FIM: campo = endereço de FIM (referência adiante)");
    }

    void testeFormaTresEDois() {
        ProgramaMontado pm = Montador.montar("""
                LACOMP COMP #5          ; 4 campos: rótulo + operador + operando + comentário
                TESTE  CLEAR A          ; rótulo + operador + operando, formato 2
                       ADDR X, A,       ; sem rótulo, 3 campos (vírgula/espaço separam operandos)
                COMPA  COMP #7          ; rótulo parecido com o mnemônico
                        RSUB           ; sem rótulo, 2 campos
                 END
                """);
        verdadeiro(pm.validada(), "linhas com e sem rótulo deveriam montar: " + pm.erros());
        igual(0x29, byteDe(pm, 0, 0) & 0xFF, "linha de 4 campos: rótulo reconhecido");
        igual(0x05, byteDe(pm, 0, 2) & 0xFF, "COMP #5: campo 5");
        igual(0xB4, byteDe(pm, 1, 0) & 0xFF, "CLEAR código");
        igual(0x00, byteDe(pm, 1, 1) & 0xFF, "CLEAR A: (A=0 << 4) | nibble2 = 0");
        igual(0x90, byteDe(pm, 2, 0) & 0xFF, "ADDR código em linha de 3 campos");
        igual(0x10, byteDe(pm, 2, 1) & 0xFF, "ADDR X,A = 90 10 como no example.sic");
        igual(0x29, byteDe(pm, 3, 0) & 0xFF, "rótulo parecido com mnemônico não confunde o desambiguador");
        igual(0x07, byteDe(pm, 3, 2) & 0xFF, "COMP #7: campo 7");
        igual(0x4C, byteDe(pm, 4, 0) & 0xFF, "RSUB: 4C");
        igual(0x00, byteDe(pm, 4, 1) & 0xFF, "RSUB: 00 00");
        igual(0x00, byteDe(pm, 4, 2) & 0xFF, "RSUB: 4C 00 00");
    }

    void testeErros() {
        ProgramaMontado pm = Montador.montar("""
                A LDA #0
                A LDA #0
                XXX #0
                LDA #1FFF
                RSUB ERRADO
                J VAZIA
                END
                """);
        falso(pm.validada(), "programa com erros deveria relatar erros");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("Símbolo duplicado")), "erro de símbolo duplicado");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("Operador inválido")), "erro de opcode inválido");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("fora da faixa")), "imediato > 0xFFF deveria dar erro de faixa");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("RSUB")), "RSUB com operando deveria dar erro");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("Símbolo indefinido")), "símbolo indefinido é erro no passo 2");
        falso(pm.entradas().isEmpty(), "mesmo com erros, deveriam existir entradas parciais");
    }

    void testeListagemETabelaDeSimbolos() {
        ProgramaMontado pm = Montador.montar("""
                LDA #1
                X RESW 1
                END
                """);
        verdadeiro(pm.validada(), "o programa pequeno deveria montar sem erros");
        igual(2, pm.listagem().size(), "listagem deveria ter 2 linhas (sem END)");
        ProgramaMontado.LinhaListagem primeira = pm.listagem().get(0);
        igual(1, primeira.numero(), "número da linha fonte");
        igual(0, primeira.endereco(), "endereço da primeira instrução");
        igual("010001", primeira.codigo(), "código hex da listagem");
        ProgramaMontado.LinhaListagem segunda = pm.listagem().get(1);
        igual(1, segunda.endereco(), "RESW segue no endereço 1");
        igual("", segunda.codigo(), "RESW não gera código");
        igual(2, pm.tabelaSimbolos().size(), "tabela com X e STOP");
        igual("STOP", pm.tabelaSimbolos().get(0).nome(), "nome em maiúsculas");
        igual(2, pm.tabelaSimbolos().get(0).endereco(), "STOP = 2 palavras");
        igual("X", pm.tabelaSimbolos().get(1).nome(), "X ordenado depois de STOP");
        igual(1, pm.tabelaSimbolos().get(1).endereco(), "X = 1");
        verdadeiro(pm.listagem().stream().noneMatch(l -> l.numero() == 3), "END não entra na listagem");
        verdadeiro(pm.entradas().stream().allMatch(ProgramaMontado.Entrada::executavel),
                "instruções e WORD devem gerar entrada executável");
    }

    void testeEscalaFormato4() {
        ProgramaMontado pm = Montador.montar("""
                START 1000
                +J FIM
                RESW 1000
                FIM J STOP
                END
                """);
        verdadeiro(pm.validada(), "formato 4 para símbolo de endereço > 0xFFF: " + pm.erros());
        igual(0x1000, pm.enderecoEntrada(), "START 1000 (hex) → entrada 0x1000");
        // +J FIM: FIM = 0x1002 + 0x1000 (RESW 1000 reserva 4096 palavras) = 0x2002 →
        // byte0 = 3F, byte1 = (x?128)|16|((FIM>>16)&0xF) = 0x10, byte2 = 0x20, byte3 = 0x02
        igual(0x3F, byteDe(pm, 0x1000, 0) & 0xFF, "+J: 3C|3");
        igual(0x10, byteDe(pm, 0x1000, 1) & 0xFF, "+J: bit e = 1 (0x10), sem nibble alto");
        igual(0x20, byteDe(pm, 0x1000, 2) & 0xFF, "+J: byte 2 do endereço 0x2002");
        igual(0x02, byteDe(pm, 0x1001, 0) & 0xFF, "+J: byte 3 do endereço 0x2002");
        byte[] salto = pm.entradas().get(1).dados();
        igual(0x3F, salto[0] & 0xFF, "J STOP no formato 3");
        igual(0x20, salto[1] & 0xFF, "J STOP: PC-relativo (p=1), STOP = instrução sobre si mesma");
        igual(0x00, salto[2] & 0xFF, "J STOP: campo do deslocamento 0");
        // Sem o '+', o mesmo salto é erro de faixa no formato 3
        ProgramaMontado semQuatro = Montador.montar("""
                START 1000
                J FIM
                RESW 1000
                FIM J STOP
                END
                """);
        verdadeiro(semQuatro.erros().stream().anyMatch(e -> e.mensagem().contains("não alcançável")),
                "sem '+' deveria dar erro 'não alcançável no formato 3'");
    }

    void testeDiretivaEqu() {
        ProgramaMontado pm = Montador.montar("""
                TAM    EQU 10
                VETOR  EQU TAM
                       LDA #TAM
                       STA VETOR
                       J STOP
                END
                """);
        verdadeiro(pm.validada(), "EQU deveria montar: " + pm.erros());
        igual(0x01, byteDe(pm, 0, 0) & 0xFF, "LDA #TAM: LDA imediato");
        igual(0x10, byteDe(pm, 0, 2) & 0xFF, "imediato com símbolo: campo = valor de TAM (10 hex = 0x10)");
        igual(0x0F, byteDe(pm, 1, 0) & 0xFF, "STA VETOR: 0C|3");
        igual(0x10, byteDe(pm, 1, 2) & 0xFF, "VETOR EQU TAM → campo = 0x10 (EQU não ocupa memória)");
        boolean vetor = false;
        for (ProgramaMontado.Simbolo s : pm.tabelaSimbolos())
            if (s.nome().equals("VETOR")) { vetor = true; igual(0x10, s.endereco(), "VETOR = valor de TAM"); }
        verdadeiro(vetor, "VETOR deve constar na tabela");
        // EQU com símbolo a frente não resolvível (passo 1), EQU sem rótulo e duplicado:
        ProgramaMontado ruim = Montador.montar("""
                LDA #X
                X EQU 1
                Y EQU Z
                EQU 5
                W EQU 1
                W EQU 2
                END
                """);
        verdadeiro(ruim.erros().stream().anyMatch(e -> e.mensagem().contains("Símbolo indefinido em EQU")),
            "EQU com símbolo a frente deveria dar erro no passo 1");
        verdadeiro(ruim.erros().stream().anyMatch(e -> e.mensagem().contains("EQU exige um rótulo")),
            "EQU sem rótulo deveria dar erro");
        verdadeiro(ruim.erros().stream().anyMatch(e -> e.mensagem().contains("Símbolo duplicado")),
            "EQU duplicando rótulo deveria dar erro");
    }

    void testeDiretivaOrg() {
        ProgramaMontado pm = Montador.montar("""
                ORG 100
                       LDA #1
                ATRAS  WORD 5
                ORG 0
                INICIO LDA ATRAS
                       J STOP
                END
                """);
        verdadeiro(pm.validada(), "ORG deveria montar: " + pm.erros());
        igual(0x01, byteDe(pm, 0x100, 0) & 0xFF, "instrução após ORG 100 (hex) fica em 0x100");
        igual(0x05, byteDe(pm, 0x101, 2) & 0xFF, "WORD 5 em 0x101");
        igual(0x03, byteDe(pm, 0, 0) & 0xFF, "segundo bloco volta a LC 0");
        igual(0x01, byteDe(pm, 0, 2) & 0xFF, "INICIO LDA ATRAS: campo = 0x101");
        igual(0x3F, byteDe(pm, 1, 0) & 0xFF, "J STOP em LC 1: 3C|3");
        igual(0x02, byteDe(pm, 1, 2) & 0xFF, "campo = STOP implícito (LC final = 2)");
    }

    void testeDiretivaByte() {
        ProgramaMontado pm = Montador.montar("""
                MENS   BYTE C'AB'
                DADOS  BYTE X'0F01'
                LONGA  BYTE C'ABCDEF'
                ERRADA BYTE X'0F0'
                VAZIA  BYTE C''
                       J STOP
                END
                """);
        falso(pm.validada(), "X ímpar e C vazio deveriam dar erro");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("quantidade par")), "X'0F0' ímpar");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("não pode ser vazio")), "C'' vazio");
        igual(0x41, byteDe(pm, 0, 0) & 0xFF, "C'AB' byte 0 = 'A'");
        igual(0x42, byteDe(pm, 0, 1) & 0xFF, "C'AB' byte 1 = 'B'");
        igual(0x00, byteDe(pm, 0, 2) & 0xFF, "C'AB' preenchimento zero");
        igual(0x0F, byteDe(pm, 1, 0) & 0xFF, "X'0F01' byte 0");
        igual(0x01, byteDe(pm, 1, 1) & 0xFF, "X'0F01' byte 1");
        igual(0x44, byteDe(pm, 3, 0) & 0xFF, "C'ABCDEF': 6 bytes ocupam 2 palavras (byte 0 da 2ª = 'D')");
        igual(0x46, byteDe(pm, 3, 2) & 0xFF, "C'ABCDEF' último byte = 'F' sem preenchimento");
        falso(pm.entradas().stream().filter(e -> e.endereco() <= 3).anyMatch(ProgramaMontado.Entrada::executavel),
            "BYTE gera dados, não instruções executáveis (linhas de BYTE)");
    }

    void testeEnderecamentoBaseRelativo() {
        ProgramaMontado pm = Montador.montar("""
                START 1000
                       +LDB #1000
                BASE   1000
                       LDA  ATRASADO
                       J    STOP
                VETOR  RESW 800
                ATRASADO WORD B
                END
                """);
        verdadeiro(pm.validada(), "BASE + PC-relativo estourando deveria montar: " + pm.erros());
        // RESW 800 reserva 0x800 palavras; VETOR = 0x1004, ATRASADO = 0x1804;
        // PC-rel de 0x1003 estoura; base-relativo: deslocamento = 0x1804 - 0x1000 = 0x804 → byte1 = b=0x40 | 8
        igual(0x03, byteDe(pm, 0x1002, 0) & 0xFF, "LDA ATRASADO: 00|3");
        igual(0x48, byteDe(pm, 0x1002, 1) & 0xFF, "LDA ATRASADO: bit b (0x40) e nibble 8");
        igual(0x04, byteDe(pm, 0x1002, 2) & 0xFF, "LDA ATRASADO: byte baixo do deslocamento");
        List<ImagemPrograma.Entrada> entradas = pm.entradas().stream()
            .map(e -> new ImagemPrograma.Entrada(e.endereco(), e.dados(), e.executavel()))
            .toList();
        ImagemPrograma imagem = new ImagemPrograma(pm.enderecoEntrada(), pm.enderecoParada(), entradas);
        CPU cpu = imagem.criarCPU();
        int limite = 50;
        while (!imagem.estaConcluido(cpu) && limite-- > 0) cpu.executarPasso();
        verdadeiro(limite > 0, "o programa com BASE deveria alcançar STOP");
        igual(0xB, cpu.obterRegistradores().obterA().obterValor(), "A = ATRASADO (base-relativo executou)");
        igual(0x1000, cpu.obterRegistradores().obterB().obterValor(), "B = 1000 carregado por formato 4 imediato");
    }

    void testeImediatoNegativoEAvisos() {
        ProgramaMontado pm = Montador.montar("""
                LDA #-5
                RESB 4
                END
                """);
        falso(pm.validada(), "imediato negativo deveria dar erro");
        verdadeiro(pm.erros().stream().anyMatch(e -> e.mensagem().contains("negativo")),
            "mensagem clara sobre imediato negativo");
        verdadeiro(pm.avisos().stream().anyMatch(a -> a.contains("RESB 4")),
            "RESB 4 deveria emitir aviso de alinhamento (2 palavras)");
    }

    void testeIntegracaoExecutorLOADER() {
        ProgramaMontado pm = Montador.montar("""
                LDA #10
                SUB #3
                CLEAR B
                ADDR X,A
                RMO A,S
                SHIFTL A,1
                STA VALOR
                J STOP
                VALOR RESB 1
                END
                """);
        verdadeiro(pm.validada(), "o programa determinístico deveria montar: " + pm.erros());
        List<ImagemPrograma.Entrada> entradas = pm.entradas().stream()
            .map(e -> new ImagemPrograma.Entrada(e.endereco(), e.dados(), e.executavel()))
            .toList();
        ImagemPrograma imagem = new ImagemPrograma(pm.enderecoEntrada(), pm.enderecoParada(), entradas);
        CPU cpu = imagem.criarCPU();
        int limite = 20;
        while (!imagem.estaConcluido(cpu) && limite-- > 0) cpu.executarPasso();
        verdadeiro(limite > 0, "o programa deveria alcançar STOP sem travar");
        igual(52L, cpu.obterRegistradores().obterA().obterValor(), "A = (0xA-3+X=0) deslocado 2 = 52");
        igual(13L, cpu.obterRegistradores().obterS().obterValor(), "S = valor de A antes do deslocamento (= 13)");
        igual(52, cpu.lerPalavra(8), "STA VALOR grava 52 no rótulo VALOR");
    }
}
