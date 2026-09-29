package cpu.instrucoes;

public record Instrucao(Opcode codigoOperacao, int formato, int endereco, int proximoEndereco,
        int primeiroRegistrador, int segundoRegistrador, boolean n, boolean i, boolean x,
        boolean b, boolean p, boolean e, int campo, String hexadecimal) {

    public boolean ehImediata() { return !n && i; }
    public boolean ehIndireta() { return n && !i; }
    public boolean ehSic() { return !n && !i; }
    public String indicadores() {
        return formato == 2 ? "registrador"
            : String.format("n%d i%d x%d b%d p%d e%d", n ? 1 : 0, i ? 1 : 0, x ? 1 : 0,
                b ? 1 : 0, p ? 1 : 0, e ? 1 : 0);
    }
}
