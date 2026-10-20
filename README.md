# programacao-de-sistemas-grupo-6

# Simulador SIC/XE

Projeto de um simulador SIC/XE, computador hipotético baseado na
arquitetura do livro: Systems Software: An Introduction to System
Programming; Leland L. Beck

## Parte I

## Estruturas de dados

| Estrutura | Descrição |
| --- | --- |
| `Memoria` | Vetor de bytes com capacidade padrão de 3 MiB. Fornece acesso validado a bytes e a palavras de três bytes em big-endian. |
| `Palavra` | Representa uma palavra de 24 bits usando um inteiro limitado por `0xFFFFFF`. Também permite interpretação com sinal em complemento de dois. |
| `Registrador` | Armazena identificador, número, largura e valor de um registrador. Usa `long` e uma máscara de bits para suportar registradores de 24 e 48 bits. |
| `Registradores` | Contém A, X, L, B, S, T, F, PC e SW. Permite consulta, limpeza e captura/restauração do estado dos registradores. |
| `SW` | Estende `Registrador` e representa a palavra de estado. Seus dois bits menos significativos codificam IGUAL, MENOR ou MAIOR. |
| `Opcode` | Enumeração que associa as 38 instruções exigidas aos respectivos códigos de máquina e formatos base. |
| `Instrucao` | Registro imutável com codigoOperacao decodificado, formato, endereços, registradores operandos, indicadores de endereçamento e campo codificado. |
| `ImagemPrograma` | Armazena as entradas iniciais do programa, o endereço de entrada, o endereço de parada e o conjunto de endereços válidos de instrução. |
| `ImagemPrograma.Entrada` | Registro com endereço, vetor de bytes copiado defensivamente e indicador de entrada executável. |

## Funções principais

| Função | Responsabilidade |
| --- | --- |
| `Memoria.lerByte()` / `escreverByte()` | Lê ou escreve um byte físico depois de validar o endereço. |
| `Memoria.lerPalavra()` / `escreverPalavra()` | Lê ou escreve uma palavra completa de 24 bits. |
| `Registradores.capturarEstado()` / `restaurarEstado()` | Salva e restaura os valores dos registradores quando ocorre um erro de execução. |
| `SW.comparar()` | Atualiza o código de condição a partir de uma comparação. |
| `Decodificador.decodificar()` | Interpreta os bytes da instrução, extrai operandos e indicadores e calcula o endereço da próxima instrução. |
| `ExecutorInstrucoes.calcularEnderecoEfetivo()` | Calcula o endereço efetivo, incluindo endereçamento relativo, indexado e indireto. |
| `ExecutorInstrucoes.executar()` | Executa a operação aritmética, lógica, de transferência, comparação ou desvio da instrução decodificada. |
| `CPU.executarPasso()` | Executa um ciclo de busca, decodificação e execução e contabiliza as instruções concluídas. |
| `CarregadorPrograma.analisar()` | Valida uma listagem hexadecimal e cria a imagem inicial do programa. |
| `ImagemPrograma.criarCPU()` | Cria uma nova máquina com a memória inicial do programa e o PC configurado. |
| `PainelSimulador.carregarFonte()` | Carrega ou reinicia o programa exibido no editor. |
| `PainelSimulador.executarPasso()` / `atualizar()` | Executa uma instrução e atualiza a exibição do estado da máquina. |
| `PainelSimulador.executarTudo()` / `executarLote()` | Executa automaticamente em lotes limitados, mantendo a interface responsiva. |
| `Main.main()` | Inicializa a aplicação Swing. |

## Estratégias de implementação

### Separação de responsabilidades

A memória e os registradores representam o estado da máquina. O decodificador interpreta a codificação das instruções, enquanto o executor implementa o comportamento de cada instrução. A CPU coordena esses componentes. A interface solicita operações e apresenta os resultados.

### Endereçamento de memória

A especificação do trabalho define endereçamento por palavra, mas descreve instruções de tamanho variável usando notação por bytes. Esta implementação adota endereços lógicos por palavra: o endereço `a` corresponde ao deslocamento físico de bytes `3 * a`.

Cada instrução começa no limite de uma palavra. Os formatos 2 e 3 ocupam uma palavra; o formato 4 ocupa duas palavras, com bytes finais não utilizados reservados como preenchimento. PC, B, X e os cálculos de endereço usam unidades de palavra. `LDCH` e `STCH` acessam o primeiro byte da palavra endereçada.

