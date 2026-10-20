; Exemplo em assembly SIC/XE para o montador de dois passos.
; Soma 1 + 2 + 3 + 4 + 5; ao fim A = 00000F e SOMA = 00000F.
;
; Sintaxe: [rótulo] operador [operandos] ; comentário
;   - números são hexadecimais, como no carregador de código;
;   - #valor imediato, @símbolo indireto, símbolo,X indexado, +MNEMÔNICO formato 4;
;   - instruções de formato 2 usam nomes de registradores (ADDR X,A);
;   - STOP é um símbolo implícito do montador: a primeira palavra livre após o programa.

        LDA   #0        ; A = 0
        LDX   #1        ; X = 1
VOLTA   ADDR  X,A       ; ACC <- ACC + X
        TIX   #6        ; X <- X + 1; compara X com 6
        JLT   VOLTA     ; volta enquanto X < 6
        STA   SOMA      ; grava o resultado
        J     STOP      ; encerra a execução (PC == STOP)
SOMA    RESW  1         ; espaço reservado para o resultado
