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

    // Programma composto da una sola espressione
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

    // Dichiarazione di funzione
    public static class FunNode extends DecNode {

        // Nome funzione
        final String id;

        // Tipo di ritorno dichiarato
        final TypeNode retType;

        // Parametri formali in ordine
        final List<ParNode> parList;

        // Dichiarazioni locali nel corpo della funzione
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

    // Parametro formale di funzione o metodo
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

    // Dichiarazione di variabile
    public static class VarNode extends DecNode {

        final String id;

        // Espressione usata per inizializzare la variabile
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

        // Nome della superclasse, null se la classe non estende nulla
        final String superID;

        // Campi dichiarati direttamente nella classe
        final List<FieldNode> fields;

        // Metodi dichiarati direttamente nella classe
        final List<MethodNode> methods;

        // Entry della superclasse, valorizzata dalla symbol table visit
        STentry superEntry;

        // Tipo completo della classe, comprensivo dei membri ereditati
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

        final String id;

        // Offset del campo nel layout dell'oggetto
        // Nei campi l'offset serve per sapere dove leggere/scrivere dentro l'oggetto
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

        final String id;
        final TypeNode retType;
        final List<ParNode> parList;
        final List<DecNode> decList;
        final Node exp;

        // Offset del metodo nella dispatch table della classe
        // Se c'è overriding, l'offset deve restare quello ereditato
        int offset;

        // Label del codice del metodo usata nella code generation
        String label;

        MethodNode(String id, TypeNode retType, List<ParNode> parList, List<DecNode> decList, Node exp) {
            this.id = id;
            this.retType = retType;
            this.parList = Collections.unmodifiableList(parList);
            this.decList = Collections.unmodifiableList(decList);
            this.exp = exp;

            // Un metodo viene trattato come una funzione:
            // il suo tipo statico è un ArrowTypeNode(parametri -> ritorno)
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

    // Nodo di uguaglianza
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

    // Operatore booleano OR
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

    // Operatore booleano AND
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

    // Operatore booleano NOT
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

    // Operatore >=
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

    // Operatore <=
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

    // Operatore moltiplicazione
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

    // Operatore divisione
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

    // Operatore somma
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

    // Operatore sottrazione
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

        final String id;
        final List<Node> argList;

        // Entry risolta dalla symbol table visit
        STentry entry;

        // Nesting level del punto in cui avviene la chiamata
        // Serve per calcolare quanti access link risalire
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

    // Chiamata a metodo: obj.meth(...)
    public static class ClassCallNode extends Node {

        // Identificatore della variabile oggetto
        final String objId;

        // Nome del metodo chiamato
        final String methId;

        // Nesting level del punto di chiamata
        int nl;

        // Entry della variabile oggetto
        STentry entry;

        // Entry del metodo risolto nella classe dell'oggetto
        // È distinta da entry perché una cosa è trovare l'oggetto,
        // un'altra è trovare il metodo nella gerarchia di classi
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

        final String id;

        // Entry risolta dalla symbol table visit
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

    // Creazione oggetto: new Class(...)
    public static class NewNode extends Node {

        // Entry della classe istanziata
        STentry entry;

        // Nome della classe da istanziare
        final String id;

        // Argomenti usati per inizializzare i campi
        // L'ordine deve combaciare con l'ordine dei campi nel layout dell'oggetto
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

    // Costante booleana
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

    // Costante intera
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

    // Tipo funzione: (parList) -> retType
    public static class ArrowTypeNode extends TypeNode {

        // Tipi dei parametri formali in ordine
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

    // Tipo booleano
    public static class BoolTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo intero
    public static class IntTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo completo di classe
    public static class ClassTypeNode extends TypeNode {

        // Lista completa dei campi, compresi quelli ereditati
        // La posizione nella lista coincide con il layout usato dagli offset
        final List<TypeNode> allFields;

        // Lista completa dei metodi, compresi quelli ereditati
        // La posizione nella lista coincide con la dispatch table
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

    // Nodo vuoto, usato per rappresentare null nell'AST OO
    public static class EmptyNode extends Node {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    // Tipo vuoto associato a EmptyNode
    // Serve per gestire null nel type checking
    public static class EmptyTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }
}