### Execução das instruções

O simulador implementa as 38 instruções não destacadas em vermelho no enunciado. Instruções de ponto flutuante e de dispositivos marcadas em vermelho são rejeitadas; F permanece disponível como armazenamento de 48 bits.

O decodificador suporta os formatos 2, 3 e 4, além da compatibilidade SIC. São aceitos os modos imediato, simples, indireto, indexado, relativo à base e relativo ao PC. Deslocamentos relativos ao PC usam interpretação com sinal de 12 bits.

As operações aritméticas usam operandos de 24 bits com sinal; as máscaras dos registradores preservam os 24 bits menos significativos do resultado. Divisão por zero é rejeitada. O PC avança antes da execução para que o endereçamento relativo e `JSUB` usem o endereço da próxima instrução.

### Validação e tratamento de erros

As operações de memória validam todo o intervalo antes da escrita. Opcodes inválidos, combinações inválidas de indicadores, entradas de programa sobrepostas e endereços de execução inválidos são rejeitados.

Se a execução falhar, a CPU restaura os valores dos registradores e não incrementa o contador de ciclos. A interface exibe o erro e exige um novo carregamento antes de continuar a execução.

### Entrada do programa e conclusão

O carregador aceita os comandos `ENTRY`, `STOP`, `CODE` e `WORD`. Endereços e valores são hexadecimais, e `;` inicia um comentário.

O carregador lê diretamente código de máquina; ele não funciona como montador. `STOP` define um limite externo de conclusão, e não uma instrução adicional da máquina.

### Controles de execução

**Passo** executa uma instrução. **Executar** continua automaticamente a partir do estado atual da máquina até `STOP` ou até ocorrer um erro de execução. Durante a execução, o mesmo botão passa a ser **Pausar**, permitindo retornar ao modo manual. **Carregar / reiniciar** interrompe a execução e recarrega o conteúdo do editor.

Um temporizador Swing executa lotes de até 100 instruções e devolve o controle à linha de execução de eventos entre os lotes. As tabelas de registradores e memória são atualizadas depois de cada lote. O botão **Passo** fica desabilitado durante a execução contínua. Cada sessão de **Executar** pausa após 100.000 instruções para impedir que um laço infinito acidental continue indefinidamente; depois disso, a execução pode ser inspecionada ou retomada.

### Decisões sobre a especificação

Todos os nove registradores listados no trabalho são implementados. `CLEAR` usa o codigoOperacao `B4`, preservando `04` para `LDX`. O formato 2 usa um codigoOperacao de 8 bits seguido por dois campos de operandos de 4 bits. Essas decisões resolvem inconsistências presentes na especificação fornecida.

## Parte II

## Montador de dois passos

O montador traduz linguagem de montagem SIC/XE para a imagem de programa executada pelo simulador. Ele está em um pacote próprio (`montador/`), totalmente desacoplado do resto do projeto: a interface converte o `ProgramaMontado` produzido em uma `ImagemPrograma` e o fluxo de execução não muda.

### Entrada da montagem

```
        LDA   #0        ; comentário
VOLTA   ADDR  X,A
SOMA    RESW  1
```

- Uma linha tem de 1 a 4 campos separados por espaços: `[rótulo] operador [opd1 [opd2]]`.
- **Desambiguação:** o primeiro campo é o operador quando o token (em maiúsculas e sem o prefixo `+`) é um mnemônico ou diretiva conhecido; caso contrário é rótulo. Por consequência, um rótulo não pode ter o mesmo nome de um mnemônico.
- Números são hexadecimais, como no carregador de código.
- Operandos: `#valor` imediato, `@alvo` indireto, `alvo,X` indexado, `+NLDM` formato 4. Instruções de formato 2 usam nomes de registradores (A, X, L, B, S, T, F, PC, SW); `CLEAR`/`TIXR` aceitam um registrador e `SHIFTL`/`SHIFTR` aceitam registrador e deslocamento (`SHIFTL A,3`).
- Diretivas: `START fim` (deve ser a primeira linha com operador; define a LC inicial e o início de execução padrão), `WORD valor`, `RESW n` (reserva n palavras), `RESB n` (reserva n bytes, arredondados para cima em palavras; emite aviso quando há preenchimento desperdiçado), `END [início]` (início de execução; padrão: endereço inicial), **`EQU valor`** (defini o rótulo com o valor — número ou símbolo já definido — sem ocupar memória), **`ORG v`** (reinicializa a LC; rótulos em ORG/BASE não definem símbolo), **`BASE v`** (valor que B terá na execução) e **`BYTE X'0F1A'` / `BYTE C'ab'`** (constantes em hexa por pares de dígitos ou caracteres; ocupam as palavras necessárias, arredondadas para cima).

