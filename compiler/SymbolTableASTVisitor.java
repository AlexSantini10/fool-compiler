package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void, VoidException> {

    private final List<Map<String, STentry>> symTable = new ArrayList<>();
    private final Map<String, Map<String, STentry>> classTable = new HashMap<>();

    private int nestingLevel = 0;
    private int decOffset = -2;

    private int currentFieldOffset;
    private int currentMethodOffset;

    int stErrors = 0;

    private STentry stLookup(String id) {
        int j = nestingLevel;
        STentry entry = null;
        while (j >= 0 && entry == null) {
            entry = symTable.get(j--).get(id);
        }
        return entry;
    }

    SymbolTableASTVisitor() {}

    SymbolTableASTVisitor(boolean debug) {
        super(debug);
    }

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

    @Override
    public Void visitNode(ProgNode n) {
        if (print) printNode(n);
        visit(n.exp);
        return null;
    }

    @Override
    public Void visitNode(FunNode n) {
        if (print) printNode(n);

        Map<String, STentry> scopeTable = symTable.get(nestingLevel);

        List<TypeNode> parTypes = new ArrayList<>();
        for (ParNode par : n.parList) {
            parTypes.add(par.getType());
        }

        STentry entry = new STentry(
                nestingLevel,
                new ArrowTypeNode(parTypes, n.retType),
                decOffset--
        );

        if (scopeTable.put(n.id, entry) != null) {
            System.out.println("Fun id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }

        nestingLevel++;
        Map<String, STentry> funScope = new HashMap<>();
        symTable.add(funScope);

        int prevNLDecOffset = decOffset;
        decOffset = -2;

        int parOffset = 1;
        for (ParNode par : n.parList) {
            if (funScope.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
                System.out.println("Par id " + par.id + " at line " + n.getLine() + " already declared");
                stErrors++;
            }
        }

        for (Node dec : n.decList) {
            visit(dec);
        }
        visit(n.exp);

        symTable.remove(nestingLevel--);
        decOffset = prevNLDecOffset;

        return null;
    }

    @Override
    public Void visitNode(VarNode n) {
        if (print) printNode(n);

        visit(n.exp);

        Map<String, STentry> scopeTable = symTable.get(nestingLevel);
        STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);

        if (scopeTable.put(n.id, entry) != null) {
            System.out.println("Var id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }
        return null;
    }

    @Override
    public Void visitNode(ClassNode n) {
        if (print) printNode(n, n.id);

        Map<String, STentry> globalST = symTable.get(0);

        ClassTypeNode ct = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());

        if (n.superID != null) {
            Map<String, STentry> superVT = classTable.get(n.superID);
            if (superVT == null) {
                System.out.println("Extending class id " + n.superID + " at line " + n.getLine() + " not declared");
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

        if (globalST.put(n.id, classEntry) != null) {
            System.out.println("Class id " + n.id + " at line " + n.getLine() + " already declared");
            stErrors++;
        }

        nestingLevel++;
        Map<String, STentry> vt = new HashMap<>();
        if (n.superID != null && classTable.containsKey(n.superID)) {
            vt.putAll(classTable.get(n.superID));
        }
        classTable.put(n.id, vt);
        symTable.add(vt);

        if (n.superID != null && n.superEntry != null) {
            currentFieldOffset = -((ClassTypeNode) n.superEntry.type).allFields.size() - 1;
            currentMethodOffset = ((ClassTypeNode) n.superEntry.type).allMethods.size();
        } else {
            currentFieldOffset = -1;
            currentMethodOffset = 0;
        }

        Set<String> seenInClass = new HashSet<>();

        for (FieldNode f : n.fields) {
            if (seenInClass.contains(f.id)) {
                System.out.println("Field id " + f.id + " at line " + f.getLine() + " already declared in class");
                stErrors++;
            }
            seenInClass.add(f.id);

            STentry overridden = vt.get(f.id);
            STentry fe;

            if (overridden != null && overridden.offset < 0) {
                fe = new STentry(nestingLevel, f.getType(), overridden.offset);
                ct.allFields.set(-fe.offset - 1, fe.type);
            } else {
                fe = new STentry(nestingLevel, f.getType(), currentFieldOffset--);
                ct.allFields.add(-fe.offset - 1, fe.type);
                if (overridden != null) {
                    System.out.println("Cannot override field id " + f.id + " with a method");
                    stErrors++;
                }
            }

            vt.put(f.id, fe);
            f.offset = fe.offset;
        }

        int prevNLDecOffset = decOffset;
        int prevClassMethodOffset = currentMethodOffset;
        decOffset = currentMethodOffset;

        for (MethodNode m : n.methods) {
            if (seenInClass.contains(m.id)) {
                System.out.println("Method id " + m.id + " at line " + m.getLine() + " already declared in class");
                stErrors++;
            }
            seenInClass.add(m.id);

            visit(m);

            STentry me = vt.get(m.id);
            ArrowTypeNode funType = (ArrowTypeNode) me.type;

            if (m.offset < ct.allMethods.size()) {
                ct.allMethods.set(m.offset, funType);
            } else {
                while (ct.allMethods.size() < m.offset) {
                    ct.allMethods.add(null);
                }
                ct.allMethods.add(funType);
            }
        }

        decOffset = prevNLDecOffset;
        currentMethodOffset = prevClassMethodOffset;

        symTable.remove(nestingLevel--);
        return null;
    }

    @Override
    public Void visitNode(FieldNode n) {
        if (print) printNode(n, n.id);
        // handled in visitNode(ClassNode)
        return null;
    }

    @Override
    public Void visitNode(MethodNode n) {
        if (print) printNode(n, n.id);

        Map<String, STentry> vt = symTable.get(nestingLevel);

        List<TypeNode> parTypes = new ArrayList<>();
        for (ParNode p : n.parList) {
            parTypes.add(p.getType());
        }
        ArrowTypeNode mType = new ArrowTypeNode(parTypes, n.retType);

        STentry overridden = vt.get(n.id);
        STentry me;

        if (overridden != null && overridden.offset >= 0) {
            me = new STentry(nestingLevel, mType, overridden.offset);
        } else {
            me = new STentry(nestingLevel, mType, decOffset++);
            if (overridden != null) {
                System.out.println("Cannot override method id " + n.id + " with a field");
                stErrors++;
            }
        }

        vt.put(n.id, me);
        n.offset = me.offset;

        nestingLevel++;
        Map<String, STentry> methodScope = new HashMap<>();
        symTable.add(methodScope);

        int prevNLDecOffset = decOffset;
        decOffset = -2;

        int parOffset = 1;
        for (ParNode p : n.parList) {
            if (methodScope.put(p.id, new STentry(nestingLevel, p.getType(), parOffset++)) != null) {
                System.out.println("Par id " + p.id + " at line " + n.getLine() + " already declared");
                stErrors++;
            }
        }

        for (DecNode d : n.decList) {
            visit(d);
        }
        visit(n.exp);

        symTable.remove(nestingLevel--);
        decOffset = prevNLDecOffset;

        return null;
    }

    @Override
    public Void visitNode(NewNode n) {
        if (print) printNode(n, n.id);

        STentry classEntry = symTable.get(0).get(n.id);
        if (classEntry == null) {
            System.out.println("Class id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        } else {
            n.entry = classEntry;
        }

        for (Node arg : n.argList) {
            visit(arg);
        }
        return null;
    }

    @Override
    public Void visitNode(RefTypeNode n) {
        if (print) printNode(n, n.id);

        if (!classTable.containsKey(n.id)) {
            System.out.println("Class with id " + n.id + " at line " + n.getLine() + " not declared");
            stErrors++;
        }
        return null;
    }

    @Override
    public Void visitNode(ClassCallNode n) {
        if (print) printNode(n, n.objId + "." + n.methId);

        STentry objEntry = stLookup(n.objId);
        if (objEntry == null) {
            System.out.println("Object id " + n.objId + " at line " + n.getLine() + " not declared");
            stErrors++;
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        if (!(objEntry.type instanceof RefTypeNode)) {
            System.out.println("Object id " + n.objId + " at line " + n.getLine() + " is not a class reference");
            stErrors++;
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        n.entry = objEntry;
        n.nl = nestingLevel;

        String classId = ((RefTypeNode) objEntry.type).id;
        Map<String, STentry> vtable = classTable.get(classId);
        if (vtable == null) {
            System.out.println("Class " + classId + " for object " + n.objId + " not declared");
            stErrors++;
            for (Node arg : n.argList) visit(arg);
            return null;
        }

        STentry methodEntry = vtable.get(n.methId);
        if (methodEntry == null
                || methodEntry.offset < 0
                || !(methodEntry.type instanceof ArrowTypeNode)) {
            System.out.println("Method id " + n.methId + " at line " + n.getLine()
                    + " not declared in class " + classId);
            stErrors++;
        } else {
            n.methodEntry = methodEntry;
        }

        for (Node arg : n.argList) {
            visit(arg);
        }
        return null;
    }

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