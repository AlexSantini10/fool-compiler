package svm;

public class ExecuteVM {

    // Dimensione massima del codice eseguibile dalla VM.
    public static final int CODESIZE = 10000;

    // Dimensione totale della memoria della VM (condivisa da stack e heap).
    public static final int MEMSIZE = 10000;

    // Programma eseguibile: sequenza di opcode e operandi rappresentati come interi.
    private int[] code;

    // Memoria dati della VM: heap e stack condividono questo array.
    private int[] memory = new int[MEMSIZE];

    // Instruction Pointer: indice della prossima istruzione da leggere in code[].
    private int ip = 0;

    // Stack Pointer: punta all'ultima cella occupata dello stack (che cresce verso indirizzi più piccoli).
    private int sp = MEMSIZE;

    // Heap Pointer: punta alla prima cella libera dello heap (che cresce verso indirizzi più grandi).
    private int hp = 0;

    // Frame Pointer: base dell'Activation Record corrente.
    private int fp = MEMSIZE;

    // Return Address: indirizzo di ritorno dopo una js.
    private int ra;

    // Temporary register: registro di appoggio usato dal codegen per salvare valori temporanei.
    private int tm;

    // Costruttore: inizializza la VM con il codice già assemblato.
    public ExecuteVM(int[] code) {
        this.code = code;
    }

    public void cpu() {
        // Ciclo fetch-decode-execute della VM.
        while ( true ) {
            int bytecode = code[ip++]; // fetch istruzione

            // Variabili temporanee per operazioni aritmetiche e indirizzi.
            int v1,v2;
            int address;

            switch ( bytecode ) {

                case SVMParser.PUSH:
                    // Pusha sullo stack l'operando
                    push( code[ip++] );
                    break;

                case SVMParser.POP:
                    // Rimuove il valore in cima allo stack.
                    pop();
                    break;

                case SVMParser.ADD :
                    // Somma i due valori in cima allo stack e pusha il risultato.
                    v1=pop();
                    v2=pop();
                    push(v2 + v1);
                    break;

                case SVMParser.MULT :
                    // Moltiplica i due valori in cima allo stack.
                    v1=pop();
                    v2=pop();
                    push(v2 * v1);
                    break;

                case SVMParser.DIV :
                    // Divide v2 per v1 (ordine importante nello stack).
                    v1=pop();
                    v2=pop();
                    push(v2 / v1);
                    break;

                case SVMParser.SUB :
                    // Sottrae v1 da v2 (ordine importante nello stack).
                    v1=pop();
                    v2=pop();
                    push(v2 - v1);
                    break;

                case SVMParser.STOREW :
                    // Salva un valore in memoria: pop address, poi pop value.
                    address = pop();
                    memory[address] = pop();
                    break;

                case SVMParser.LOADW :
                    // Carica memory[address] sullo stack.
                    push(memory[pop()]);
                    break;

                case SVMParser.BRANCH :
                    // Salto incondizionato all'indirizzo indicato nel codice.
                    address = code[ip];
                    ip = address;
                    break;

                case SVMParser.BRANCHEQ :
                    // Salta se i due valori in cima allo stack sono uguali.
                    address = code[ip++];
                    v1=pop();
                    v2=pop();
                    if (v2 == v1) ip = address;
                    break;

                case SVMParser.BRANCHLESSEQ :
                    // Salta se v2 <= v1.
                    address = code[ip++];
                    v1=pop();
                    v2=pop();
                    if (v2 <= v1) ip = address;
                    break;

                case SVMParser.JS :
                    // Jump to subroutine: salva ip in ra e salta all'indirizzo sullo stack.
                    address = pop();
                    ra = ip;
                    ip = address;
                    break;

                case SVMParser.STORERA :
                    // Carica in ra il valore in cima allo stack.
                    ra=pop();
                    break;

                case SVMParser.LOADRA :
                    // Pusha sullo stack il valore corrente di ra.
                    push(ra);
                    break;

                case SVMParser.STORETM :
                    // Salva in tm il valore in cima allo stack.
                    tm=pop();
                    break;

                case SVMParser.LOADTM :
                    // Pusha sullo stack il valore salvato in tm.
                    push(tm);
                    break;

                case SVMParser.LOADFP :
                    // Pusha sullo stack il frame pointer corrente.
                    push(fp);
                    break;

                case SVMParser.STOREFP :
                    // Ripristina fp prendendo il valore dallo stack.
                    fp=pop();
                    break;

                case SVMParser.COPYFP :
                    // Imposta fp al valore corrente di sp (inizio nuovo frame).
                    fp=sp;
                    break;

                case SVMParser.STOREHP :
                    // Aggiorna hp prendendo il valore dallo stack.
                    hp=pop();
                    break;

                case SVMParser.LOADHP :
                    // Pusha sullo stack il valore corrente di hp.
                    push(hp);
                    break;

                case SVMParser.PRINT :
                    // Stampa il top dello stack senza rimuoverlo.
                    System.out.println((sp<MEMSIZE)?memory[sp]:"Empty stack!");
                    break;

                case SVMParser.HALT :
                    // Termina l'esecuzione del programma.
                    return;
            }
        }
    }

    private int pop() {
        // Restituisce il top dello stack e incrementa sp.
        return memory[sp++];
    }

    private void push(int v) {
        // Decrementa sp e scrive il valore nello stack.
        memory[--sp] = v;
    }

}