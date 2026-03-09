package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

/*
FUNZIONAMENTO OFFSET
I parametri hanno offset positivo: +1 (primo), +2 (secondo), +3 (terzo) perché stanno sopra il frame pointer nello stack frame.
Le variaibli locali hanno offset negativo: -2 (prima), -3 (seconda), -4 (terza) perché stanno sotto il frame pointer nello stack frame.
Le variabili globali sono gestite come dichiarazioni nel primo scope (symTable[0]) per cui usano stesso offset delle var locali.
I campi delle classi hanno offset negativo: -1 (primo), -2 (secondo), -3(terzo) [indicano posizione del campo nell'oggetto]
I metodi delle classi hanno offset che parte da 0: 0 (metodo 1), 1 (metodo 2) ... [Sono l'indice nella dispatch table (vtable).]
Se una classe estende un'altra, l'offset resta lo stesso.
*/

public class SymbolTableASTVisitor extends BaseASTVisitor<Void, VoidException> {

    /**
     * Lista di scope, dove ogni scope è una mappa es: scope 0, chiave x -> STentry di x: {type: "int", offset: 0, level: 0})
     * Livelli di scope:
     * 0 = scope globale
     * 1 = dentro funzione o classe
     * 2 = dentro metodo, if ecc.
     */
    private final List<Map<String, STentry>> symTable = new ArrayList<>();

    /**
     * Associa ad ogni classe la sua virtual table (dispatch table).
     * La dispatch table / virtual table (vtable) è una struttura a runtime che contiene solo i metodi, nel compilatore invece vogliamo contenga anche i campi
     * per risolvere obj.x (campo) e obj.somma() (metodo)
     * Es: classe A {int x; int y; int somma(){..}}
     * "A" →
            x      → offset -1
            y      → offset -2
            somma  → offset 0
     */
    private final Map<String, Map<String, STentry>> classTable = new HashMap<>();

    private int nestingLevel = 0; // Livello corrente di scope.
    private int decOffset = -2;

    private int currentFieldOffset;
    private int currentMethodOffset;

    int stErrors = 0;

    /**
     * Ricerca della dichiarazione di id, parte dallo scope più interno e risale verso l'esterno.
     * @param id nome dell'identificatore da risolvere nella symbol table
     * @return restituisce l'entry della prima dichiarazione trovata.
     */
    private STentry stLookup(String id) {
        int j = nestingLevel;
        STentry entry = null;
        while (j >= 0 && entry == null) {
            entry = symTable.get(j--).get(id);
        }
        return entry;
    }

    private void checkTypeExists(TypeNode type, int line) {
        if (!(type instanceof RefTypeNode refType)) {
            return;
        }

        if (symTable.isEmpty()) {
            return;
        }

        STentry entry = symTable.get(0).get(refType.id);
        if (entry == null || !(entry.type instanceof ClassTypeNode)) {
            System.out.println("Class type " + refType.id + " at line " + line + " not declared");
            stErrors++;
        }
    }

    SymbolTableASTVisitor() {}

    SymbolTableASTVisitor(boolean debug) {
        super(debug);
    }

    /**
     * Crea lo scope globale. Poi visita tutte le dichiarazioni globali (dopo let), poi l'espressione finale (dopo in), 
     * e infine rimuove lo scope globale.
     */
    @Override
    public Void visitNode(ProgLetInNode n) {
        if (print) printNode(n);

        Map<String, STentry> globalScope = new HashMap<>();
        symTable.add(globalScope);

        for (Node dec : n.decList) {
            visit(dec);
        }
        visit(n.exp);

        symTable.remove(0);
        return null;
    }

    /**
     * Caso in cui il programma è costituito solo da un'espressione.
     */
    @Override
    public Void visitNode(ProgNode n) {
        if (print) printNode(n);

        Map<String, STentry> globalScope = new HashMap<>();
        symTable.add(globalScope);

        visit(n.exp);

        symTable.remove(0);
        return null;
    }


