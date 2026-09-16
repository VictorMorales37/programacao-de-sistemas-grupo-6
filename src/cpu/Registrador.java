package cpu;

public class Registrador {
    private int endereco;
    private int tamanhoBits;
    private long valor;

    public Registrador(int endereco, int tamanhoBits) {
        this.endereco = endereco;
        this.tamanhoBits = tamanhoBits;
    }

    public void setValor(long valor) {
        if (tamanhoBits == 24) {
            this.valor = valor & 0xFFFFFF;
        }
        if (tamanhoBits == 48) {
            this.valor = valor & 0xFFFFFFFFFFFFL;
        }
    }
}
