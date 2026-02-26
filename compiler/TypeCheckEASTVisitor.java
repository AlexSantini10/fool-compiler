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

    @Override
    public TypeNode visitNode(ProgNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

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

    @Override
    public TypeNode visitNode(VarNode n) throws TypeException {
        if (print) printNode(n, n.id);
        if (!isSubtype(visit(n.exp), ckvisit(n.getType()))) {
            throw new TypeException("Incompatible value for variable " + n.id, n.getLine());
        }
        return null;
    }

    @Override
    public TypeNode visitNode(PrintNode n) throws TypeException {
        if (print) printNode(n);
        return visit(n.exp);
    }

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

    @Override
    public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, r) || isSubtype(r, l))) {
            throw new TypeException("Incompatible types in greater equal", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(LessEqualNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, r) || isSubtype(r, l))) {
            throw new TypeException("Incompatible types in less equal", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(AndNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, r) || isSubtype(r, l))) {
            throw new TypeException("Incompatible types in AND", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(OrNode n) throws TypeException {
        if (print) printNode(n);
        TypeNode l = visit(n.l);
        TypeNode r = visit(n.r);
        if (!(isSubtype(l, r) || isSubtype(r, l))) {
            throw new TypeException("Incompatible types in OR", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(NotNode n) throws TypeException {
        if (print) printNode(n);
        if (!isSubtype(visit(n.exp), new BoolTypeNode())) {
            throw new TypeException("Non boolean operand in not", n.getLine());
        }
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(TimesNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in multiplication", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(DivNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in division", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(PlusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in sum", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(MinusNode n) throws TypeException {
        if (print) printNode(n);
        if (!(isSubtype(visit(n.l), new IntTypeNode()) && isSubtype(visit(n.r), new IntTypeNode()))) {
            throw new TypeException("Non integers in subtraction", n.getLine());
        }
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(CallNode n) throws TypeException {
        if (print) printNode(n, n.id);

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

    @Override
    public TypeNode visitNode(ClassCallNode n) throws TypeException {
        if (print) printNode(n, n.objId + "." + n.methId);

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

    @Override
    public TypeNode visitNode(IdNode n) throws TypeException {
        if (print) printNode(n, n.id);
        TypeNode t = visit(n.entry);

        if (t instanceof ArrowTypeNode) {
            throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
        }
        if (t instanceof ClassTypeNode) {
            throw new TypeException("Wrong usage of class identifier " + n.id, n.getLine());
        }
        return t;
    }

    @Override
    public TypeNode visitNode(NewNode n) throws TypeException {
        if (print) printNode(n, n.id);

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

    @Override
    public TypeNode visitNode(ClassNode n) throws TypeException {
        if (print) printNode(n, n.id);

        for (MethodNode m : n.methods) {
            visit(m);
        }

        if (n.superID != null) {
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

    @Override
    public TypeNode visitNode(BoolNode n) {
        if (print) printNode(n, n.val.toString());
        return new BoolTypeNode();
    }

    @Override
    public TypeNode visitNode(IntNode n) {
        if (print) printNode(n, n.val.toString());
        return new IntTypeNode();
    }

    @Override
    public TypeNode visitNode(EmptyNode n) {
        if (print) printNode(n);
        return new EmptyTypeNode();
    }

    @Override
    public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (TypeNode par : n.parList) {
            visit(par);
        }
        visit(n.retType, "->");
        return null;
    }

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

    @Override
    public TypeNode visitNode(ClassTypeNode n) throws TypeException {
        if (print) printNode(n);
        for (TypeNode f : n.allFields) ckvisit(f);
        for (ArrowTypeNode m : n.allMethods) ckvisit(m);
        return null;
    }

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