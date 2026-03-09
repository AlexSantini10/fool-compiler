package compiler;

import java.util.*;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;

import static compiler.lib.FOOLlib.*;

// Visitor ANTLR che trasforma il parse tree in AST.
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

    // Indentazione usata solo per debug di visita
    String indent;

    public boolean print;

    ASTGenerationSTVisitor() {}

    ASTGenerationSTVisitor(boolean debug) {
        print = debug;
    }


    // Stampa il nome del contesto corrente.
    // Se il contesto è un'alternativa etichettata, stampa anche la produzione padre.
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix = "";
        Class<?> ctxClass = ctx.getClass(), parentClass = ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) {
            prefix = lowerizeFirstChar(extractCtxName(parentClass.getName())) + ": production #";
        }
        System.out.println(indent + prefix + lowerizeFirstChar(extractCtxName(ctxClass.getName())));
    }

    /**
     * Override generico di visit: aggiorna indent per la stampa gerarchica (debug)
     */
    @Override
    public Node visit(ParseTree t) {
        if (t == null) {
            return null;
        }

        // Qui gestiamo solo il debug gerarchico, non la logica di parsing.
        String temp = indent;
        indent = (indent == null) ? "" : indent + "  ";
        Node result = super.visit(t);
        indent = temp;
        return result;
    }

    // Root del parsing: il vero programma sta dentro progbody
    @Override
    public Node visitProg(ProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        return visit(c.progbody());
    }

    // Programma della forma let dec* in exp
    @Override
    public Node visitLetInProg(LetInProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // L'ordine delle dichiarazioni viene mantenuto.
        // Prima classi, poi dichiarazioni normali.
        List<DecNode> declist = new ArrayList<>();

        for (var classDec : c.cldec()) {
            declist.add((DecNode) visit(classDec));
        }

        for (DecContext dec : c.dec()) {
            declist.add((DecNode) visit(dec));
        }

        return new ProgLetInNode(declist, visit(c.exp()));
    }

    // Programma composto da una sola espressione
    @Override
    public Node visitNoDecProg(NoDecProgContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        return new ProgNode(visit(c.exp()));
    }

    // Dichiarazione di classe
    @Override
    public Node visitCldec(CldecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // ID(0) è sempre il nome della classe.
        String classID = c.ID(0).getText();

        // Se c'è extends, ID(1) è la superclasse.
        String superID = null;
        if (c.EXTENDS() != null) {
            superID = c.ID(1).getText();
        }

        List<FieldNode> fields = new ArrayList<>();

        // Se c'è extends, la lista degli ID è:
        // ID(0)=classe, ID(1)=superclasse, poi partono i campi.
        // Altrimenti i campi partono subito da ID(1).
        int extendingPad = c.EXTENDS() != null ? 1 : 0;

        // Allineamento delicato:
        // gli ID dei campi partono dopo nome classe ed eventuale superclasse,
        // mentre i type(...) contengono solo i tipi dei campi.
        IntStream.range(1 + extendingPad, c.ID().size()).forEach(i -> {
            var field = new FieldNode(
                    c.ID(i).getText(),
                    (TypeNode) visit(c.type(i - (1 + extendingPad)))
            );
            field.setLine(c.ID(i).getSymbol().getLine());
            fields.add(field);
        });

        List<MethodNode> methods = new ArrayList<>();
        for (var method : c.methdec()) {
            methods.add((MethodNode) visit(method));
        }

        var n = new ClassNode(classID, superID, fields, methods);
        n.setLine(c.ID(0).getSymbol().getLine());
        return n;
    }

    // Dichiarazione di metodo
    @Override
    public Node visitMethdec(MethdecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Convenzione della grammatica:
        // ID(0) è il nome metodo, type(0) è il tipo di ritorno.
        String methodId = c.ID(0).getText();
        TypeNode returnType = (TypeNode) visit(c.type(0));

        // Da ID(1) e type(1) in poi abbiamo i parametri.
        List<ParNode> parameters = new ArrayList<>();
        IntStream.range(1, c.ID().size()).forEach(i -> {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parameters.add(p);
        });

        List<DecNode> declarations = new ArrayList<>();
        for (var declaration : c.dec()) {
            declarations.add((DecNode) visit(declaration));
        }

        var n = new MethodNode(methodId, returnType, parameters, declarations, visit(c.exp()));
        n.setLine(c.ID(0).getSymbol().getLine());
        return n;
    }

    // Creazione oggetto: new ClassName(e1, e2, ...)
    @Override
    public Node visitNew(NewContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        List<Node> argumentsList = new ArrayList<>();
        for (int i = 0; i < c.exp().size(); i++) {
            argumentsList.add(visit(c.exp(i)));
        }

        // Qui salviamo solo nome classe e argomenti.
        // Il collegamento alla dichiarazione della classe avviene dopo.
        var n = new NewNode(c.ID().getText(), argumentsList);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    // e1 * e2 oppure e1 / e2
    @Override
    public Node visitTimesDiv(TimesDivContext c) {
        if (print) {
            printVarAndProdName(c);
        }

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

    // e1 && e2 oppure e1 || e2
    @Override
    public Node visitAndOr(AndOrContext c) {
        if (print) {
            printVarAndProdName(c);
        }

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

    // e1 + e2 oppure e1 - e2
    @Override
    public Node visitPlusMinus(PlusMinusContext c) {
        if (print) {
            printVarAndProdName(c);
        }

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

    // e1 == e2, e1 <= e2, e1 >= e2
    @Override
    public Node visitComp(CompContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // La grammatica garantisce che uno di questi token ci sia.
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

    // !exp
    @Override
    public Node visitNot(NotContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        Node n = new NotNode(visit(c.exp()));
        n.setLine(c.NOT().getSymbol().getLine());
        return n;
    }

    // var ID : type = exp
    @Override
    public Node visitVardec(VardecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        Node n = null;
        if (c.ID() != null) {
            n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
            n.setLine(c.VAR().getSymbol().getLine());
        }
        return n;
    }

    // Dichiarazione di funzione
    @Override
    public Node visitFundec(FundecContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Convenzione della grammatica:
        // ID(0) nome funzione, type(0) tipo di ritorno,
        // da 1 in poi parametri e tipi dei parametri.
        List<ParNode> parList = new ArrayList<>();
        for (int i = 1; i < c.ID().size(); i++) {
            ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
            p.setLine(c.ID(i).getSymbol().getLine());
            parList.add(p);
        }

        List<DecNode> decList = new ArrayList<>();
        for (DecContext dec : c.dec()) {
            decList.add((DecNode) visit(dec));
        }

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

    // int
    @Override
    public Node visitIntType(IntTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new IntTypeNode();
    }

    // bool
    @Override
    public Node visitBoolType(BoolTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolTypeNode();
    }

    // Tipo riferimento a classe: un ID che qui non viene ancora risolto
    @Override
    public Node visitIdType(IdTypeContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // In AST salviamo solo il nome della classe.
        // La verifica che esista davvero avviene in analisi semantica.
        var n = new RefTypeNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    // null
    @Override
    public Node visitNull(NullContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new EmptyNode();
    }

    // Costante intera, con supporto al meno unario
    @Override
    public Node visitInteger(IntegerContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Il meno unario viene già assorbito qui nel valore.
        int value = Integer.parseInt(c.NUM().getText());
        return new IntNode(c.MINUS() == null ? value : -value);
    }

    // true
    @Override
    public Node visitTrue(TrueContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(true);
    }

    // false
    @Override
    public Node visitFalse(FalseContext c) {
        if (print) {
            printVarAndProdName(c);
        }
        return new BoolNode(false);
    }

    // if exp then exp else exp
    @Override
    public Node visitIf(IfContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Posizioni fissate dalla grammatica:
        // exp(0) condizione, exp(1) then, exp(2) else.
        Node ifNode = visit(c.exp(0));
        Node thenNode = visit(c.exp(1));
        Node elseNode = visit(c.exp(2));

        Node n = new IfNode(ifNode, thenNode, elseNode);
        n.setLine(c.IF().getSymbol().getLine());
        return n;
    }

    // print(exp)
    @Override
    public Node visitPrint(PrintContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        Node n = new PrintNode(visit(c.exp()));
        n.setLine(c.PRINT().getSymbol().getLine());
        return n;
    }

    // (exp)
    @Override
    public Node visitPars(ParsContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Le parentesi servono solo nel parse tree.
        // Nell'AST non aggiungono informazione strutturale.
        return visit(c.exp());
    }

    // Uso di identificatore come espressione
    @Override
    public Node visitId(IdContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        // Qui creiamo solo il nodo sintattico.
        // Entry e nesting level si attaccano dopo nella symbol table visit.
        Node n = new IdNode(c.ID().getText());
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    // Chiamata a funzione: ID(e1, e2, ...)
    @Override
    public Node visitCall(CallContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }

        Node n = new CallNode(c.ID().getText(), arglist);
        n.setLine(c.ID().getSymbol().getLine());
        return n;
    }

    // Chiamata a metodo: obj.meth(e1, e2, ...)
    @Override
    public Node visitDotCall(DotCallContext c) {
        if (print) {
            printVarAndProdName(c);
        }

        List<Node> arglist = new ArrayList<>();
        for (ExpContext arg : c.exp()) {
            arglist.add(visit(arg));
        }

        // ID(0) è il ricevente, ID(1) è il metodo.
        // La risoluzione del metodo nella gerarchia di classi avviene dopo.
        Node n = new ClassCallNode(c.ID(0).getText(), c.ID(1).getText(), arglist);
        n.setLine(c.ID(1).getSymbol().getLine());
        return n;
    }
}