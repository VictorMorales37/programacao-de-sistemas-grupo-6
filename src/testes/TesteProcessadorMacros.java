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

    void testeDefinicaoInternaSoExisteDepoisDaExpansao() {
        igual("INTERNA 1\nLDA 2", normalizar(ProcessadorMacros.processar("""
                EXTERNA MACRO
                INTERNA MACRO &X
                LDA &X
                MEND
                MEND
                INTERNA 1
                EXTERNA
                INTERNA 2
                """)), "a definição interna não pode antecipar sua ativação");
    }

    void testeCapturaRedefinicaoESombreamento() {
        igual("LDA 3\nSTA 8\nLDA 4\nSTA 9", normalizar(ProcessadorMacros.processar("""
                EXTERNA MACRO &X,&Y
                INTERNA MACRO &X
                LDA &X
                STA &Y
                MEND
                MEND
                EXTERNA 1,8
                INTERNA 3
                EXTERNA 2,9
                INTERNA 4
                """)), "formal interno sombreia o externo e o parâmetro livre captura cada redefinição");
    }

    void testeTresNiveisDeDefinicao() {
        igual("LDA 1\nADD 2\nSTA 3", normalizar(ProcessadorMacros.processar("""
                FORA MACRO &A
                MEIO MACRO &B
                DENTRO MACRO &C
                LDA &A
                ADD &B
                STA &C
                MEND
                MEND
                MEND
                FORA 1
                MEIO 2
                DENTRO 3
                """)), "captura parâmetros de dois níveis externos");
    }

    void testeChamadasRestauramParametros() {
        igual("LDA 1\nLDA 2\nSTA 1\nLDA 3\nLDA 2\nSTA 3", normalizar(ProcessadorMacros.processar("""
                INTERNA MACRO &X
                LDA &X
                MEND
                EXTERNA MACRO &X
                LDA &X
                INTERNA 2
                STA &X
                MEND
                EXTERNA 1
                EXTERNA 3
                """)), "cada chamada possui seu próprio ambiente");
    }

    void testeRedefinicaoMaiorEChamadaEmAndamento() {
        igual("LDA 1\nSTA 1\nLDA 2\nADD 3\nSTA 4", normalizar(ProcessadorMacros.processar("""
                M MACRO
                LDA 1
                M MACRO
                LDA 2
                ADD 3
                STA 4
                MEND
                STA 1
                MEND
                M
                M
                """)), "redefinir não altera o corpo da chamada já em andamento");
    }

    void testeArgumentosComEspacosAspasEOmissao() {
        igual("LDA VALOR\nSTA RESULTADO\nBYTE C'a, b;c'\nLDA DADO", normalizar(ProcessadorMacros.processar("""
                COPIA MACRO &A, &B
                LDA &A
                STA &B
                MEND
                DADOS MACRO &T
                BYTE &T
                MEND
                ROT MACRO &L,&A
                &L LDA &A
                MEND
                COPIA VALOR, RESULTADO
                DADOS C'a, b;c'
                ROT , DADO
                """)), "preserva espaços, vírgulas e ponto e vírgula entre aspas e a posição vazia");
    }

    void testeOmissaoNoMeioENoFim() {
        igual("BYTE C'1//3'\nBYTE C'1/2/'", normalizar(ProcessadorMacros.processar("""
                M MACRO &A,&B,&C
                BYTE C'&A/&B/&C'
                MEND
                M 1,,3
                M 1,2,
                """)), "omissões não deslocam os argumentos");
    }

    void testeSubstituicaoSimultaneaECaseInsensitive() {
        igual("LDA &B\nSTA 7\nLDA &AB", normalizar(ProcessadorMacros.processar("""
                M MACRO &a,&b
                LDA &A
                STA &B
                LDA &AB
                MEND
                m &B,7
                """)), "não substitui de novo o valor inserido nem prefixos de parâmetros");
    }

    void testeParametroNaoVazaParaMacroChamada() {
        igual("LDA &X", normalizar(ProcessadorMacros.processar("""
                INTERNA MACRO
                LDA &X
                MEND
                EXTERNA MACRO &X
                INTERNA
                MEND
                EXTERNA 3
                """)), "chamada não captura o ambiente dinâmico da chamadora");
    }

    void testeNomeDeMacroComoOperandoNaoExpande() {
        igual("LDA M\nJ M", normalizar(ProcessadorMacros.processar("M MACRO\n LDA 1\nMEND\nLDA M\nJ M")),
                "somente o campo de operação pode chamar macros");
    }

    void testeParametroNaoSubstituiParteDeConstante() {
        igual("LDA 1A\nSTA DADO", normalizar(ProcessadorMacros.processar(
                "M MACRO A\nLDA 1A\nSTA A\nMEND\nM DADO")), "preserva constantes hexadecimais");
    }

    void testeUmaPassagemSemReferenciaAvante() {
        igual("M 1\nLDA 2", normalizar(ProcessadorMacros.processar("M 1\nM MACRO &X\nLDA &X\nMEND\nM 2")),
                "não retorna ao fonte para expandir chamadas anteriores à definição");
    }

    void testeSintaxeDosSlidesEParametroDeRotulo() {
        igual("MARK LDA COUNT\nADD #1\nSTA COUNT", normalizar(ProcessadorMacros.processar("""
                MCDEFN
                L INCR A
                L LDA A
                ADD #1
                STA A
                MCEND
                MARK INCR COUNT
                """)), "aceita protótipo separado e parâmetro formal de rótulo");
        igual("MARK LDA COUNT", normalizar(ProcessadorMacros.processar(
                "INCR MCDEFN L, A\nL LDA A\nMCEND\nINCR MARK, COUNT")), "aceita protótipo na mesma linha");
    }

    void testeExemploScaleDosSlides() {
        igual("LDA DADO\nSHIFTR A,3\nSTA SAIDA", normalizar(ProcessadorMacros.processar("""
                MCDEFN
                SCALE RP
                MCDEFN
                MULTSC ORIGEM,DESTINO
                LDA ORIGEM
                SHIFTR A,RP
                STA DESTINO
                MCEND
                MCEND
                SCALE 3
                MULTSC DADO,SAIDA
                """)), "captura RP na definição produzida por SCALE");
    }

    void testeSerialDeRotulosPorExpansao() {
        String fonte = ProcessadorMacros.processar("""
                REP MACRO
                VOLTA.SER LDA #0
                J VOLTA.SER
                MEND
                REP
                REP
                END
                """);
        igual("VOLTA0001 LDA #0\nJ VOLTA0001\nVOLTA0002 LDA #0\nJ VOLTA0002\nEND", normalizar(fonte),
                "serial global mantém referências e distingue chamadas");
        verdadeiro(Montador.montar(fonte).validada(), "rótulos gerados devem montar sem duplicação");
    }

    void testeComentariosEBomPreservados() {
        igual("; MEND\nLDA 1 ; &X não altera comentário", normalizar(ProcessadorMacros.processar(
                "\uFEFFM MACRO &X\n; MEND\nLDA &X ; &X não altera comentário\nMEND\nM 1")),
                "comentários não participam da expansão");
    }

    void testeErrosAdicionais() {
        String[] invalidos = {
            "MEND", "MCDEFN", "M MACRO &X,&x\nMEND", "M MACRO &X,\nMEND",
            "M MACRO\nMEND lixo", "LDA MACRO\nMEND", "M MACRO &X\nMEND\nM C'ab",
            "M MACRO\nROT LDA 1\nMEND\nOUTRO M", "M MACRO\nMEND\nROT M",
            "M MACRO\nN MACRO\nMEND"
        };
        for (String fonte : invalidos)
            lanca(IllegalArgumentException.class, () -> ProcessadorMacros.processar(fonte), "deve rejeitar: " + fonte);
    }

    void testeRecursaoMutua() {
        lanca(IllegalArgumentException.class, () -> ProcessadorMacros.processar(
                "M MACRO\nN\nMEND\nN MACRO\nM\nMEND\nM"), "limita recursão indireta");
    }

    void testeErroNaoSobrescreveArquivoExistente() throws Exception {
        Path pasta = Files.createTempDirectory("macro-erro-");
        Path destino = pasta.resolve("MASMAPRG.ASM");
        try {
            Files.writeString(destino, "anterior");
            boolean falhou = false;
            try { ProcessadorMacros.processarEGravar("M MACRO", destino); }
            catch (IllegalArgumentException esperado) { falhou = true; }
            verdadeiro(falhou, "fonte inválido precisa falhar");
            igual("anterior", Files.readString(destino), "erro de expansão preserva saída existente");
        } finally {
            Files.deleteIfExists(destino);
            Files.delete(pasta);
        }
    }

    void testeMainGeraArquivoAntesDaMontagem() throws Exception {
        Path pasta = Files.createTempDirectory("macro-main-");
        Path fonte = pasta.resolve("entrada com espaços.asm");
        Path destino = pasta.resolve("MASMAPRG.ASM");
        Path log = pasta.resolve("execucao.log");
        String executavelJava = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of(TesteProcessadorMacros.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toString();
        try {
            Files.writeString(fonte, "M MACRO &X\nLDA #&X\nMEND\nM 2\nEND\n");
            Process processo = new ProcessBuilder(executavelJava, "-cp", classes, "Main", fonte.toString())
                    .directory(pasta.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!processo.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new AssertionError("Main não terminou no prazo");
            }
            igual(0, processo.exitValue(), "Main deve concluir: " + Files.readString(log));
            igual("LDA #2\nEND", normalizar(Files.readString(destino)), "nome e conteúdo da saída do integrador");
            verdadeiro(Montador.montar(Files.readString(destino)).validada(), "arquivo produzido pelo Main pode ser montado");
            Files.writeString(fonte, "M MACRO");
            processo = new ProcessBuilder(executavelJava, "-cp", classes, "Main", fonte.toString())
                    .directory(pasta.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!processo.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new AssertionError("Main não terminou no prazo");
            }
            igual(1, processo.exitValue(), "Main sinaliza erro de macro");
            igual("LDA #2\nEND", normalizar(Files.readString(destino)), "erro não sobrescreve a saída anterior");
        } finally {
            Files.deleteIfExists(log);
            Files.deleteIfExists(destino);
            Files.deleteIfExists(fonte);
            Files.delete(pasta);
        }
    }

    private static String normalizar(String texto) {
        return texto.lines().map(String::trim).filter(l -> !l.isEmpty()).collect(java.util.stream.Collectors.joining("\n"));
    }

}
