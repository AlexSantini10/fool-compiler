package compiler;

import java.util.*;
import java.util.stream.Collectors;

import compiler.lib.*;

/**
 * AST del linguaggio FOOL.
 *
 * Rappresenta la struttura sintattica astratta del programma.
 * Tutte le fasi del compilatore (symbol table, type checking,
 * code generation) lavorano visitando questi nodi.
 *
 * Le liste sono rese immutabili per evitare modifiche
 * accidentali alla struttura dell'albero.
 */
public class AST {

    /**
     * Programma con dichiarazioni globali.
     * Forma: let decList in exp
     */
    public static class ProgLetInNode extends Node {

        // Dichiarazioni globali visibili nell'espressione finale
        final List<DecNode> decList;

        // Espressione principale del programma
        final Node exp;

        ProgLetInNode(List<DecNode> decs, Node exp) {
            this.decList = Collections.unmodifiableList(decs);
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Programma composto da una sola espressione.
     */
    public static class ProgNode extends Node {

        final Node exp;

        ProgNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Dichiarazione di funzione.
     */
    public static class FunNode extends DecNode {

        // Nome funzione
        final String id;

        // Tipo di ritorno
        final TypeNode retType;

        // Parametri formali
        final List<ParNode> parList;

        // Dichiarazioni locali nel corpo
        final List<DecNode> decList;

        // Corpo della funzione
        final Node exp;

        FunNode(String id, TypeNode retType, List<ParNode> pars, List<DecNode> decs, Node exp) {
            this.id = id;
            this.retType = retType;
            this.parList = Collections.unmodifiableList(pars);
            this.decList = Collections.unmodifiableList(decs);
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Parametro formale di funzione o metodo.
     */
    public static class ParNode extends DecNode {

        final String id;

        ParNode(String id, TypeNode type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Dichiarazione di variabile.
     */
    public static class VarNode extends DecNode {

        final String id;

        // Espressione di inizializzazione
        final Node exp;

        VarNode(String id, TypeNode type, Node exp) {
            this.id = id;
            this.type = type;
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Dichiarazione di classe.
     */
    public static class ClassNode extends DecNode {

        // Nome classe
        final String id;

        // Nome superclasse (null se assente)
        final String superID;

        // Campi dichiarati
        final List<FieldNode> fields;

        // Metodi dichiarati
        final List<MethodNode> methods;

        // Entry della superclasse nella symbol table
        STentry superEntry;

        // Tipo completo della classe (campi + metodi anche ereditati)
        ClassTypeNode type;

        public ClassNode(String id, String superID, List<FieldNode> fields, List<MethodNode> methods) {
            this.id = id;
            this.superID = superID;
            this.fields = Collections.unmodifiableList(fields);
            this.methods = Collections.unmodifiableList(methods);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Campo di una classe.
     */
    public static class FieldNode extends DecNode {

        final String id;

        // Offset nel layout dell'oggetto in heap
        int offset;

        FieldNode(String id, TypeNode type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Metodo di una classe.
     */
    public static class MethodNode extends DecNode {

        final String id;
        final TypeNode retType;
        final List<ParNode> parList;
        final List<DecNode> decList;
        final Node exp;

        // Offset nella dispatch table
        int offset;

        // Label usata in generazione di codice
        String label;

        MethodNode(String id, TypeNode retType, List<ParNode> parList, List<DecNode> decList, Node exp) {
            this.id = id;
            this.retType = retType;
            this.parList = Collections.unmodifiableList(parList);
            this.decList = Collections.unmodifiableList(decList);
            this.exp = exp;

            // Un metodo è trattato come funzione (ArrowType)
            this.type = new ArrowTypeNode(
                    this.parList.stream().map(ParNode::getType).collect(Collectors.toList()),
                    this.retType
            );
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Nodo print.
     */
    public static class PrintNode extends Node {

        final Node exp;

        PrintNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Nodo if-then-else.
     */
    public static class IfNode extends Node {

        final Node cond;
        final Node th;
        final Node el;

        IfNode(Node cond, Node th, Node el) {
            this.cond = cond;
            this.th = th;
            this.el = el;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Nodo di uguaglianza.
     */
    public static class EqualNode extends Node {

        final Node l;
        final Node r;

        EqualNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore booleano OR.
     */
    public static class OrNode extends Node {

        final Node l;
        final Node r;

        OrNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore booleano AND.
     */
    public static class AndNode extends Node {

        final Node l;
        final Node r;

        AndNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore booleano NOT.
     */
    public static class NotNode extends Node {

        final Node exp;

        NotNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore >=.
     */
    public static class GreaterEqualNode extends Node {

        final Node l;
        final Node r;

        GreaterEqualNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore <=.
     */
    public static class LessEqualNode extends Node {

        final Node l;
        final Node r;

        LessEqualNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore moltiplicazione.
     */
    public static class TimesNode extends Node {

        final Node l;
        final Node r;

        TimesNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore divisione.
     */
    public static class DivNode extends Node {

        final Node l;
        final Node r;

        DivNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore somma.
     */
    public static class PlusNode extends Node {

        final Node l;
        final Node r;

        PlusNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Operatore sottrazione.
     */
    public static class MinusNode extends Node {

        final Node l;
        final Node r;

        MinusNode(Node l, Node r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Chiamata a funzione.
     */
    public static class CallNode extends Node {

        final String id;
        final List<Node> argList;

        // Entry nella symbol table
        STentry entry;

        // Nesting level del punto di chiamata
        int nl;

        CallNode(String id, List<Node> arguments) {
            this.id = id;
            this.argList = Collections.unmodifiableList(arguments);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Chiamata a metodo: obj.meth(...)
     */
    public static class ClassCallNode extends Node {

        final String objId;
        final String methId;

        int nl;

        // Entry dell'oggetto
        STentry entry;

        // Entry del metodo nella class table
        STentry methodEntry;

        final List<Node> argList;

        public ClassCallNode(String objId, String methId, List<Node> arguments) {
            this.objId = objId;
            this.methId = methId;
            this.argList = Collections.unmodifiableList(arguments);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Uso di identificatore.
     */
    public static class IdNode extends Node {

        final String id;

        // Entry nella symbol table
        STentry entry;

        // Nesting level
        int nl;

        IdNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo riferimento a classe.
     */
    public static class RefTypeNode extends TypeNode {

        final String id;

        RefTypeNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Creazione oggetto: new Class(...)
     */
    public static class NewNode extends Node {

        // Entry della classe
        STentry entry;

        final String id;

        // Argomenti costruttore
        List<Node> argList;

        NewNode(String id, List<Node> arguments) {
            this.id = id;
            this.argList = Collections.unmodifiableList(arguments);
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Costante booleana.
     */
    public static class BoolNode extends Node {

        final Boolean val;

        BoolNode(boolean val) {
            this.val = val;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Costante intera.
     */
    public static class IntNode extends Node {

        final Integer val;

        IntNode(Integer val) {
            this.val = val;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo funzione: (parList) -> retType
     */
    public static class ArrowTypeNode extends TypeNode {

        final List<TypeNode> parList;
        final TypeNode retType;

        ArrowTypeNode(List<TypeNode> pars, TypeNode retType) {
            this.parList = Collections.unmodifiableList(pars);
            this.retType = retType;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo booleano.
     */
    public static class BoolTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo intero.
     */
    public static class IntTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo classe completo.
     *
     * allFields: lista completa dei campi (anche ereditati)
     * allMethods: lista completa dei metodi (anche ereditati)
     */
    public static class ClassTypeNode extends TypeNode {

        final List<TypeNode> allFields;
        final List<ArrowTypeNode> allMethods;

        ClassTypeNode(List<TypeNode> allFields, List<ArrowTypeNode> allMethods) {
            this.allFields = allFields;
            this.allMethods = allMethods;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Nodo vuoto (usato in casi particolari dell'AST).
     */
    public static class EmptyNode extends Node {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    /**
     * Tipo vuoto (usato in casi particolari del type system).
     */
    public static class EmptyTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }
}