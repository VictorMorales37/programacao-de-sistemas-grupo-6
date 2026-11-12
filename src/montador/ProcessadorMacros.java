package montador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessadorMacros {

    private static final String MACRO = "MACRO";
    private static final String MEND = "MEND";
    private static final int LIMITE_EXPANSAO = 100;
    private static final Pattern PARAMETRO = Pattern.compile("&[A-Za-z_][A-Za-z0-9_]*");

    private record Macro(String nome, List<String> parametros, List<String> corpo, int linhaDefinicao) {}
    private record Definicao(String nome, List<String> parametros) {}
    private record LinhaExpansivel(String rotulo, String operador, String operandos) {}

    private final Map<String, Macro> macros = new LinkedHashMap<>();
    private final List<String> saida = new ArrayList<>();
    private List<String> linhas;
    private int indice;

    private ProcessadorMacros() {}

    public static String processar(String fonte) {
        ProcessadorMacros processador = new ProcessadorMacros();
        processador.linhas = List.of(fonte.replace("\uFEFF", "").split("\\R", -1));
        processador.processarLinhas(0);
        return String.join(System.lineSeparator(), processador.saida);
    }

    public static Path processarArquivo(Path arquivoFonte) throws IOException {
        String fonte = Files.readString(arquivoFonte, StandardCharsets.UTF_8);
        return processarEGravar(fonte, Path.of("MASMAPRG.ASM"));
    }

    public static Path processarEGravar(String fonte, Path arquivoSaida) throws IOException {
        Path saida = arquivoSaida.toAbsolutePath();
        Files.writeString(saida, processar(fonte), StandardCharsets.UTF_8);
        return saida;
    }

    private void processarLinhas(int profundidade) {
        while (indice < linhas.size()) {
            int numeroLinha = indice + 1;
            String texto = linhas.get(indice++);
            String codigo = semComentario(texto).trim();
            if (codigo.isEmpty()) {
                saida.add(texto);
                continue;
            }
            if (ehMend(codigo))
                throw erro(numeroLinha, "MEND sem uma definição MACRO correspondente");

            Definicao definicao = analisarDefinicao(codigo);
            if (definicao != null) {
                lerDefinicao(definicao, numeroLinha, profundidade);
                continue;
            }

            LinhaExpansivel chamada = analisarChamada(codigo);
            Macro macro = chamada == null ? null : macros.get(normalizar(chamada.operador()));
            if (macro == null) {
                saida.add(texto);
                continue;
            }

            if (profundidade >= LIMITE_EXPANSAO)
                throw erro(numeroLinha, "Limite de expansão de macros excedido (possível recursão)");
            List<String> argumentos = analisarArgumentos(chamada.operandos());
            if (argumentos.size() != macro.parametros().size())
                throw erro(numeroLinha, "Macro " + macro.nome() + " espera "
                    + macro.parametros().size() + " argumento(s), recebeu " + argumentos.size());

            Map<String, String> substituicoes = new LinkedHashMap<>();
            for (int i = 0; i < argumentos.size(); i++)
                substituicoes.put(macro.parametros().get(i), substituir(argumentos.get(i), Map.of()));

            int inicioExpansao = saida.size();
            for (String linhaCorpo : macro.corpo()) {
                String expandida = substituir(linhaCorpo, substituicoes);
                processarLinhaExpandida(expandida, substituicoes, profundidade + 1, numeroLinha);
            }
            aplicarRotulo(chamada.rotulo(), inicioExpansao);
        }
    }

    private void lerDefinicao(Definicao definicao, int numeroLinha, int profundidade) {
        String nome = normalizar(definicao.nome());
        if (nome.equals(MACRO) || nome.equals(MEND))
            throw erro(numeroLinha, "Nome de macro reservado: " + nome);
        if (macros.containsKey(nome))
            throw erro(numeroLinha, "Macro já definida: " + nome);
        for (String parametro : definicao.parametros()) {
            if (!PARAMETRO.matcher(parametro).matches())
                throw erro(numeroLinha, "Parâmetro inválido na definição de " + nome + ": " + parametro);
        }
        if (definicao.parametros().stream().map(ProcessadorMacros::normalizar).distinct().count()
                != definicao.parametros().size())
            throw erro(numeroLinha, "Parâmetro duplicado na definição de " + nome);

        List<String> corpo = new ArrayList<>();
        while (indice < linhas.size()) {
            int linhaCorpo = indice + 1;
            String texto = linhas.get(indice++);
            String codigo = semComentario(texto).trim();
            if (codigo.isEmpty()) {
                corpo.add(texto);
                continue;
            }
            if (ehMend(codigo)) {
                macros.put(nome, new Macro(nome, definicao.parametros(), List.copyOf(corpo), numeroLinha));
                return;
            }
            Definicao interna = analisarDefinicao(codigo);
            if (interna != null) {
                lerDefinicao(interna, linhaCorpo, profundidade + 1);
                continue;
            }
            corpo.add(texto);
        }
        throw erro(numeroLinha, "Definição de macro " + nome + " sem MEND");
    }

    private void processarLinhaExpandida(
            String linha, Map<String, String> substituicoes, int profundidade, int numeroChamada) {
        String codigo = semComentario(linha).trim();
        if (codigo.isEmpty()) {
            saida.add(linha);
            return;
        }
        LinhaExpansivel chamada = analisarChamada(codigo);
        Macro macro = chamada == null ? null : macros.get(normalizar(chamada.operador()));
        if (macro == null) {
            saida.add(linha);
            return;
        }
        if (profundidade >= LIMITE_EXPANSAO)
            throw erro(numeroChamada, "Limite de expansão de macros excedido (possível recursão)");
        List<String> argumentos = analisarArgumentos(chamada.operandos());
        if (argumentos.size() != macro.parametros().size())
            throw erro(numeroChamada, "Macro " + macro.nome() + " espera "
                + macro.parametros().size() + " argumento(s), recebeu " + argumentos.size());

        Map<String, String> locais = new LinkedHashMap<>(substituicoes);
        for (int i = 0; i < argumentos.size(); i++)
            locais.put(macro.parametros().get(i), substituir(argumentos.get(i), substituicoes));
        int inicioExpansao = saida.size();
        for (String linhaCorpo : macro.corpo())
            processarLinhaExpandida(substituir(linhaCorpo, locais), locais, profundidade + 1, numeroChamada);
        aplicarRotulo(chamada.rotulo(), inicioExpansao);
    }

    private void aplicarRotulo(String rotulo, int inicioExpansao) {
        if (rotulo == null) return;
        for (int i = inicioExpansao; i < saida.size(); i++) {
            String linha = saida.get(i);
            String codigo = semComentario(linha).trim();
            if (codigo.isEmpty()) continue;
            if (Linha.ler(1, codigo).rotulo() == null) {
                saida.set(i, rotulo + " " + linha);
            }
            return;
        }
    }

    private static Definicao analisarDefinicao(String codigo) {
        String[] campos = codigo.split("\\s+", 3);
        if (campos.length >= 2 && normalizar(campos[1]).equals(MACRO))
            return new Definicao(campos[0], campos.length == 3 ? analisarArgumentos(campos[2]) : List.of());
        if (campos.length >= 2 && normalizar(campos[0]).equals(MACRO))
            return new Definicao(campos[1], campos.length == 3 ? analisarArgumentos(campos[2]) : List.of());
        return null;
    }

    private static boolean ehMend(String codigo) {
        String[] campos = codigo.split("\\s+", 2);
        return normalizar(campos[0]).equals(MEND);
    }

    private LinhaExpansivel analisarChamada(String codigo) {
        String[] campos = codigo.split("\\s+", 3);
        if (campos.length == 0) return null;
        String operador;
        String rotulo = null;
        String operandos;
        if (macros.containsKey(normalizar(campos[0]))) {
            operador = campos[0];
            operandos = campos.length == 3 ? campos[2] : campos.length == 2 ? campos[1] : "";
        } else if (campos.length >= 2 && macros.containsKey(normalizar(campos[1]))) {
            rotulo = campos[0];
            operador = campos[1];
            operandos = campos.length == 3 ? campos[2] : "";
        } else {
            return null;
        }
        return new LinhaExpansivel(rotulo, operador, operandos);
    }

    private static List<String> analisarArgumentos(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        List<String> argumentos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        char delimitador = 0;
        for (int i = 0; i < texto.length(); i++) {
            char caractere = texto.charAt(i);
            if ((caractere == '\'' || caractere == '"') && (delimitador == 0 || delimitador == caractere))
                delimitador = delimitador == 0 ? caractere : 0;
            if (caractere == ',' && delimitador == 0) {
                adicionarArgumento(argumentos, atual);
            } else {
                atual.append(caractere);
            }
        }
        adicionarArgumento(argumentos, atual);
        return List.copyOf(argumentos);
    }

    private static void adicionarArgumento(List<String> argumentos, StringBuilder atual) {
        String argumento = atual.toString().trim();
        if (!argumento.isEmpty()) argumentos.add(argumento);
        atual.setLength(0);
    }

    private static String substituir(String texto, Map<String, String> substituicoes) {
        String resultado = texto;
        for (Map.Entry<String, String> entrada : substituicoes.entrySet()) {
            Pattern padrao = Pattern.compile("(?i)(?<![A-Za-z0-9_])"
                + Pattern.quote(entrada.getKey()) + "(?![A-Za-z0-9_])");
            Matcher matcher = padrao.matcher(resultado);
            resultado = matcher.replaceAll(Matcher.quoteReplacement(entrada.getValue()));
        }
        return resultado;
    }

    private static String semComentario(String texto) {
        int comentario = texto.indexOf(';');
        return comentario < 0 ? texto : texto.substring(0, comentario);
    }

    private static String normalizar(String nome) {
        return nome.toUpperCase(Locale.ROOT);
    }

    private static IllegalArgumentException erro(int numeroLinha, String mensagem) {
        return new IllegalArgumentException("Linha " + numeroLinha + ": " + mensagem);
    }
}
