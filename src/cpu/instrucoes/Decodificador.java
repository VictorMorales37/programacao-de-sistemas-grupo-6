package cpu.instrucoes;

import memoria.Memoria;

public final class Decodificador {
    public Instrucao decodificar(Memoria memoria, int endereco) {
        int deslocamento = Memoria.converterParaEnderecoEmBytes(endereco);
        int primeiroByte = memoria.lerByte(deslocamento);
        Opcode codigoOperacao = Opcode.decodificar(primeiroByte);
        int segundoByte = memoria.lerByte(deslocamento + 1);
        int formato = codigoOperacao.formato();
        boolean n = false, i = false, x = false, b = false, p = false, e = false;
        int campo = 0;

        if (formato != 2) {
            n = (primeiroByte & 2) != 0;
            i = (primeiroByte & 1) != 0;
            x = (segundoByte & 128) != 0;
            if (!n && !i) {
                campo = ((segundoByte & 127) << 8) | memoria.lerByte(deslocamento + 2);
            } else {
                b = (segundoByte & 64) != 0;
                p = (segundoByte & 32) != 0;
                e = (segundoByte & 16) != 0;
                if (b && p || e && (b || p)) throw new IllegalArgumentException("Combinação inválida dos bits de controle b/p/e");
                if (x && !(n && i)) throw new IllegalArgumentException("A indexação exige endereçamento simples");
                formato = e ? 4 : 3;
                campo = ((segundoByte & 15) << 8) | memoria.lerByte(deslocamento + 2);
                if (e) campo = (campo << 8) | memoria.lerByte(deslocamento + 3);
            }
        }

        StringBuilder hexadecimal = new StringBuilder();
        for (int indice = 0; indice < formato; indice++)
            hexadecimal.append(String.format("%02X", memoria.lerByte(deslocamento + indice)));

        return new Instrucao(codigoOperacao, formato, endereco, endereco + ((formato + 2) / 3),
            segundoByte >>> 4, segundoByte & 15, n, i, x, b, p, e, campo, hexadecimal.toString());
    }
}
