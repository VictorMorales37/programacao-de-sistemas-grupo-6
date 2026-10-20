package montador;

/**
 * Cópia da tabela de opcodes de {@code cpu.instrucoes.Opcode} (MOT).
 * <p>
 * O montador NÃO pode depender do pacote cpu; por isso os 38 mnemônicos, seus códigos
 * e seus formatos são repetidos aqui de forma intencional e devem permanecer idênticos:
 * <pre>
 * LDA=00, LDX=04, LDL=08, STA=0C, STX=10, STL=14, SUB=1C, MUL=20, COMP=28, TIX=2C,
 * JEQ=30, JGT=34, JLT=38, ADD=18, J=3C, AND=40, OR=44, JSUB=48, RSUB=4C, LDCH=50,
 * STCH=54, LDB=68, LDS=6C, LDT=74, STB=78, STS=7C, STT=84, DIV=24, SUBR=94,
 * MULR=98, COMPR=A0, RMO=AC, SHIFTL=A4, SHIFTR=A8, TIXR=B8, CLEAR=B4, DIVR=9C,
 * ADDR=90  (os dez últimos são de formato 2, instrução de registrador)
 * </pre>
 * Todos os demais são de formato 3. Os bits n/i (mascara Opcode.decodificar() &amp; 0xFC)
 * pertencem à codificação, não a esta tabela.
 */
public enum TabelaInstrucoes {
    ADD(0x18), ADDR(0x90, 2), AND(0x40), CLEAR(0xB4, 2), COMP(0x28), COMPR(0xA0, 2),
    DIV(0x24), DIVR(0x9C, 2), J(0x3C), JEQ(0x30), JGT(0x34), JLT(0x38), JSUB(0x48),
    LDA(0x00), LDB(0x68), LDCH(0x50), LDL(0x08), LDS(0x6C), LDT(0x74), LDX(0x04),
    MUL(0x20), MULR(0x98, 2), OR(0x44), RMO(0xAC, 2), RSUB(0x4C),
    SHIFTL(0xA4, 2), SHIFTR(0xA8, 2), STA(0x0C), STB(0x78), STCH(0x54),
    STL(0x14), STS(0x7C), STT(0x84), STX(0x10), SUB(0x1C), SUBR(0x94, 2), TIX(0x2C), TIXR(0xB8, 2);

    private final int codigo;
    private final int formato;

    TabelaInstrucoes(int codigo) { this(codigo, 3); }
    TabelaInstrucoes(int codigo, int formato) { this.codigo = codigo; this.formato = formato; }

    public int codigo() { return codigo; }
    public int formato() { return formato; }

    /** Encontra a instrução pelo mnemônico (sem diferenciar maiúsculas), ou null. */
    public static TabelaInstrucoes de(String mnemonico) {
        try {
            return valueOf(mnemonico.toUpperCase());
        } catch (IllegalArgumentException desconhecida) {
            return null;
        }
    }

    /** Números de registradores iguais aos de cpu.Registradores: A=0 .. F=6, PC=8, SW=9. */
    public static Integer codigoRegistrador(String nome) {
        return switch (nome.toUpperCase()) {
            case "A" -> 0;
            case "X" -> 1;
            case "L" -> 2;
            case "B" -> 3;
            case "S" -> 4;
            case "T" -> 5;
            case "F" -> 6;
            case "PC" -> 8;
            case "SW" -> 9;
            default -> null;
        };
    }
}
