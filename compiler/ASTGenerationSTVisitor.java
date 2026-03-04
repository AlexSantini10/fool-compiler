package compiler;

import java.util.*;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;

import static compiler.lib.FOOLlib.*;

/**
 * Visitor ANTLR che costruisce l'AST a partire dal parse tree.
 *
 * Riduce il parse tree (molto verboso) in un AST più compato e assegna la riga
 * sorgente ai nodi (setLine) per errori più chiari nelle fasi successive.
 */
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

    /**
     * Indentazione usata solo in debug per stampare l'albero di visita.
     */
    String indent;

    /**
     * Flag debug: se true stampa quali produzioni/contesti stiamo visitando.
     */
    public boolean print;

    ASTGenerationSTVisitor() {}

    /**
     * @param debug se true abilita la stampa dei contesti visitati.
     */
    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }

    /**
     * Stampa il nome del contesto ANTLR corrente, con un prefisso che evidenzia
     * quando siamo dentro una produzione "specializzata" (alternative etichettate).
     *
     * Serve solo per debug di grammatica/visitor.
     */
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) {
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        }
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    /**
     * Override generico di visit:
     * - gestisce t == null in modo safe
     * - aggiorna indent per la stampa gerarchica (debug)
     */
    @Override
    public Node visit(ParseTree t) {
        if (t == null) {
            return null;
        }

        // NB: qui passano tutte le chiamate visit(c.qualcosa()).
        // super.visit(t) fa t.accept(this) e il contesto concreto richiama visitXXX(...)
        // in base al tipo dinamico (PlusMinusContext, CallContext, ...).
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    /**
     * Root del parsing: delega al corpo del programma (progbody).
     */
    @Override
    public Node visitProg(ProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Delega al nodo reale del programma: let-in oppure solo exp.
        return visit(c.progbody());
    }

    /**
     * Programma "let-in": costruisce ProgLetInNode.
     */
    @Override
    public Node visitLetInProg(LetInProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione lista dichiarazioni globali del let.
        List<DecNode> declist = new ArrayList<>();

        // Prima le class declarations.
        for (var classDec : c.cldec()) {
            declist.add((DecNode) visit(classDec));
        }

        // Poi dichiarazioni di variabili/funzioni.
        for (DecContext dec : c.dec()) {
            declist.add((DecNode) visit(dec));
        }

        // Espressione finale dopo il let.
        return new ProgLetInNode(declist, visit(c.exp()));
    }

    /**
     * Programma senza dichiarazioni: ProgNode(exp).
     */
    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        return new ProgNode(visit(c.exp()));
    }

    /**
     * Dichiarazione di classe.
     */
    @Override
    public Node visitCldec(CldecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // ID(0): nome classe.
        String classID = c.ID(0).getText();

        // Se presente, ID(1): superclasse.
        String superID = null;
        if (c.EXTENDS() != null) {
            superID = c.ID(1).getText();
        }

        // Creazione lista campi.
        List<FieldNode> fields = new ArrayList<>();

        // NB: se c'è EXTENDS, gli ID dei campi partono dopo anche la superclasse.
        int extendingPad = c.EXTENDS() != null ? 1 : 0;

        // Creazione nodi Field allineando ID dei campi con i type(...) corrispondenti.
        IntStream.range(1 + extendingPad, c.ID().size()).forEach(i -> {
            var field = new FieldNode(
                    c.ID(i).getText(),
                    (TypeNode) visit(c.type(i - (1 + extendingPad)))
            );
            field.setLine(c.ID(i).getSymbol().getLine());
            fields.add(field);
        });

        // Creazione lista metodi.
        List<MethodNode> methods = new ArrayList<>();
        for (var method : c.methdec()) {
            methods.add((MethodNode) visit(method));
        }

        // Nodo classe completo + riga (puntiamo al nome classe).
        var n = new ClassNode(classID, superID, fields, methods);
        n.setLine(c.ID(0).getSymbol().getLine());
        return n;
    }

    /**
     * Dichiarazione di metodo.
     */
    @Override
    public Node visitMethdec(MethdecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // ID(0): nome metodo, type(0): tipo di ritorno.
        String methodId = c.ID(0).getText();
        TypeNode returnType = (TypeNode) visit(c.type(0));

        // Creazione lista parametri: ID(1..n) con type(1..n).
        List<ParNode> parameters = new ArrayList<>();
        IntStream.range(1, c.ID().size()).forEach(i -> {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parameters.add(p);
        });

        // Creazione lista dichiarazioni locali nel corpo del metodo.
        List<DecNode> declarations = new ArrayList<>();
        for (var declaration : c.dec()) {
            declarations.add((DecNode) visit(declaration));
        }

        // Corpo del metodo: exp().
        var n = new MethodNode(methodId, returnType, parameters, declarations, visit(c.exp()));
        n.setLine(c.ID(0).getSymbol().getLine());
        return n;
    }

    /**
     * Creazione oggetto: new ClassName(e1, e2, ...)
     */
    @Override
    public Node visitNew(NewContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione lista argomenti del costruttore.
        List<Node> argumentsList = new ArrayList<>();
        for (int i = 0; i < c.exp().size(); i++) {
            argumentsList.add(visit(c.exp(i)));
        }

        // NB: qui salviamo solo nome classe e argomenti; il binding alla classe (STentry)
        // verrà fatto in analisi semantica.
        var n = new NewNode(c.ID().getText(), argumentsList);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    /**
     * Moltiplicazione o divisione: e1 * e2 oppure e1 / e2
     */
    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Visita dei due operandi; scelta del nodo in base al token presente.
        Node n;
        if (c.TIMES() != null) {
            n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.TIMES().getSymbol().getLine());
        } else {
            n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.DIV().getSymbol().getLine());
        }
        return n;
    }

    /**
     * And/Or booleani: e1 && e2 oppure e1 || e2
     */
    @Override
    public Node visitAndOr(AndOrContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Visita dei due operandi; scelta del nodo in base al token presente.
        Node n;
        if (c.AND() != null) {
            n = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.AND().getSymbol().getLine());
        } else {
            n = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.OR().getSymbol().getLine());
        }
        return n;
    }

    /**
     * Somma o sottrazione: e1 + e2 oppure e1 - e2
     */
    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Visita dei due operandi; scelta del nodo in base al token presente.
        Node n;
        if (c.PLUS() != null) {
            n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.PLUS().getSymbol().getLine());
        } else {
            n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.MINUS().getSymbol().getLine());
        }
        return n;
    }

    /**
     * Confronti: ==, <=, >=
     */
    @Override
    public Node visitComp(CompContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Visita dei due operandi; scelta del nodo in base al token presente.
        Node n = null;
        if (c.EQ() != null) {
            n = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.EQ().getSymbol().getLine());
        } else if (c.LE() != null) {
            n = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.LE().getSymbol().getLine());
        } else if (c.GE() != null) {
            n = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
            n.setLine(c.GE().getSymbol().getLine());
        }
        return n;
    }

    /**
     * Negazione booleana: !exp
     */
    @Override
    public Node visitNot(NotContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        Node n = new NotNode(visit(c.exp()));
        n.setLine(c.NOT().getSymbol().getLine());
        return n;
    }

    /**
     * Dichiarazione di variabile: var ID : type = exp
     */
    @Override
    public Node visitVardec(VardecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione VarNode con tipo e inizializzazione.
        Node n = null;
        if (c.ID() != null) {
            n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
            n.setLine(c.VAR().getSymbol().getLine());
        }
        return n;
    }

    /**
     * Dichiarazione di funzione.
     */
    @Override
    public Node visitFundec(FundecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione lista parametri: ID(1..n) con type(1..n).
        List<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }

        // Creazione lista dichiarazioni locali della funzione.
        List<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) {
            decList.add((DecNode) visit(dec));
        }

        // Creazione FunNode completo: nome, tipo ritorno, parametri, decl locali, corpo.
        Node n = null;
        if (c.ID().size() > 0) {
            n = new FunNode(
                    c.ID(0).getText(),
                    (TypeNode) visit(c.type(0)),
                    parList,
                    decList,
                    visit(c.exp())
            );
            n.setLine(c.FUN().getSymbol().getLine());
        }
        return n;
    }

    /**
     * Tipo int.
     */
    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new IntTypeNode();
    }

    /**
     * Tipo bool.
     */
    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolTypeNode();
    }

    /**
     * Tipo riferimento a classe: ID.
     */
    @Override
    public Node visitIdType(IdTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // NB: qui salviamo solo l'ID. La risoluzione a una dichiarazione di classe
        // avviene dopo (symbol table / type checking).
        var n = new RefTypeNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    /**
     * null: rappresentato come EmptyNode.
     */
    @Override
    public Node visitNull(NullContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new EmptyNode();
    }

    /**
     * Costante intera (supporta anche meno unario).
     */
    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        int value = Integer.parseInt(c.NUM().getText());
        return new IntNode(c.MINUS() == null ? value : -value);
    }

    /**
     * true.
     */
    @Override
    public Node visitTrue(TrueContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(true);
    }

    /**
     * false.
     */
    @Override
    public Node visitFalse(FalseContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(false);
    }

    /**
     * If expression.
     */
    @Override
    public Node visitIf(IfContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // NB: per convenzione grammaticale exp(0)=cond, exp(1)=then, exp(2)=else.
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));

        Node n = new IfNode(ifNode, thenNode, elseNode);
        n.setLine(c.IF().getSymbol().getLine());
        return n;
    }

    /**
     * Print expression: print(exp)
     */
    @Override
    public Node visitPrint(PrintContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        Node n = new PrintNode(visit(c.exp()));
        n.setLine(c.PRINT().getSymbol().getLine());
        return n;
    }

    /**
     * Parentesi: (exp)
     */
    @Override
    public Node visitPars(ParsContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Le parentesi non servono nell'AST, quindi ritorniamo direttamente l'exp interna.
        return visit(c.exp());
    }

    /**
     * Uso di identificatore come espressione: ID
     */
    @Override
    public Node visitId(IdContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // NB: qui creiamo solo IdNode. Entry e nesting level si attaccano dopo.
        Node n = new IdNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    /**
     * Chiamata a funzione: ID(e1, e2, ...)
     */
    @Override
    public Node visitCall(CallContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione lista argomenti della chiamata.
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }

        Node n = new CallNode(c.ID().getText(), arglist);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    /**
     * Chiamata a metodo: obj.meth(e1, e2, ...)
     */
    @Override
    public Node visitDotCall(DotCallContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Creazione lista argomenti della chiamata.
        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }

        // ID(0): oggetto, ID(1): metodo.
        Node n = new ClassCallNode(c.ID(0).getText(), c.ID(1).getText(), arglist);
        n.setLine(c.ID(1).getSymbol().getLine());
        return n;
    }
}