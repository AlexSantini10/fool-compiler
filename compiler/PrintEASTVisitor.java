package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void, VoidException> {

    PrintEASTVisitor() {
        super(false, true);
    }

    @Override
    public Void visitNode(ProgLetInNode n) {
        printNode(n);
        for (Node declaration : n.decList) {
            visit(declaration);
        }
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ProgNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ClassNode n) {
        printNode(n, n.id + (n.superID != null ? " extends " + n.superID : ""));
        for (FieldNode field : n.fields) {
            visit(field);
        }
        for (MethodNode method : n.methods) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visitNode(FieldNode n) {
        printNode(n, n.id + " offset " + n.offset);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(MethodNode n) {
        printNode(n, n.id + " offset " + n.offset);
        visit(n.retType);
        for (ParNode parameter : n.parList) {
            visit(parameter);
        }
        for (DecNode declaration : n.decList) {
            visit(declaration);
        }
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(NewNode n) {
        printNode(n, n.id);
        visit(n.entry);
        for (Node argument : n.argList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode n) {
        printNode(n, n.id);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        printNode(n, n.id);
        visit(n.retType);
        for (ParNode parameter : n.parList) {
            visit(parameter);
        }
        for (Node declaration : n.decList) {
            visit(declaration);
        }
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(ParNode n) {
        printNode(n, n.id);
        visit(n.getType());
        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        printNode(n, n.id);
        visit(n.getType());
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(PrintNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(IfNode n) {
        printNode(n);
        visit(n.cond);
        visit(n.th);
        visit(n.el);
        return null;
    }

    @Override
    public Void visitNode(EqualNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(NotNode n) {
        printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(LessEqualNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(GreaterEqualNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(OrNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(AndNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(TimesNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(DivNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(PlusNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(MinusNode n) {
        printNode(n);
        visit(n.l);
        visit(n.r);
        return null;
    }

    @Override
    public Void visitNode(CallNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nl);
        visit(n.entry);
        for (Node argument : n.argList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode n) {
        printNode(n, n.objId + "." + n.methId + " at nl " + n.nl);
        visit(n.entry);
        visit(n.methodEntry);
        for (Node argument : n.argList) {
            visit(argument);
        }
        return null;
    }

    @Override
    public Void visitNode(IdNode n) {
        printNode(n, n.id + " at nestinglevel " + n.nl);
        visit(n.entry);
        return null;
    }

    @Override
    public Void visitNode(BoolNode n) {
        printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(IntNode n) {
        printNode(n, n.val.toString());
        return null;
    }

    @Override
    public Void visitNode(ArrowTypeNode n) {
        printNode(n);
        for (TypeNode parameter : n.parList) {
            visit(parameter);
        }
        visit(n.retType, "->");
        return null;
    }

    @Override
    public Void visitNode(BoolTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(IntTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(ClassTypeNode n) {
        printNode(n);
        for (TypeNode field : n.allFields) {
            visit(field);
        }
        for (ArrowTypeNode method : n.allMethods) {
            visit(method);
        }
        return null;
    }

    @Override
    public Void visitNode(EmptyNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitNode(EmptyTypeNode n) {
        printNode(n);
        return null;
    }

    @Override
    public Void visitSTentry(STentry entry) {
        printSTentry("nestlev " + entry.nl);
        printSTentry("type");
        visit(entry.type);
        printSTentry("offset " + entry.offset);
        return null;
    }
}