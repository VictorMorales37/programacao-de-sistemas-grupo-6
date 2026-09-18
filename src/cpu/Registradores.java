package cpu;

// A=0, X=1, L=2, B=3, S=4, T=5, F=6, PC=8 e SW=9. 

public class Registradores {

    private final Registrador a;
    private final Registrador x;
    private final Registrador l;
    private final Registrador b;
    private final Registrador s;
    private final Registrador t;
    private final Registrador f;
    private final Registrador pc;
    private final SW sw;

    public Registradores() {
        a = new Registrador("A", 0, Registrador.TAMANHO_24_BITS); // Acumulador
        x = new Registrador("X", 1, Registrador.TAMANHO_24_BITS); // Registrador de índice
        l = new Registrador("L", 2, Registrador.TAMANHO_24_BITS); // Registrador de ligação
        b = new Registrador("B", 3, Registrador.TAMANHO_24_BITS); // Registrador Base
        s = new Registrador("S", 4, Registrador.TAMANHO_24_BITS); // Geral
        t = new Registrador("T", 5, Registrador.TAMANHO_24_BITS); // Geral
        f = new Registrador("F", 6, Registrador.TAMANHO_48_BITS); // Acumulador de ponto flutuante
        pc = new Registrador("PC", 8, Registrador.TAMANHO_24_BITS); // Program Counter
        sw = new SW(); // Palavra de status - várias informações, incluindo código condicional (CC)
    }

    public Registrador getA() {
        return a;
    }

    public Registrador getX() {
        return x;
    }

    public Registrador getL() {
        return l;
    }

    public Registrador getB() {
        return b;
    }

    public Registrador getS() {
        return s;
    }

    public Registrador getT() {
        return t;
    }

    public Registrador getF() {
        return f;
    }

    public Registrador getPC() {
        return pc;
    }

    public SW getSW() {
        return sw;
    }

    public Registrador getPorNumero(int numero) {
        return switch (numero) {
            case 0 -> a;
            case 1 -> x;
            case 2 -> l;
            case 3 -> b;
            case 4 -> s;
            case 5 -> t;
            case 6 -> f;
            case 8 -> pc;
            case 9 -> sw;
            default -> throw new IllegalArgumentException("Numero de registrador invalido: " + numero);
        };
    }

    public Registrador getPorIdentificador(String identificador) {
        if (identificador == null) {
            throw new IllegalArgumentException("Identificador nao pode ser nulo");
        }

        return switch (identificador.toUpperCase()) {
            case "A" -> a;
            case "X" -> x;
            case "L" -> l;
            case "B" -> b;
            case "S" -> s;
            case "T" -> t;
            case "F" -> f;
            case "PC" -> pc;
            case "SW" -> sw;
            default -> throw new IllegalArgumentException("Identificador de registrador invalido: " + identificador);
        };
    }

    public void limpar() {
        a.limpar();
        x.limpar();
        l.limpar();
        b.limpar();
        s.limpar();
        t.limpar();
        f.limpar();
        pc.limpar();
        sw.limpar();
    }
}
