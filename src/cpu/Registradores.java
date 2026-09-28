package cpu;

import java.util.List;
import java.util.Locale;

public final class Registradores {
    private final Registrador a = new Registrador("A", 0, 24);
    private final Registrador x = new Registrador("X", 1, 24);
    private final Registrador l = new Registrador("L", 2, 24);
    private final Registrador b = new Registrador("B", 3, 24);
    private final Registrador s = new Registrador("S", 4, 24);
    private final Registrador t = new Registrador("T", 5, 24);
    private final Registrador f = new Registrador("F", 6, 48);
    private final Registrador pc = new Registrador("PC", 8, 24);
    private final SW sw = new SW();

    public Registrador obterA() { return a; }
    public Registrador obterX() { return x; }
    public Registrador obterL() { return l; }
    public Registrador obterB() { return b; }
    public Registrador obterS() { return s; }
    public Registrador obterT() { return t; }
    public Registrador obterF() { return f; }
    public Registrador obterPC() { return pc; }
    public SW obterSW() { return sw; }
    public List<Registrador> todos() { return List.of(a, x, l, b, s, t, f, pc, sw); }

    public Registrador obterPorNumero(int numero) {
        return todos().stream().filter(registrador -> registrador.obterNumero() == numero).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Número de registrador inválido: " + numero));
    }
    public Registrador obterPorIdentificador(String identificador) {
        if (identificador == null) throw new IllegalArgumentException("O identificador do registrador não pode ser nulo");
        return todos().stream().filter(registrador -> registrador.obterIdentificador().equals(identificador.toUpperCase(Locale.ROOT))).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Registrador inválido: " + identificador));
    }
    public void limpar() { todos().forEach(Registrador::limpar); }
    public long[] capturarEstado() { return todos().stream().mapToLong(Registrador::obterValor).toArray(); }
    public void restaurarEstado(long[] valores) {
        for (int indice = 0; indice < valores.length; indice++) todos().get(indice).definirValor(valores[indice]);
    }
}
