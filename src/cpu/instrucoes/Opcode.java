package cpu.instrucoes;

import java.util.Arrays;

/** Apenas as instruções obrigatórias da especificação podem ser executadas. */
public enum Opcode {
    ADD(0x18), ADDR(0x90, 2), AND(0x40), CLEAR(0xB4, 2), COMP(0x28), COMPR(0xA0, 2),
    DIV(0x24), DIVR(0x9C, 2), J(0x3C), JEQ(0x30), JGT(0x34), JLT(0x38), JSUB(0x48),
    LDA(0x00), LDB(0x68), LDCH(0x50), LDL(0x08), LDS(0x6C), LDT(0x74), LDX(0x04),
    MUL(0x20), MULR(0x98, 2), OR(0x44), RMO(0xAC, 2), RSUB(0x4C),
    SHIFTL(0xA4, 2), SHIFTR(0xA8, 2), STA(0x0C), STB(0x78), STCH(0x54),
    STL(0x14), STS(0x7C), STT(0x84), STX(0x10), SUB(0x1C), SUBR(0x94, 2), TIX(0x2C), TIXR(0xB8, 2);

    private final int codigo;
    private final int formato;

    Opcode(int codigo) { this(codigo, 3); }
    Opcode(int codigo, int formato) { this.codigo = codigo; this.formato = formato; }
    public int codigo() { return codigo; }
    public int formato() { return formato; }

    public static Opcode decodificar(int primeiroByte) {
        return Arrays.stream(values())
            .filter(candidato -> candidato.formato == 2
                ? primeiroByte == candidato.codigo
                : (primeiroByte & 0xFC) == candidato.codigo)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Código de operação não suportado ou excluído: %02X", primeiroByte)));
    }
}
