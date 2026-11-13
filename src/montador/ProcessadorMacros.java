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

/** Pré-processador de uma passagem: copia, define ou expande na ordem do fonte. */
public final class ProcessadorMacros {
    private static final int LIMITE_EXPANSAO = 100;
    private static final Pattern IDENTIFICADOR = Pattern.compile("&?[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern TOKEN = Pattern.compile("(?i)\\.SER(?![A-Za-z0-9_])|(?<![A-Za-z0-9_&])&?[A-Za-z_][A-Za-z0-9_]*");
    private interface Item {}
    private record Texto(String valor, int linha) implements Item {}
    private record Definicao(String nome, List<String> parametros, String parametroRotulo,
                             List<Item> corpo, int linha) implements Item {}
    private record Macro(Definicao definicao, Map<String, String> capturados) {}
    private record Cabecalho(String nome, List<String> parametros, String parametroRotulo) {}
    private record Chamada(String rotulo, String nome, String operandos) {}

    private final Map<String, Macro> macros = new LinkedHashMap<>();
    private final List<String> saida = new ArrayList<>();
    private final List<String> linhas;
    private int indice;
    private long serial;

    private ProcessadorMacros(String fonte) {
        // O BOM só é marcador de codificação quando está no início do arquivo.
        linhas = List.of((fonte.startsWith("\uFEFF") ? fonte.substring(1) : fonte).split("\\R", -1));
    }

    public static String processar(String fonte) {
        ProcessadorMacros p = new ProcessadorMacros(fonte);
        while (p.indice < p.linhas.size()) p.executar(p.lerItem(), Map.of(), 0, 0, 0);
        return String.join(System.lineSeparator(), p.saida);
    }

    public static Path processarArquivo(Path arquivoFonte) throws IOException {
        return processarEGravar(Files.readString(arquivoFonte, StandardCharsets.UTF_8), Path.of("MASMAPRG.ASM"));
    }

    public static Path processarEGravar(String fonte, Path arquivoSaida) throws IOException {
        String expandido = processar(fonte);
        Path destino = arquivoSaida.toAbsolutePath();
        Files.writeString(destino, expandido, StandardCharsets.UTF_8);
        return destino;
    }

    private Item lerItem() {
        int numero = indice + 1;
        String texto = linhas.get(indice++);
        String codigo = semComentario(texto).trim();
        if (ehFim(codigo)) throw erro(numero, "MEND/MCEND sem uma definição MACRO correspondente");
        Cabecalho cabecalho = cabecalho(codigo, numero);
        if (cabecalho == null) return new Texto(texto, numero);
        return lerDefinicao(cabecalho, numero, 0);
    }

    private Definicao lerDefinicao(Cabecalho cabecalho, int numero, int nivel) {
        if (nivel >= LIMITE_EXPANSAO) throw erro(numero, "Limite de definições aninhadas excedido");
        validarCabecalho(cabecalho, numero);
        List<Item> corpo = new ArrayList<>();
        while (indice < linhas.size()) {
            int linha = indice + 1;
            String texto = linhas.get(indice++);
            String codigo = semComentario(texto).trim();
            if (ehFim(codigo)) {
                if (codigo.split("\\s+").length != 1) throw erro(linha, "MEND/MCEND não aceita operandos");
                return new Definicao(normalizar(cabecalho.nome()),
                    cabecalho.parametros().stream().map(ProcessadorMacros::normalizar).toList(),
                    cabecalho.parametroRotulo(), List.copyOf(corpo), numero);
            }
            Cabecalho interna = cabecalho(codigo, linha);
            corpo.add(interna == null ? new Texto(texto, linha) : lerDefinicao(interna, linha, nivel + 1));
        }
        throw erro(numero, "Definição de macro " + cabecalho.nome() + " sem MEND/MCEND");
    }

    private void executar(Item item, Map<String, String> ambiente, int profundidade, int linhaChamada, long numeroSerial) {
        if (item instanceof Definicao definicao) {
            // Só ativa uma definição interna quando a expansão chega até ela.
            Map<String, String> capturados = new LinkedHashMap<>(ambiente);
            definicao.parametros().forEach(capturados::remove);
            if (definicao.parametroRotulo() != null) capturados.remove(definicao.parametroRotulo());
            macros.put(definicao.nome(), new Macro(definicao, Map.copyOf(capturados)));
            return;
        }
        Texto texto = (Texto) item;
        String linha = substituir(texto.valor(), ambiente, numeroSerial);
        String codigo = semComentario(linha).trim();
        Chamada chamada = analisarChamada(codigo);
        if (chamada == null) {
            saida.add(linha);
            return;
        }
        int origem = linhaChamada == 0 ? texto.linha() : linhaChamada;
        if (profundidade >= LIMITE_EXPANSAO)
            throw erro(origem, "Limite de expansão de macros excedido (possível recursão)");
        Macro macro = macros.get(chamada.nome());
        Definicao d = macro.definicao();
        List<String> argumentos = argumentos(chamada.operandos(), origem);
        if (argumentos.size() != d.parametros().size())
            throw erro(origem, "Macro " + d.nome() + " espera " + d.parametros().size()
                + " argumento(s), recebeu " + argumentos.size());
        Map<String, String> locais = new LinkedHashMap<>(macro.capturados());
        for (int i = 0; i < argumentos.size(); i++) locais.put(d.parametros().get(i), argumentos.get(i));
        if (d.parametroRotulo() != null)
            locais.put(d.parametroRotulo(), chamada.rotulo() == null ? "" : chamada.rotulo());
        int inicio = saida.size();
        long serialAtual = ++serial;
        for (Item filho : d.corpo()) executar(filho, locais, profundidade + 1, origem, serialAtual);
        if (d.parametroRotulo() == null) aplicarRotulo(chamada.rotulo(), inicio, origem);
    }

    private void aplicarRotulo(String rotulo, int inicio, int numero) {
        if (rotulo == null) return;
        for (int i = inicio; i < saida.size(); i++) {
            String codigo = semComentario(saida.get(i)).trim();
            if (codigo.isEmpty()) continue;
            if (Linha.ler(1, codigo).rotulo() != null)
                throw erro(numero, "Rótulo da chamada " + rotulo + " conflita com o rótulo da expansão; use um parâmetro de rótulo");
            saida.set(i, rotulo + " " + saida.get(i));
            return;
        }
        throw erro(numero, "Macro sem instruções não pode receber o rótulo " + rotulo);
    }

    private Cabecalho cabecalho(String codigo, int numero) {
        if (codigo.isEmpty()) return null;
        String[] campos = codigo.split("\\s+", 3);
        if (campos.length == 1 && ehInicio(campos[0])) {
            // Forma dos slides: delimitador seguido pelo protótipo [rótulo] NOME parâmetros.
            while (indice < linhas.size()) {
                String prototipo = semComentario(linhas.get(indice++)).trim();
                if (prototipo.isEmpty()) continue;
                String[] partes = prototipo.split("\\s+", 3);
                if (ehFim(prototipo) || ehInicio(partes[0])) throw erro(numero, "Protótipo de macro ausente");
                boolean temRotulo = partes.length == 3 && !partes[1].contains(",")
                    && !partes[2].startsWith(",") && !partes[1].startsWith("&");
                return new Cabecalho(partes[temRotulo ? 1 : 0],
                    argumentos(temRotulo ? partes[2] : prototipo.substring(partes[0].length()).trim(), numero),
                    temRotulo ? normalizar(partes[0]) : null);
            }
            throw erro(numero, "Protótipo de macro ausente");
        }
        if (campos.length >= 2 && ehInicio(campos[1]))
            return new Cabecalho(campos[0], argumentos(campos.length == 3 ? campos[2] : "", numero), null);
        if (campos.length >= 2 && ehInicio(campos[0]))
            return new Cabecalho(campos[1], argumentos(campos.length == 3 ? campos[2] : "", numero), null);
        return null;
    }

    private static void validarCabecalho(Cabecalho c, int numero) {
        if (!c.nome().matches("[A-Za-z_][A-Za-z0-9_]*") || ehInicio(c.nome()) || ehFim(c.nome())
                || TabelaInstrucoes.de(c.nome()) != null || TabelaDiretivas.de(c.nome()) != null)
            throw erro(numero, "Nome de macro inválido ou reservado: " + c.nome());
        List<String> parametros = new ArrayList<>(c.parametros());
        if (c.parametroRotulo() != null) parametros.add(c.parametroRotulo());
        for (String p : parametros)
            if (!IDENTIFICADOR.matcher(p).matches()) throw erro(numero, "Parâmetro inválido: " + p);
        if (parametros.stream().map(ProcessadorMacros::normalizar).distinct().count() != parametros.size())
            throw erro(numero, "Parâmetro duplicado na definição de " + c.nome());
    }

    private Chamada analisarChamada(String codigo) {
        if (codigo.isEmpty()) return null;
        String[] campos = codigo.split("\\s+", 3);
        String primeiro = normalizar(campos[0]);
        if (macros.containsKey(primeiro))
            return new Chamada(null, primeiro, codigo.substring(campos[0].length()).trim());
        // Um nome de macro no operando de uma instrução não é uma chamada.
        String operador = primeiro.startsWith("+") ? primeiro.substring(1) : primeiro;
        if (TabelaInstrucoes.de(operador) != null || TabelaDiretivas.de(operador) != null) return null;
        if (campos.length >= 2 && macros.containsKey(normalizar(campos[1])))
            return new Chamada(campos[0], normalizar(campos[1]), campos.length == 3 ? campos[2] : "");
        return null;
    }

    private static List<String> argumentos(String texto, int numero) {
        List<String> resultado = new ArrayList<>();
        if (texto.isBlank()) return resultado;
        int inicio = 0;
        char aspas = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if ((c == '\'' || c == '"') && (aspas == 0 || aspas == c)) aspas = aspas == 0 ? c : 0;
            if (c == ',' && aspas == 0) {
                resultado.add(texto.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        if (aspas != 0) throw erro(numero, "Aspas não fechadas na lista de argumentos");
        resultado.add(texto.substring(inicio).trim());
        return resultado;
    }

    private static String substituir(String texto, Map<String, String> ambiente, long serial) {
        String codigo = semComentario(texto);
        Matcher matcher = TOKEN.matcher(codigo);
        StringBuilder resultado = new StringBuilder();
        while (matcher.find()) {
            String valor = ambiente.get(normalizar(matcher.group()));
            if (serial != 0 && matcher.group().equalsIgnoreCase(".SER"))
                valor = String.format(Locale.ROOT, "%04d", serial);
            // Substituição simultânea: um argumento não é reinterpretado como outro formal.
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valor == null ? matcher.group() : valor));
        }
        matcher.appendTail(resultado);
        return resultado + texto.substring(codigo.length());
    }

    private static String semComentario(String texto) {
        char aspas = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if ((c == '\'' || c == '"') && (aspas == 0 || aspas == c)) aspas = aspas == 0 ? c : 0;
            if (c == ';' && aspas == 0) return texto.substring(0, i);
        }
        return texto;
    }

    private static boolean ehInicio(String texto) {
        return normalizar(texto).equals("MACRO") || normalizar(texto).equals("MCDEFN");
    }

    private static boolean ehFim(String codigo) {
        String token = normalizar(codigo.split("\\s+", 2)[0]);
        return token.equals("MEND") || token.equals("MCEND");
    }

    private static String normalizar(String nome) { return nome.toUpperCase(Locale.ROOT); }
    private static IllegalArgumentException erro(int linha, String mensagem) {
        return new IllegalArgumentException("Linha " + linha + ": " + mensagem);
    }
}
