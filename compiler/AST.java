package compiler;

import java.util.*;
import java.util.stream.Collectors;

import compiler.lib.*;

public class AST {

    public static class ProgLetInNode extends Node {

        final List<DecNode> decList;
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

    public static class FunNode extends DecNode {

        final String id;
        final TypeNode retType;
        final List<ParNode> parList;
        final List<DecNode> decList;
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

    public static class VarNode extends DecNode {

        final String id;
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

    public static class ClassNode extends DecNode {

        final String id;
        final String superID;
        final List<FieldNode> fields;
        final List<MethodNode> methods;
        STentry superEntry;
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

    public static class FieldNode extends DecNode {

        final String id;
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

    public static class MethodNode extends DecNode {

        final String id;
        final TypeNode retType;
        final List<ParNode> parList;
        final List<DecNode> decList;
        final Node exp;
        int offset;
        String label;

        MethodNode(String id, TypeNode retType, List<ParNode> parList, List<DecNode> decList, Node exp) {
            this.id = id;
            this.retType = retType;
            this.parList = Collections.unmodifiableList(parList);
            this.decList = Collections.unmodifiableList(decList);
            this.exp = exp;
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

    public static class CallNode extends Node {

        final String id;
        final List<Node> argList;
        STentry entry;
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

    public static class ClassCallNode extends Node {

        final String objId;
        final String methId;

        int nl;
        STentry entry;
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

    public static class IdNode extends Node {

        final String id;
        STentry entry;
        int nl;

        IdNode(String id) {
            this.id = id;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

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

    public static class NewNode extends Node {

        STentry entry;
        final String id;
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

        final Integer val;

        IntNode(Integer val) {
            this.val = val;
        }

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

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

    public static class EmptyNode extends Node {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }

    public static class EmptyTypeNode extends TypeNode {

        @Override
        public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
            return visitor.visitNode(this);
        }
    }
}