### Passos, símbolos e encerramento

- **Passo 1:** LC (contador de posição, em palavras de 3 bytes), tabela de símbolos e validações (símbolo duplicado, operador inválido, quantidade de operandos).
- **Passo 2:** byte a byte do código, com resolução de símbolos e geração da listagem (símbolo indefinido, endereços fora de faixa).
- **Símbolo implícito `STOP`:** primeira palavra livre após o programa. Programas são encerrados com `J STOP`, e o Simulador conclui quando PC chega a esse endereço — a mesma convenção do carregador.
- Instruções de formato 2 e 3 ocupam 1 palavra; formato 4, 2 palavras.

### Codificação de formato 3

- Sem operando (só `RSUB`): `ni = 00`.
- Imediato: `n=0 i=1`; campo = valor (0..0xFFF).
- Indireto: `n=1 i=0`; simples: `n=1 i=1` (obrigatório para permitir indexação).
- Campo do alvo: direto se alvo ≤ 0xFFF; senão PC-relativo (janela ±2048 palavras, bit `p`); senão **base-relativo** (se `BASE` ativo e deslocamento 0..0xFFF, bit `b`); caso contrário erro sugerindo formato 4. A diretiva `BASE` documenta o valor que o programa carrega em B (o montador não gera carga de B).
- Imediato com símbolo (ex.: `#CONTADOR`) usa o valor do símbolo; imediato negativo (`#-5`) não é suportado — o campo da instrução é sem sinal — e gera erro com sugestão de uso.
- Indexação `,X` exige endereçamento simples, igual ao decodificador da CPU.

### Estruturas de dados

| Estrutura | Descrição |
| --- | --- |
| `Linha` | Analisa uma linha do fonte: remove comentários, separa campos e desambígua rótulo/operador. |
| `TabelaInstrucoes` | MOT do montador: os 38 mnemônicos e seus códigos/formatos com os mesmos valores de `Opcode` (copia intencional, `montador` não depende de `cpu`). |
| `TabelaDiretivas` | POT: diretivas `START`, `END`, `WORD`, `RESW`, `RESB`, `EQU`, `ORG`, `BASE`, `BYTE`. |
| `TabelaSimbolos` | TS mapeando rótulos aos endereços (passo 1 preenche; passo 2 consulta). |
| `ProgramaMontado` | Saída: entradas de memória, endereço de entrada, endereço de parada (STOP), tabela de símbolos, listagem e erros. |
| `Montador` | Orquestra os dois passos de `montar(fonte)`; erros são acumulados e nunca lançados. |

### Saída e integração

O método `Montador.montar(fonte)` devolve o `ProgramaMontado` a quem chamou. A interface chama o montador e converte as entradas para `ImagemPrograma` (endereço, bytes e indicação de instrução executável); com erros, exibe erros, listagem e tabela de símbolos. O carregador legado em hexa continua funcionando: a interface detecta o formato pela primeira linha (comandos `ENTRY`/`STOP`/`CODE`/`WORD` → código de máquina; senão → montador).

## Como executar

Requer Java 17 ou superior.

```sh
java -jar dist/sic-xe-simulator.jar
```

No Windows, `run.bat` inicia a aplicação. Para recompilar, é necessário ter um JDK completo instalado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
```

Extraia completamente o arquivo ZIP antes de abrir `run.bat`. Na interface, use **Carregar / reiniciar** para recomeçar, **Passo** para avançar manualmente ou **Executar** para executar o programa automaticamente.

O exemplo incluído calcula `1 + 2 + 3 + 4 + 5`. Depois de 19 passos, A e o endereço de memória `000100` contêm `00000F`, equivalente a 15 em decimal.