    /**
     * Gestisce una dichiarazione di funzione.
     */
    @Override
    public Void visitNode(FunNode n) {
        if (print) printNode(n);

        checkTypeExists(n.retType, n.getLine());

        // Recupera lo scope corrente.
        Map<String, STentry> scopeTable = symTable.get(nestingLevel);

        // Prende i tipi dei parametri.
        List<TypeNode> parTypes = new ArrayList<>();
        for (ParNode par : n.parList) {
            checkTypeExists(par.getType(), par.getLine());
            parTypes.add(par.getType());
        }

        // Crea l'entry per la funzione.
        // NOTA: ArrowTypeNode è defninito dal tipo dei parametri e dai parametri di ritorno.
        STentry entry = new STentry(
                nestingLevel,
                new ArrowTypeNode(parTypes, n.retType),
                decOffset--
        );

        // Inserisce la funzione nello scope corrente, se esiste già una funzione con lo stesso nome nello stesso scope, da errore.
        if (scopeTable.put(n.id, entry) != null) {
            System.out.println("Fun id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }

        // Entra nello scope interno della funzione.
        nestingLevel++;
        Map<String, STentry> funScope = new HashMap<>();
        symTable.add(funScope);

        // Salva l'offset esterno e resetta quello locale.
        int prevNLDecOffset = decOffset;
        decOffset = -2;

        // Inserisce i parametri con offset positivi, se il parametro con stesso nome esiste già, da errore.
        int parOffset = 1;
        for (ParNode par : n.parList) {
            if (funScope.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
                System.out.println("Par id " + par.id + " at line " + n.getLine() + " already declared");
                stErrors++;
            }
        }

        // Visita dichiarazioni locali e corpo della funzione.
        for (Node dec : n.decList) {
            visit(dec);
        }
        visit(n.exp);

        // Esce dallo scope e ripristina l'offset precedente.
        symTable.remove(nestingLevel--);
        decOffset = prevNLDecOffset;

        return null;
    }

    /**
     * Gestisce una dichiarazione di variabile.
     */
    @Override
    public Void visitNode(VarNode n) {
        if (print) printNode(n);
        /**
         * L'espressione viene analizzata prima di inserire la variabile nello scope, questo per evitare che la variabile sia visibile dentro la propria inizializzazione.
         * int x = x + 1 Evita che il compilatore veda un'assegnazione di questo tipo come valida.
         */
        checkTypeExists(n.getType(), n.getLine());
        visit(n.exp);

        Map<String, STentry> scopeTable = symTable.get(nestingLevel);
        STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);

        // Solo ora viene inserita la variabile nello scope.
        if (scopeTable.put(n.id, entry) != null) {
            System.out.println("Var id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }
        return null;
    }

    /**
     * Gestione delle classi.
     */
    @Override
    public Void visitNode(ClassNode n) {
        if (print) printNode(n, n.id);

        // Recupero dello scope globale (le classi sono dichiarazioni globali).
        Map<String, STentry> globalST = symTable.get(0);

        // Creazione del tipo della classe.
        ClassTypeNode ct = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());

        // Se la classe estende una superclasse, recupera la sua virtual table, recupera il ClassTypeNode della sua superclasse e copia liste di campi e metodi (che erediterà).
        if (n.superID != null) {
            Map<String, STentry> superVT = classTable.get(n.superID);
            if (superVT == null) {
                System.out.println("Extending class id " + n.superID + " at line "
                        + n.getLine() + " not declared");
                stErrors++;
            } else {
                STentry superEntry = globalST.get(n.superID);
                n.superEntry = superEntry;

                ClassTypeNode superCT = (ClassTypeNode) superEntry.type;
                ct = new ClassTypeNode(
                        new ArrayList<>(superCT.allFields),
                        new ArrayList<>(superCT.allMethods)
                );
            }
        }

        
        STentry classEntry = new STentry(0, ct, decOffset--);
        n.type = ct;

        // L' entry della classe ct viene inserito nello scope globale.
        if (globalST.put(n.id, classEntry) != null) {
            System.out.println("Class id " + n.id + " at line " + n.getLine()
                    + " already declared");
            stErrors++;
        }

        
        nestingLevel++; // Entriamo nello scope della classe.

        // Crea la virtual table della classe: parte da quella della superclasse se presente.
        Map<String, STentry> vt = new HashMap<>();
        if (n.superID != null && classTable.containsKey(n.superID)) {
            vt.putAll(classTable.get(n.superID));
        }
        classTable.put(n.id, vt);
        // La vtable viene temporanemente usata anche come scope corrente.
        symTable.add(vt);

        // Imposta gli offset iniziali per nuovi campi e nuovi metodi.
        if (n.superID != null && n.superEntry != null) {
            currentFieldOffset = -((ClassTypeNode) n.superEntry.type).allFields.size() - 1;
            currentMethodOffset = ((ClassTypeNode) n.superEntry.type).allMethods.size();
        } else {
            currentFieldOffset = -1;
            currentMethodOffset = 0;
        }

        // Serve per intercettare duplicati all'interno della stessa classe.
        Set<String> seenInClass = new HashSet<>();

        // GESTIONE CAMPI:
        for (FieldNode f : n.fields) {
            checkTypeExists(f.getType(), f.getLine());

            if (seenInClass.contains(f.id)) {
                System.out.println("Field or method id " + f.id + " at line "
                        + f.getLine() + " already declared in class " + n.id);
                stErrors++;
                continue;
            }
            seenInClass.add(f.id);

            STentry overridden = vt.get(f.id);
            STentry fe;

            // Se esiste già un campo ereditato con stesso nome, lo override mantenendo offset.
            if (overridden != null && overridden.offset < 0) {
                fe = new STentry(nestingLevel, f.getType(), overridden.offset);
                ct.allFields.set(-fe.offset - 1, fe.type);
            } else {
                // Campo nuovo: assegna nuovo offset negativo.
                fe = new STentry(nestingLevel, f.getType(), currentFieldOffset--);
                ct.allFields.add(-fe.offset - 1, fe.type);
                // Se il nome apparteneva a un metodo, override non consentito.
                if (overridden != null) {
                    System.out.println("Cannot override field id " + f.id + " with a method");
                    stErrors++;
                }
            }

            vt.put(f.id, fe);
            f.offset = fe.offset;
        }

        // GESTIONE METODI:
        int prevNLDecOffset = decOffset;
        int prevClassMethodOffset = currentMethodOffset;
        decOffset = currentMethodOffset;

        for (MethodNode m : n.methods) {
            // Controlla duplicati nella stessa classe.
            if (seenInClass.contains(m.id)) {
                System.out.println("Field or method id " + m.id + " at line "
                        + m.getLine() + " already declared in class " + n.id);
                stErrors++;
                continue;
            }
            seenInClass.add(m.id);

            // La logica di inserimento del metodo è delegata a visitNode(MethodNode).
            visit(m);

            STentry me = vt.get(m.id);
            ArrowTypeNode funType = (ArrowTypeNode) me.type;

            if (m.offset < ct.allMethods.size()) { // esiste già un entry in quella posizione
                ct.allMethods.set(m.offset, funType); // vuol dire che si sta overrideando un metodo eridatato => si sostituisce nella stessa posizione
            } else { // il metodo è nuovo
                while (ct.allMethods.size() < m.offset) {
                    ct.allMethods.add(null);
                }
                ct.allMethods.add(funType); // aggiunge il tipo del nuovo metodo.
            }
        }

        // Esce dallo scope e ripristina l'offset precedente.
        decOffset = prevNLDecOffset;
        currentMethodOffset = prevClassMethodOffset;
        symTable.remove(nestingLevel--);

        return null;
    }

    @Override
    public Void visitNode(FieldNode n) {
        if (print) printNode(n, n.id);
        // I campi non vengono visitati autonomamente: sono già gestiti dentro visitNode(ClassNode).
        return null;
    }

    /**
     * Gestisce la dichiarazione di un metodo dentro una classe.
     */
    @Override
    public Void visitNode(MethodNode n) {
        if (print) printNode(n, n.id);

        checkTypeExists(n.retType, n.getLine());

        // Recupera la virtual table della classe corrente.
        Map<String, STentry> vt = symTable.get(nestingLevel);

        List<TypeNode> parTypes = new ArrayList<>();
        for (ParNode p : n.parList) {
            checkTypeExists(p.getType(), p.getLine());
            parTypes.add(p.getType());
        }
        // Costruisce il tipo funzionale del metodo.
        ArrowTypeNode mType = new ArrowTypeNode(parTypes, n.retType);

        STentry overridden = vt.get(n.id);
        STentry me;

        // Se esiste già un metodo ereditato con lo stesso nome, fa l' override mantenendo lo stesso offset nella dispatch table.
        if (overridden != null && overridden.offset >= 0) {
            me = new STentry(nestingLevel, mType, overridden.offset);
        } else {
            // Altrimenti, se il metodo è nuovo usa il prossimo offset disponibile.
            me = new STentry(nestingLevel, mType, decOffset++);
            // Se il nome era di un campo, override non consentito.
            if (overridden != null) {
                System.out.println("Cannot override method id " + n.id + " with a field");
                stErrors++;
            }
        }

        vt.put(n.id, me);
        n.offset = me.offset;

        // Entra nello scope locale del metodo.
        nestingLevel++;
        Map<String, STentry> methodScope = new HashMap<>();
        symTable.add(methodScope);

        int prevNLDecOffset = decOffset;
        decOffset = -2;

        // Inserisce i parametri del metodo con offset positivi.
        int parOffset = 1;
        for (ParNode p : n.parList) {
            if (methodScope.put(p.id, new STentry(nestingLevel, p.getType(), parOffset++)) != null) {
                System.out.println("Par id " + p.id + " at line " + n.getLine() + " already declared");
                stErrors++;
            }
        }

        // Visita dichiarazioni locali e corpo del metodo.
        for (DecNode d : n.decList) {
            visit(d);
        }
        visit(n.exp);

        // Esce dallo scope locale del metodo.
        symTable.remove(nestingLevel--);
        decOffset = prevNLDecOffset;

        return null;
    }

    /**
     * Gestisce la creazione di un oggetto. (new Classe(...))
     */
    @Override
    public Void visitNode(NewNode n) {
        if (print) printNode(n, n.id);

        // Cerca la classe nello scope globale.
        STentry classEntry = symTable.get(0).get(n.id);
        if (classEntry == null) {
            System.out.println("Class id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = classEntry; // Se esiste collega il nodo alla dichiarazione.
        }

        // Visita gli argomenti.
        // Anche se la classe non esiste, gli argomenti vengono comunque visitati. In modo da trovare altri errori semantici se ci sono.
        for (Node arg : n.argList) {
            visit(arg);
        }
        return null;
    }

    /**
     * RefTypeNode rappresenta un tipo riferimento a classe, cioè un tipo del genere: A, dove A nome di una classe.
     * Verifica che il nome di classe usato come tipo esista davvero.
     */
    @Override
    public Void visitNode(RefTypeNode n) {
        if (print) printNode(n, n.id);
        checkTypeExists(n, n.getLine());
        return null;
    }

    /**
     * Rappresenta e gestisce una chiamata di metodo su oggetto, Es: obj.metodo(arg1, arg2, ...)
     */
    @Override
    public Void visitNode(ClassCallNode n) {
        if (print) printNode(n, n.objId + "." + n.methId);

        // Cerca l'oggetto (objiDd = nome) 
        STentry objEntry = stLookup(n.objId); // Qui usa stLookup invece di symTable.get(0) perché l'oggetto può essere un parametro, una variabile globale o locale.
        if (objEntry == null) {
            System.out.println("Object id " + n.objId + " at line " + n.getLine() + " not declared");
            stErrors++;
            // Anche in caso di errore per oggetto non dichiarato, continua a visitare gli argomenti in modo da raccogliere altri eventuali errori.
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        // Verifica sia un oggetto di classe. Potrebbe essere un int o una funzione.
        if (!(objEntry.type instanceof RefTypeNode)) {
            System.out.println("Object id " + n.objId + " at line " + n.getLine() + " is not a class reference");
            stErrors++;
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        //Salva la dichiarazione dell'oggetto e il livello di annidamento in cui avviene la chiamata.
        n.entry = objEntry;
        n.nl = nestingLevel;

        // Recupera la classe concreta dell'oggetto.
        String classId = ((RefTypeNode) objEntry.type).id;
        Map<String, STentry> vtable = classTable.get(classId);
        // Se la classe non esiste, errore. (Controllo di sicurezza)
        if (vtable == null) {
            System.out.println("Class " + classId + " for object " + n.objId + " not declared");
            stErrors++;
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        // Cerca il metodo nella vtable e siccome la vtable contiene sia campi sia metodi, non basta verificare che il nome esista:
        // bisogna verificare anche sia un metodo.
        STentry methodEntry = vtable.get(n.methId);
        if (methodEntry == null
                || methodEntry.offset < 0 // I metodi hanno offset positivi, i campi negativi
                || !(methodEntry.type instanceof ArrowTypeNode)) {
            System.out.println("Method id " + n.methId + " at line " + n.getLine()
                    + " not declared in class " + classId);
            stErrors++;
        } else {
            n.methodEntry = methodEntry; // Collega il nodo al metodo se tutto ok.
        }

        // Infine, visita tutti gli argoementi.
        for (Node arg : n.argList) {
            visit(arg);
        }
        return null;
    }

    /**
     * Rappresenta e gestisce una chiamata a funzione normale. Es: f(arg1, arg2, ...)
     * Ha solo nome funzione e lista di argomenti.
     */
    @Override
    public Void visitNode(CallNode n) {
        if (print) printNode(n);

        STentry entry = stLookup(n.id);
        if (entry == null) {
            System.out.println("Fun id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = entry;
            n.nl = nestingLevel;
        }

        for (Node arg : n.argList) {
            visit(arg);
        }
        return null;
    }

    /**
     * IdNode è l’uso di un identificatore semplice come espressione, per esempio: x
     * Può essere una variabile o un parametro.
     */
    @Override
    public Void visitNode(IdNode n) {
        if (print) printNode(n);

        STentry entry = stLookup(n.id);
        if (entry == null) {
            System.out.println("Var or Par id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = entry;
            n.nl = nestingLevel;
        }
        return null;
    }

    /**
     * I nodi seguenti non introducono scope né dichiarazioni:
     * visitano semplicemente ricosivamente i sottoalberi.
     */

    @Override
    public Void visitNode(PrintNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        if (print) printNode(n);
        visit(n.cond);
        visit(n.th);
        visit(n.el);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(TimesNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        if (print) printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public Void visitNode(EmptyTypeNode n) {
        if (print) printNode(n);
        return null;
    }
}