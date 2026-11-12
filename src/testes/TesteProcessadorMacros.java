package testes;

import montador.Montador;
import montador.ProcessadorMacros;
import montador.ProgramaMontado;

import static testes.Asserts.*;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TesteProcessadorMacros {

    void testeExpansaoDeMacroComArgumentos() {
        String expandido = ProcessadorMacros.processar("""
                COPIA MACRO &ORIGEM,&DESTINO
                    LDA &ORIGEM
                    STA &DESTINO
                MEND
                    COPIA VALOR,RESULTADO
                VALOR   WORD 1
                RESULTADO RESW 1
                END
                """);
        verdadeiro(expandido.contains("LDA VALOR"), "substitui o primeiro parâmetro");
        verdadeiro(expandido.contains("STA RESULTADO"), "substitui o segundo parâmetro");
        falso(expandido.contains("COPIA MACRO"), "remove a definição da saída");
        ProgramaMontado montado = Montador.montar(expandido);
        verdadeiro(montado.validada(), "o resultado expandido deve ser montável: " + montado.erros());
    }

    void testeChamadaAninhada() {
        String expandido = ProcessadorMacros.processar("""
                CARREGA MACRO &FONTE
                    LDA &FONTE
                MEND
                GUARDA MACRO &FONTE,&ALVO
                    CARREGA &FONTE
                    STA &ALVO
                MEND
                    GUARDA VALOR,RESULTADO
                VALOR WORD 1
                RESULTADO RESW 1
                END
                """);
        verdadeiro(expandido.contains("LDA VALOR"), "expande a macro chamada pelo corpo");
        verdadeiro(expandido.contains("STA RESULTADO"), "expande as demais linhas do corpo");
        verdadeiro(Montador.montar(expandido).validada(), "chamada aninhada deve gerar assembly válido");
    }

    void testeDefinicaoAninhadaEChamadaAninhada() {
        String expandido = ProcessadorMacros.processar("""
                EXTERNA MACRO &VALOR
                INTERNA MACRO &OPERANDO
                    LDA &OPERANDO
                MEND
                    INTERNA &VALOR
                MEND
                    EXTERNA DADO
                DADO WORD 1
                END
                """);
        verdadeiro(expandido.contains("LDA DADO"), "macro interna definida no corpo é expandida");
        falso(expandido.contains("INTERNA MACRO"), "definição interna não aparece na saída");
        verdadeiro(Montador.montar(expandido).validada(), "expansão de definição aninhada deve montar");
    }

    void testeRotuloNaChamadaEAusenciaDeRecursaoInfinita() {
        String expandido = ProcessadorMacros.processar("""
                NOP MACRO
                    LDA #0
                MEND
                INICIO NOP
                END
                """);
        verdadeiro(expandido.lines().anyMatch(linha ->
                linha.stripLeading().startsWith("INICIO ") && linha.contains("LDA #0")),
                "o rótulo da chamada deve ser transferido para a primeira instrução expandida");

        boolean recursaoLimitada = false;
        try {
            ProcessadorMacros.processar("REPETE MACRO\n REPETE\nMEND\n REPETE\n");
        } catch (IllegalArgumentException erro) {
            recursaoLimitada = erro.getMessage().contains("Limite de expansão");
        }
        verdadeiro(recursaoLimitada, "recursão de macros deve falhar com limite explícito");
    }

    void testeProcessamentoGravaFonteExpandidoNoArquivoIndicado() throws Exception {
        Path diretorio = Files.createTempDirectory("teste-macros-");
        Path destino = diretorio.resolve("MASMAPRG.ASM");
        String fonte = """
                UMA MACRO &N
                    LDA #&N
                MEND
                    UMA 2
                """;
        Path gravado = ProcessadorMacros.processarEGravar(fonte, destino);
        igual(destino.toAbsolutePath(), gravado, "retorna o caminho absoluto da saída");
        String resultado = Files.readString(destino);
        verdadeiro(resultado.contains("LDA #2"), "arquivo contém a macro expandida");
        falso(resultado.contains("UMA MACRO"), "arquivo não contém definição");
        Files.delete(destino);
        Files.delete(diretorio);
    }

    void testeErrosDeDefinicaoEArgumentos() {
        boolean semMend = false;
        try {
            ProcessadorMacros.processar("INCOMPLETA MACRO &A\n LDA &A\n");
        } catch (IllegalArgumentException erro) {
            semMend = erro.getMessage().contains("sem MEND");
        }
        verdadeiro(semMend, "definição sem MEND deve ser informada");

        boolean argumentosIncorretos = false;
        try {
            ProcessadorMacros.processar("M MACRO &A,&B\n LDA &A\nMEND\n M 1\n");
        } catch (IllegalArgumentException erro) {
            argumentosIncorretos = erro.getMessage().contains("espera 2 argumento(s)");
        }
        verdadeiro(argumentosIncorretos, "quantidade de argumentos deve ser validada");
    }
}
