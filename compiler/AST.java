package compiler;

import java.util.*;
import java.util.stream.Collectors;

import compiler.lib.*;

// AST del linguaggio FOOL.
// Tutte le fasi del compilatore visitano questi nodi.
// Le liste sono rese immutabili per evitare modifiche accidentali alla struttura dell'albero.
public class AST {

    // Programma della forma: let decList in exp
    public static class ProgLetInNode extends Node {

        // Lista delle dichiarazioni globali del programma
        // (variabili, funzioni o classi).
        // Sono visibili nell'espressione finale exp.
        final List<DecNode> decList;

        // Espressione principale del programma.
        // Il valore prodotto da questa espressione è il risultato del programma.
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

    // Programma composto da una sola espressione
    public static class ProgNode extends Node {

        // Espressione principale del programma
        // (caso senza dichiarazioni globali).
        final Node exp;

        ProgNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Dichiarazione di funzione
    public static class FunNode extends DecNode {

        // Nome della funzione
        final String id;

        // Tipo di ritorno dichiarato
        final TypeNode retType;

        // Lista dei parametri formali della funzione
        // nell'ordine in cui compaiono nella dichiarazione
        final List<ParNode> parList;

        // Dichiarazioni locali nel corpo della funzione
        // (variabili o funzioni annidate)
        final List<DecNode> decList;

        // Corpo della funzione
        // l'espressione che produce il valore di ritorno
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

    // Parametro formale di funzione o metodo
    public static class ParNode extends DecNode {

        // Nome del parametro
        final String id;

        // Il tipo del parametro è salvato nel campo "type"
        // ereditato da DecNode
        ParNode(String id, TypeNode type) {
            this.id = id;
            this.type = type;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Dichiarazione di variabile
    public static class VarNode extends DecNode {

        // Nome della variabile
        final String id;

        // Espressione usata per inizializzare la variabile
        // viene valutata nel momento della dichiarazione
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

    // Dichiarazione di classe
    public static class ClassNode extends DecNode {

        // Nome della classe
        final String id;

        // Nome della superclasse
        // null se la classe non estende nulla
        final String superID;

        // Campi dichiarati direttamente nella classe
        // non include quelli ereditati
        final List<FieldNode> fields;

        // Metodi dichiarati direttamente nella classe
        // non include quelli ereditati
        final List<MethodNode> methods;

        // Entry della superclasse nella Symbol Table
        // risolta durante la visita di analisi semantica
        STentry superEntry;

        // Tipo completo della classe
        // contiene tutti i campi e metodi (inclusi quelli ereditati)
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

    // Campo di una classe
    public static class FieldNode extends DecNode {

        // Nome del campo
        final String id;

        // Offset del campo nel layout dell'oggetto nello heap
        // usato nella code generation per accedere al campo
        // layout tipico:
        // offset 0  -> dispatch pointer
        // offset -1 -> primo campo
        // offset -2 -> secondo campo
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

    // Metodo di una classe
    public static class MethodNode extends DecNode {

        // Nome del metodo
        final String id;

        // Tipo di ritorno dichiarato
        final TypeNode retType;

        // Parametri formali del metodo
        final List<ParNode> parList;

        // Dichiarazioni locali nel corpo del metodo
        final List<DecNode> decList;

        // Corpo del metodo
        final Node exp;

        // Offset del metodo nella dispatch table
        // usato per il dynamic dispatch
        int offset;

        // Label del codice generato per il metodo
        // usata nella code generation per saltare al metodo
        String label;

        MethodNode(String id, TypeNode retType, List<ParNode> parList, List<DecNode> decList, Node exp) {
            this.id = id;
            this.retType = retType;
            this.parList = Collections.unmodifiableList(parList);
            this.decList = Collections.unmodifiableList(decList);
            this.exp = exp;

            // Un metodo è trattato come una funzione
            // il suo tipo è (parametri -> ritorno)
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

    // Nodo print
    public static class PrintNode extends Node {

        // Espressione da stampare
        final Node exp;

        PrintNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Nodo if-then-else
    public static class IfNode extends Node {

        // Condizione dell'if
        final Node cond;

        // Espressione eseguita se la condizione è vera
        final Node th;

        // Espressione eseguita se la condizione è falsa
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

    // Nodo di uguaglianza
    public static class EqualNode extends Node {

        // Operando sinistro
        final Node l;

        // Operando destro
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

    public static class OrNode extends Node {

        // Operando sinistro
        final Node l;

        // Operando destro
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

    public static class NotNode extends Node {

        // Espressione negata
        final Node exp;

        NotNode(Node exp) {
            this.exp = exp;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

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

    // Chiamata a funzione
    public static class CallNode extends Node {

        // Nome della funzione chiamata
        final String id;

        // Argomenti passati alla funzione
        final List<Node> argList;

        // Entry della funzione nella Symbol Table
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

    // Chiamata a metodo obj.meth(...)
    public static class ClassCallNode extends Node {

        // Variabile che contiene l'oggetto
        final String objId;

        // Metodo invocato sull'oggetto
        final String methId;

        int nl;

        // Entry della variabile oggetto
        STentry entry;

        // Entry del metodo nella virtual table
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

    // Uso di identificatore
    public static class IdNode extends Node {

        // Nome dell'identificatore
        final String id;

        // Entry della dichiarazione nella Symbol Table
        STentry entry;

        // Nesting level del punto di utilizzo
        int nl;

        IdNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo riferimento a classe
    public static class RefTypeNode extends TypeNode {

        // Nome della classe referenziata
        final String id;

        RefTypeNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Creazione oggetto new Class(...)
    public static class NewNode extends Node {

        // Entry della classe nella Symbol Table
        STentry entry;

        // Nome della classe da istanziare
        final String id;

        // Argomenti usati per inizializzare i campi dell'oggetto
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

    public static class BoolNode extends Node {

        // Valore booleano della costante
        final Boolean val;

        BoolNode(boolean val) {
            this.val = val;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IntNode extends Node {

        // Valore intero della costante
        final Integer val;

        IntNode(Integer val) {
            this.val = val;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo funzione (parList) -> retType
    public static class ArrowTypeNode extends TypeNode {

        // Tipi dei parametri
        final List<TypeNode> parList;

        // Tipo di ritorno
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

    public static class BoolTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class IntTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo completo di classe
    public static class ClassTypeNode extends TypeNode {

        // Tipi di tutti i campi della classe
        // inclusi quelli ereditati
        final List<TypeNode> allFields;

        // Tipi funzionali di tutti i metodi
        // inclusi quelli ereditati
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

    // Nodo vuoto usato per rappresentare null
    public static class EmptyNode extends Node {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo associato a null
    public static class EmptyTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }
}