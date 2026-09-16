package memoria;

public class Palavra {
    private int valor;

    public Palavra(int valor) {
        this.valor = valor & 0xFFFFFF;
    }

    public int getValor() {
        return valor;
    }

    public void setValor(int valor) {
        this.valor = valor & 0xFFFFFF;
    }
}