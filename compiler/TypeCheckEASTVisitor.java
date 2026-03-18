package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import static compiler.TypeRels.*;

public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode, TypeException> {

    TypeCheckEASTVisitor() {
        super(true);
    }

    TypeCheckEASTVisitor(boolean debug) {
        super(true, debug);
    }

    private TypeNode ckvisit(TypeNode t) throws TypeException {
        visit(t);
        return t;
    }

    /**
     * Gestisce il controllo dei tipi per i programmi con dichiarazioni (LET-IN).
     * Analizza la lista delle dichiarazioni e restituisce il tipo dell'espressione finale nel corpo dell'IN.
     */
    @Override
    public TypeNode visitNode(ProgLetInNode n) throws TypeException {
        if (print) printNode(n);
        for (Node dec : n.decList) {
            try {
                visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        }
        return visit(n.exp);
    }

    /**
     * Gestisce il controllo dei tipi per un programma composto da una singola espressione.
     */
    @Override
    public TypeNode visitNode(ProgNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    /**
     * Controlla la validità dei tipi all'interno di una funzione.
     * Verifica le dichiarazioni locali e accerta che il tipo dell'espressione di ritorno
     * sia compatibile con il tipo di ritorno dichiarato (retType).
     */
    @Override
    public TypeNode visitNode(FunNode n) throws TypeException {
        if (print) printNode(n, n.id);
        for (Node dec : n.decList) {
            try {
                visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        }
        if (!isSubtype(visit(n.exp), ckvisit(n.retType))) {
            throw new TypeException("Wrong return type for function " + n.id, n.getLine());
        }
        return null;
    }

    /**
     * Controlla la coerenza tra il tipo dichiarato di una variabile e il tipo
     * dell'espressione assegnata ad essa (verifica di assegnamento).
     */
    @Override
    public TypeNode visitNode(VarNode n) throws TypeException {
        if (print) printNode(n, n.id);
        if (!isSubtype(visit(n.exp), ckvisit(n.getType()))) {
            throw new TypeException("Incompatible value for variable " + n.id, n.getLine());
        }
        return null;
    }

    /**
     * Restituisce il tipo dell'espressione che deve essere stampata a video.
     */
    @Override
    public TypeNode visitNode(PrintNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

    /**
     * Gestisce il controllo dei tipi dell'istruzione IF.
     * Verifica che la condizione sia booleana e calcola il "Lowest Common Ancestor" (LCA)
     * tra i tipi del ramo 'then' e del ramo 'else' per determinare il tipo risultante.
     */
    @Override
    public TypeNode visitNode(IfNode n) throws TypeException {
        if (print) printNode(n);
        if (!isSubtype(visit(n.cond), new BoolTypeNode())) {
            throw new TypeException("Non boolean condition in if", n.getLine());
        }
        TypeNode t = visit(n.th);
        TypeNode e = visit(n.el);
        TypeNode lca = lowestCommonAncestor(t, e);
        if (lca == null) {
            throw new TypeException("Incompatible types in then-else branches", n.getLine());
        }
        return lca;
    }

    /**
     * Verifica che i due operandi di un'uguaglianza siano confrontabili tra loro
     * (uno deve essere sottotipo dell'altro) e restituisce sempre un tipo Booleano.
     */
    @Override
    public TypeNode visitNode(EqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, r) || isSubtype(r, l))) {
            throw new TypeException("Incompatible types in equal", n.getLine());
        }
        return new BoolTypeNode();
    }

    /**
     * Verifica la confrontabilità (>=) tra tipi compatibili e restituisce un Booleano.
     */
    @Override
    public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l,new IntTypeNode()) || isSubtype(r, new IntTypeNode()))) {
            throw new TypeException("Incompatible types in greater equal", n.getLine());
        }
        return new BoolTypeNode();
    }

    /**
     * Verifica la confrontabilità (<=) tra tipi compatibili e restituisce un Booleano.
     */
    @Override
    public TypeNode visitNode(LessEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, new IntTypeNode()) || isSubtype(r, new IntTypeNode()))) {
            throw new TypeException("Incompatible types in less equal", n.getLine());
        }
        return new BoolTypeNode();
    }

    /**
     * Verifica che gli operandi dell'operatore logico AND siano booleani.
     */
    @Override
    public TypeNode visitNode(AndNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, new BoolTypeNode()) || isSubtype(r, new BoolTypeNode()))) {
            throw new TypeException("Incompatible types in AND", n.getLine());
        }
        return new BoolTypeNode();
    }

    /**
     * Verifica che gli operandi dell'operatore logico OR siano booleani.
     */
    @Override
    public TypeNode visitNode(OrNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, new BoolTypeNode()) || isSubtype(r, new BoolTypeNode()))) {
            throw new TypeException("Incompatible types in OR", n.getLine());
        }
        return new BoolTypeNode();
    }

    /**
     * Verifica che l'operando della negazione logica NOT sia booleano.
     */
    @Override
    public TypeNode visitNode(NotNode n) throws TypeException {
        if (print) printNode(n);
        if (!isSubtype(visit(n.exp), new BoolTypeNode())) {
            throw new TypeException("Non boolean operand in not", n.getLine());
        }
        return new BoolTypeNode();
    }


    /**
     * Verifica che entrambi gli operandi di una moltiplicazione siano interi.
     */
    @Override
    public TypeNode visitNode(TimesNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in multiplication", n.getLine());
        }
        return new IntTypeNode();
    }

    /**
     * Verifica che entrambi gli operandi di una divisione siano interi.
     */
    @Override
    public TypeNode visitNode(DivNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in division", n.getLine());
        }
        return new IntTypeNode();
    }

    /**
     * Verifica che entrambi gli operandi di una somma siano interi.
     */
    @Override
    public TypeNode visitNode(PlusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in sum", n.getLine());
        }
        return new IntTypeNode();
    }

    /**
     * Verifica che entrambi gli operandi di una sottrazione siano interi.
     */
    @Override
    public TypeNode visitNode(MinusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in subtraction", n.getLine());
        }
        return new IntTypeNode();
    }

    /**
     * Controlla la validità di una chiamata di funzione.
     * Verifica che l'identificatore sia effettivamente una funzione, che il numero di argomenti
     * sia corretto e che ogni argomento sia compatibile con il tipo del parametro formale.
     */
    @Override
    public TypeNode visitNode(CallNode n) throws TypeException {
        if (print) printNode(n, n.id);

        if (n.entry == null) {
            throw new IncomplException();
        }

        TypeNode t = visit(n.entry);
        if (!(t instanceof ArrowTypeNode)) {
            throw new TypeException("Invocation of a non-function " + n.id, n.getLine());
        }

        ArrowTypeNode at = (ArrowTypeNode) t;

        if (at.parList.size() != n.argList.size()) {
            throw new TypeException("Wrong number of parameters in the invocation of " + n.id, n.getLine());
        }

        for (int i = 0; i < n.argList.size(); i++) {
            if (!isSubtype(visit(n.argList.get(i)), at.parList.get(i))) {
                throw new TypeException(
                        "Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.id,
                        n.getLine()
                );
            }
        }

        return at.retType;
    }

    /**
     * Controlla la validità di una chiamata a un metodo di classe.
     * Simile a CallNode, ma opera nel contesto dell'invocazione di metodi su oggetti.
     */
    @Override
    public TypeNode visitNode(ClassCallNode n) throws TypeException {
        if (print) printNode(n, n.objId + "." + n.methId);

        if (n.entry == null || n.methodEntry == null) {
            throw new IncomplException();
        }

        TypeNode mt = visit(n.methodEntry);
        if (!(mt instanceof ArrowTypeNode)) {
            throw new TypeException("Invocation of a non-method " + n.methId, n.getLine());
        }

        ArrowTypeNode at = (ArrowTypeNode) mt;

        if (at.parList.size() != n.argList.size()) {
            throw new TypeException("Wrong number of parameters in the invocation of " + n.methId, n.getLine());
        }

        for (int i = 0; i < n.argList.size(); i++) {
            if (!isSubtype(visit(n.argList.get(i)), at.parList.get(i))) {
                throw new TypeException(
                        "Wrong type for " + (i + 1) + "-th parameter in the invocation of " + n.methId,
                        n.getLine()
                );
            }
        }

        return at.retType;
    }

    /**
     * Recupera il tipo associato a un identificatore.
     * Impedisce l'uso improprio di nomi di funzioni o classi come se fossero variabili semplici.
     */
    @Override
    public TypeNode visitNode(IdNode n) throws TypeException {
        if (print) printNode(n, n.id);

        if (n.entry == null) {
            throw new IncomplException();
        }

        TypeNode t = visit(n.entry);

        if (t instanceof ArrowTypeNode) {
            throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
        }
        if (t instanceof ClassTypeNode) {
            throw new TypeException("Wrong usage of class identifier " + n.id, n.getLine());
        }
        return t;
    }

    /**
     * Gestisce l'istanziamento di una nuova classe (NEW).
     * Verifica che l'ID corrisponda a una classe, che il numero di argomenti passati al
     * costruttore coincida con i campi della classe e che i tipi siano compatibili.
     */
    @Override
    public TypeNode visitNode(NewNode n) throws TypeException {
        if (print) printNode(n, n.id);

        if (n.entry == null) {
            throw new IncomplException();
        }

        if (!(n.entry.type instanceof ClassTypeNode)) {
            throw new TypeException("Invocation of new on a non-class " + n.id, n.getLine());
        }

        ClassTypeNode ct = (ClassTypeNode) n.entry.type;

        if (ct.allFields.size() != n.argList.size()) {
            throw new TypeException("Wrong number of fields in new " + n.id, n.getLine());
        }

        for (int i = 0; i < n.argList.size(); i++) {
            if (!isSubtype(visit(n.argList.get(i)), ct.allFields.get(i))) {
                throw new TypeException(
                        "Wrong type for " + (i + 1) + "-th field in new " + n.id,
                        n.getLine()
                );
            }
        }

        return new RefTypeNode(n.id);
    }

    /**
     * Esegue il controllo dei tipi per una dichiarazione di Classe.
     * In caso di ereditarietà, controlla il "Subtyping di tipo Safe": verifica che i campi
     * e i metodi sovrascritti (overriding) siano compatibili con quelli della superclasse.
     */
    @Override
    public TypeNode visitNode(ClassNode n) throws TypeException {
        if (print) printNode(n, n.id);

        for (MethodNode m : n.methods) {
            visit(m);
        }

        if (n.superID != null) {
            if (n.superEntry == null) {
                throw new IncomplException();
            }

            superType.put(n.id, n.superID);

            ClassTypeNode classType = n.type;
            ClassTypeNode parentType = (ClassTypeNode) n.superEntry.type;

            for (FieldNode f : n.fields) {
                int pos = -f.offset - 1;
                if (pos < parentType.allFields.size()
                        && !isSubtype(classType.allFields.get(pos), parentType.allFields.get(pos))) {
                    throw new TypeException("Wrong type for field " + f.id, f.getLine());
                }
            }

            for (MethodNode m : n.methods) {
                int pos = m.offset;
                if (pos < parentType.allMethods.size()
                        && !isSubtype(classType.allMethods.get(pos), parentType.allMethods.get(pos))) {
                    throw new TypeException("Wrong type for method " + m.id, m.getLine());
                }
            }
        }

        return null;
    }

    /**
     * Controlla il corpo di un metodo di una classe.
     * Simile a FunNode, verifica la compatibilità tra l'espressione restituita e il tipo dichiarato.
     */
    @Override
    public TypeNode visitNode(MethodNode n) throws TypeException {
        if (print) printNode(n, n.id);

        for (Node dec : n.decList) {
            try {
                visit(dec);
            } catch (IncomplException e) {
            } catch (TypeException e) {
                System.out.println("Type checking error in a declaration: " + e.text);
            }
        }

        if (!isSubtype(visit(n.exp), ckvisit(n.retType))) {
            throw new TypeException("Wrong return type for method " + n.id, n.getLine());
        }

        return null;
    }

    /**
     * Ritorna il tipo base Booleano per un nodo foglia costante booleana.
     */
    @Override
    public TypeNode visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return new BoolTypeNode();
    }

    /**
     * Ritorna il tipo base Intero per un nodo foglia costante intera.
     */
    @Override
    public TypeNode visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return new IntTypeNode();
    }

    /**
     * Ritorna il tipo Empty (null/void) per un nodo vuoto.
     */
    @Override
    public TypeNode visitNode(EmptyNode n) {
        if (print) printNode(n);
        return new EmptyTypeNode();
    }

    /**
     * Visita la definizione di un tipo funzione (arrow type), analizzando i parametri e il ritorno.
     */
    @Override
    public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (TypeNode par : n.parList) {
            visit(par);
        }
        visit(n.retType, "->");
        return null;
    }

    /**
     * Metodi di supporto che restituiscono il tipo atomico o i riferimenti a classi.
     */
    @Override
    public TypeNode visitNode(BoolTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(IntTypeNode n) {
        if (print) printNode(n);
        return null;
    }

    @Override
    public TypeNode visitNode(RefTypeNode n) throws TypeException {
        if (print) printNode(n, n.id);
        return n;
    }

    /**
     * Analizza la struttura di un tipo classe (campi e metodi).
     */
    @Override
    public TypeNode visitNode(ClassTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (TypeNode f : n.allFields) ckvisit(f);
        for (ArrowTypeNode m : n.allMethods) ckvisit(m);
        return null;
    }

    /**
     * Ottiene il tipo presente nella Symbol Table associato alla entry
     */
    @Override
    public TypeNode visitSTentry(STentry entry) throws TypeException {
        if (print) printSTentry("type");
        return ckvisit(entry.type);
    }

    @Override
    public TypeNode visitNode(EmptyTypeNode n) throws TypeException {
        if (print) printNode(n);
        return n;
    }
}