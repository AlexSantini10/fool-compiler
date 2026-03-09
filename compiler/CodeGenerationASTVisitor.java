package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

// Visitor di code generation
// Visita l'AST e produce il codice assembly per la SVM
public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

    // Una dispatch table per ogni classe
    // Ogni dispatch table contiene le label dei metodi nelle posizioni date dagli offset
    private final List<List<String>> dispatchTables = new ArrayList<>();

    CodeGenerationASTVisitor() {}

    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    }

    @Override
    public String visitNode(ProgLetInNode node) {
        if (print) {
            printNode(node);
        }

        // Generazione del codice per tutte le dichiarazioni globali
        String decListCode = null;
        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
        }

        // push 0 inizializza il frame globale
        // getCode() aggiunge in fondo il codice delle funzioni e dei metodi accumulato con putCode(...)
        return nlJoin(
                "push 0",
                decListCode,
                visit(node.exp),
                "halt",
                getCode()
        );
    }

    @Override
    public String visitNode(ProgNode node) {
        if (print) {
            printNode(node);
        }

        // Programma con sola espressione: valuta e termina
        return nlJoin(
                visit(node.exp),
                "halt"
        );
    }

    @Override
    public String visitNode(VarNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Una variabile contribuisce con il codice della sua espressione iniziale
        // Il valore prodotto verrà lasciato nello stack
        return visit(node.exp);
    }

    @Override
    public String visitNode(FunNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Codice delle dichiarazioni locali della funzione
        String decListCode = null;

        // Pop da eseguire in uscita per eliminare le dichiarazioni locali
        String popDecList = null;

        // Pop da eseguire in uscita per eliminare i parametri
        String popParList = null;

        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
            popDecList = nlJoin(popDecList, "pop");
        }

        for (int i = 0; i < node.parList.size(); i++) {
            popParList = nlJoin(popParList, "pop");
        }

        // Ogni funzione riceve una label univoca
        String functionLabel = freshFunLabel();

        // Il codice della funzione viene memorizzato separatamente con putCode(...)
        // Nel punto della dichiarazione la funzione viene rappresentata dalla sua label
        putCode(
                nlJoin(
                        functionLabel + ":",
                        "cfp",          // fp = sp: nuovo frame
                        "lra",          // carica il return address
                        decListCode,    // alloca e inizializza le dichiarazioni locali
                        visit(node.exp),// valuta il corpo
                        "stm",          // salva il valore di ritorno in tm
                        popDecList,     // rimuove le dichiarazioni locali
                        "sra",          // ripristina il return address
                        "pop",          // rimuove l'access link
                        popParList,     // rimuove i parametri
                        "sfp",          // ripristina il frame pointer del chiamante
                        "ltm",          // ricarica il valore di ritorno
                        "lra",          // ricarica il return address
                        "js"            // salto al return address
                )
        );

        // La dichiarazione di funzione lascia sullo stack il puntatore al codice della funzione
        return "push " + functionLabel;
    }

    @Override
    public String visitNode(IdNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Risalita statica della catena degli activation record
        // Serve per raggiungere il frame in cui l'identificatore è stato dichiarato
        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }

        // Da lfp si parte dal frame corrente
        // Si risale di livello con lw sugli access link
        // Poi si somma l'offset dell'identificatore e si legge il valore
        return nlJoin(
                "lfp",
                getActivationRecordCode,
                "push " + node.entry.offset,
                "add",
                "lw"
        );
    }

    @Override
    public String visitNode(CallNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Gli argomenti vengono valutati da destra verso sinistra
        String argumentsCode = null;
        for (int i = node.argList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argList.get(i)));
        }

        // Risalita statica
        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }

        // Caso 1: metodo chiamato implicitamente dentro un metodo
        // Esempio: f() dentro il corpo di un metodo
        if (node.entry.offset >= 0) {
            return nlJoin(
                    "lfp",                  // control link
                    argumentsCode,          // argomenti attuali
                    "lfp",
                    getActivationRecordCode,// object pointer del receiver corrente
                    "stm",                  // salva object pointer in tm
                    "ltm",                  // object pointer come access link del metodo chiamato
                    "ltm",                  // duplica object pointer
                    "lw",                   // dispatch pointer
                    "push " + node.entry.offset,
                    "add",
                    "lw",                   // indirizzo del metodo dalla dispatch table
                    "js"
            );
        }

        // Caso 2: funzione normale
        String commonCode = nlJoin(
                "lfp",                  // control link
                argumentsCode,          // argomenti attuali
                "lfp",
                getActivationRecordCode,// frame lessicale corretto
                "stm",                  // salva access link in tm
                "ltm",                  // rimette access link sullo stack
                "ltm"                   // duplica access link: una copia servirà per l'indirizzo funzione
        );

        return nlJoin(
                commonCode,
                "push " + node.entry.offset,
                "add",
                "lw",
                "js"
        );
    }

    @Override
    public String visitNode(PrintNode node) {
        if (print) {
            printNode(node);
        }

        // Valuta l'espressione e stampa il valore in cima allo stack
        return nlJoin(
                visit(node.exp),
                "print"
        );
    }

    @Override
    public String visitNode(IfNode node) {
        if (print) {
            printNode(node);
        }

        // Due label: una per il then e una per l'uscita
        String label1 = freshLabel();
        String label2 = freshLabel();

        // La convenzione booleana usa 1 per true e 0 per false
        return nlJoin(
                visit(node.cond),
                "push 1",
                "beq " + label1,
                visit(node.el),
                "b " + label2,
                label1 + ":",
                visit(node.th),
                label2 + ":"
        );
    }

    @Override
    public String visitNode(EqualNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();

        // Se i due operandi sono uguali produce 1, altrimenti 0
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "beq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(OrNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();
        String label3 = freshLabel();
        String label4 = freshLabel();

        // Implementazione di OR con short-circuit
        // Se il primo operando è true il secondo non viene valutato
        return nlJoin(
                visit(node.l),
                "push 0",
                "beq " + label1,
                "b " + label2,
                label1 + ":",
                visit(node.r),
                "push 0",
                "beq " + label3,
                label2 + ":",
                "push 1",
                "b " + label4,
                label3 + ":",
                "push 0",
                label4 + ":"
        );
    }

    @Override
    public String visitNode(AndNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();

        // Implementazione di AND con short-circuit
        // Se il primo operando è false il secondo non viene valutato
        return nlJoin(
                visit(node.l),
                "push 0",
                "beq " + label1,
                visit(node.r),
                "push 0",
                "beq " + label1,
                "push 1",
                "b " + label2,
                label1 + ":",
                "push 0",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(NotNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();

        // Inversione del booleano: 0 -> 1, diverso da 0 -> 0
        return nlJoin(
                visit(node.exp),
                "push 0",
                "beq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(LessEqualNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();

        // bleq verifica se il penultimo valore è <= dell'ultimo
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "bleq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(GreaterEqualNode node) {
        if (print) {
            printNode(node);
        }

        String label1 = freshLabel();
        String label2 = freshLabel();

        // Per calcolare l >= r viene usata la differenza l - r
        // e poi si controlla se il risultato è >= 0
        return nlJoin(
                visit(node.r),
                visit(node.l),
                "sub",
                "push 0",
                "bleq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(TimesNode node) {
        if (print) {
            printNode(node);
        }

        // Moltiplicazione dei due operandi
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "mult"
        );
    }

    @Override
    public String visitNode(DivNode node) {
        if (print) {
            printNode(node);
        }

        // Divisione dei due operandi
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "div"
        );
    }

    @Override
    public String visitNode(PlusNode node) {
        if (print) {
            printNode(node);
        }

        // Somma dei due operandi
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "add"
        );
    }

    @Override
    public String visitNode(MinusNode node) {
        if (print) {
            printNode(node);
        }

        // Sottrazione dei due operandi
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "sub"
        );
    }

    @Override
    public String visitNode(ClassNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Dispatch table della classe corrente
        List<String> dispatchTable = new ArrayList<>();
        dispatchTables.add(dispatchTable);

        // Se la classe estende un'altra classe, la dispatch table parte come copia di quella del padre
        if (node.superID != null) {
            List<String> superClassDispatchTable = dispatchTables.get(-node.superEntry.offset - 2);
            dispatchTable.addAll(superClassDispatchTable);
        }

        // Ogni metodo viene compilato e inserito nella dispatch table alla posizione data dall'offset
        // Se il metodo fa overriding, sostituisce la label ereditata
        for (int i = 0; i < node.methods.size(); i++) {
            MethodNode method = node.methods.get(i);
            visit(method);

            if (method.offset < dispatchTable.size()) {
                dispatchTable.set(method.offset, method.label);
            } else {
                while (dispatchTable.size() < method.offset) {
                    dispatchTable.add(null);
                }
                dispatchTable.add(method.label);
            }
        }

        // La dispatch table viene materializzata in heap
        // In ogni cella viene salvata una label di metodo
        String createDispatchTable = null;
        for (String label : dispatchTable) {
            createDispatchTable = nlJoin(
                    createDispatchTable,
                    "push " + label,
                    "lhp",
                    "sw",
                    "lhp",
                    "push 1",
                    "add",
                    "shp"
            );
        }

        // Il valore restituito dalla dichiarazione di classe è l'indirizzo iniziale della dispatch table
        return nlJoin(
                "lhp",
                createDispatchTable
        );
    }

    @Override
    public String visitNode(MethodNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Codice delle dichiarazioni locali del metodo
        String decListCode = null;

        // Pop da eseguire in uscita per eliminare le dichiarazioni locali
        String popDecList = null;

        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
            popDecList = nlJoin(popDecList, "pop");
        }

        // Pop da eseguire in uscita per eliminare i parametri
        String popParList = null;
        for (int i = 0; i < node.parList.size(); i++) {
            popParList = nlJoin(popParList, "pop");
        }

        // Ogni metodo riceve una label univoca
        String functionLabel = freshFunLabel();
        node.label = functionLabel;

        // Il prologo e l'epilogo del metodo seguono la stessa convenzione delle funzioni
        putCode(
                nlJoin(
                        functionLabel + ":",
                        "cfp",
                        "lra",
                        decListCode,
                        visit(node.exp),
                        "stm",
                        popDecList,
                        "sra",
                        "pop",
                        popParList,
                        "sfp",
                        "ltm",
                        "lra",
                        "js"
                )
        );

        // Il codice del metodo viene memorizzato nella dispatch table, non lasciato sullo stack
        return null;
    }

    @Override
    public String visitNode(ClassCallNode node) {
        if (print) {
            printNode(node, node.objId + "." + node.methId);
        }

        // Gli argomenti vengono valutati da destra verso sinistra
        String argumentsCode = null;
        for (int i = node.argList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argList.get(i)));
        }

        // Risalita statica fino al frame che contiene la variabile oggetto
        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }

        // Struttura della method call:
        // - push del control link
        // - push degli argomenti
        // - recupero dell'oggetto
        // - l'oggetto viene usato come access link dinamico del metodo
        // - dal dispatch pointer dell'oggetto si seleziona il metodo tramite offset
        return nlJoin(
                "lfp",
                argumentsCode,
                "lfp",
                getActivationRecordCode,
                "push " + node.entry.offset,
                "add",
                "lw",
                "stm",
                "ltm",
                "ltm",
                "lw",
                "push " + node.methodEntry.offset,
                "add",
                "lw",
                "js"
        );
    }

    @Override
    public String visitNode(NewNode node) {
        if (print) {
            printNode(node, node.id);
        }

        // Valutazione degli argomenti usati per inizializzare i campi dell'oggetto
        String putArgumentsOnStack = null;
        for (Node argument : node.argList) {
            putArgumentsOnStack = nlJoin(putArgumentsOnStack, visit(argument));
        }

        // I valori dei campi vengono copiati in heap uno dopo l'altro
        String loadArgumentsOnHeap = null;
        for (int i = 0; i < node.argList.size(); i++) {
            loadArgumentsOnHeap = nlJoin(
                    loadArgumentsOnHeap,
                    "lhp",
                    "sw",
                    "lhp",
                    "push 1",
                    "add",
                    "shp"
            );
        }

        // Dopo i campi viene salvato il dispatch pointer dell'oggetto
        // La dispatch table della classe è recuperata dalla zona globale in memoria
        // Il valore lasciato sullo stack è l'indirizzo base dell'oggetto
        return nlJoin(
                putArgumentsOnStack,
                loadArgumentsOnHeap,
                "push " + ExecuteVM.MEMSIZE,
                "push " + node.entry.offset,
                "add",
                "lw",
                "lhp",
                "sw",
                "lhp",
                "lhp",
                "push 1",
                "add",
                "shp"
        );
    }

    @Override
    public String visitNode(BoolNode node) {
        if (print) {
            printNode(node, node.val.toString());
        }

        // Convenzione booleana della VM: true = 1, false = 0
        return "push " + (node.val ? 1 : 0);
    }

    @Override
    public String visitNode(IntNode node) {
        if (print) {
            printNode(node, node.val.toString());
        }

        // Una costante intera viene pushata direttamente sullo stack
        return "push " + node.val;
    }

    @Override
    public String visitNode(EmptyNode node) {
        if (print) {
            printNode(node);
        }

        // null viene rappresentato con il valore convenzionale -1
        return "push -1";
    }
}