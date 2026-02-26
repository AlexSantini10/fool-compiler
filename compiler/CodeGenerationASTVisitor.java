package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

    private final List<List<String>> dispatchTables = new ArrayList<>();

    CodeGenerationASTVisitor() {}

    CodeGenerationASTVisitor(boolean debug) {
        super(false, debug);
    }

    @Override
    public String visitNode(ProgLetInNode node) {
        if (print) {
            printNode(node);
        }
        String decListCode = null;
        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
        }
        return nlJoin(
                "push 0",
                decListCode,
                visit(node.exp),
                "halt",
                getCode()
        );
    }

    @Override
    public String visitNode(ProgNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.exp),
                "halt"
        );
    }

    @Override
    public String visitNode(VarNode node) {
        if (print) {
            printNode(node, node.id);
        }
        return visit(node.exp);
    }

    @Override
    public String visitNode(FunNode node) {
        if (print) {
            printNode(node, node.id);
        }
        String decListCode = null;
        String popDecList = null;
        String popParList = null;

        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
            popDecList = nlJoin(popDecList, "pop");
        }
        for (int i = 0; i < node.parList.size(); i++) {
            popParList = nlJoin(popParList, "pop");
        }

        String functionLabel = freshFunLabel();
        putCode(
                nlJoin(
                        functionLabel + ":",
                        "cfp",
                        "lra",
                        decListCode,
                        visit(node.exp),
                        "stm",
                        popDecList,
                        "sra",
                        "pop",
                        popParList,
                        "sfp",
                        "ltm",
                        "lra",
                        "js"
                )
        );
        return "push " + functionLabel;
    }

    @Override
    public String visitNode(IdNode node) {
        if (print) {
            printNode(node, node.id);
        }
        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }
        return nlJoin(
                "lfp",
                getActivationRecordCode,
                "push " + node.entry.offset,
                "add",
                "lw"
        );
    }

    @Override
    public String visitNode(CallNode node) {
        if (print) {
            printNode(node, node.id);
        }

        String argumentsCode = null;
        for (int i = node.argList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argList.get(i)));
        }

        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }

        String commonCode = nlJoin(
                "lfp",
                argumentsCode,
                "lfp",
                getActivationRecordCode,
                "stm",
                "ltm",
                "ltm"
        );

        return nlJoin(
                commonCode,
                "push " + node.entry.offset,
                "add",
                "lw",
                "js"
        );
    }

    @Override
    public String visitNode(PrintNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.exp),
                "print"
        );
    }

    @Override
    public String visitNode(IfNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.cond),
                "push 1",
                "beq " + label1,
                visit(node.el),
                "b " + label2,
                label1 + ":",
                visit(node.th),
                label2 + ":"
        );
    }

    @Override
    public String visitNode(EqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "beq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(OrNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        String label3 = freshLabel();
        String label4 = freshLabel();
        return nlJoin(
                visit(node.l),
                "push 0",
                "beq " + label1,
                "b " + label2,
                label1 + ":",
                visit(node.r),
                "push 0",
                "beq " + label3,
                label2 + ":",
                "push 1",
                "b " + label4,
                label3 + ":",
                "push 0",
                label4 + ":"
        );
    }

    @Override
    public String visitNode(AndNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.l),
                "push 0",
                "beq " + label1,
                visit(node.r),
                "push 0",
                "beq " + label1,
                "push 1",
                "b " + label2,
                label1 + ":",
                "push 0",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(NotNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.exp),
                "push 0",
                "beq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(LessEqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "bleq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(GreaterEqualNode node) {
        if (print) {
            printNode(node);
        }
        String label1 = freshLabel();
        String label2 = freshLabel();
        return nlJoin(
                visit(node.r),
                visit(node.l),
                "sub",
                "push 0",
                "bleq " + label1,
                "push 0",
                "b " + label2,
                label1 + ":",
                "push 1",
                label2 + ":"
        );
    }

    @Override
    public String visitNode(TimesNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "mult"
        );
    }

    @Override
    public String visitNode(DivNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "div"
        );
    }

    @Override
    public String visitNode(PlusNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "add"
        );
    }

    @Override
    public String visitNode(MinusNode node) {
        if (print) {
            printNode(node);
        }
        return nlJoin(
                visit(node.l),
                visit(node.r),
                "sub"
        );
    }

    @Override
    public String visitNode(ClassNode node) {
        if (print) {
            printNode(node, node.id);
        }

        List<String> dispatchTable = new ArrayList<>();
        dispatchTables.add(dispatchTable);

        if (node.superID != null) {
            List<String> superClassDispatchTable = dispatchTables.get(-node.superEntry.offset - 2);
            dispatchTable.addAll(superClassDispatchTable);
        }

        for (int i = 0; i < node.methods.size(); i++) {
            MethodNode method = node.methods.get(i);
            visit(method);

            if (method.offset < dispatchTable.size()) {
                dispatchTable.set(method.offset, method.label);
            } else {
                while (dispatchTable.size() < method.offset) {
                    dispatchTable.add(null);
                }
                dispatchTable.add(method.label);
            }
        }

        String createDispatchTable = null;
        for (String label : dispatchTable) {
            createDispatchTable = nlJoin(
                    createDispatchTable,
                    "push " + label,
                    "lhp",
                    "sw",
                    "lhp",
                    "push 1",
                    "add",
                    "shp"
            );
        }

        return nlJoin(
                "lhp",
                createDispatchTable
        );
    }

    @Override
    public String visitNode(MethodNode node) {
        if (print) {
            printNode(node, node.id);
        }

        String decListCode = null;
        String popDecList = null;

        for (Node declaration : node.decList) {
            decListCode = nlJoin(decListCode, visit(declaration));
            popDecList = nlJoin(popDecList, "pop");
        }

        String popParList = null;
        for (int i = 0; i < node.parList.size(); i++) {
            popParList = nlJoin(popParList, "pop");
        }

        String functionLabel = freshFunLabel();
        node.label = functionLabel;

        putCode(
                nlJoin(
                        functionLabel + ":",
                        "cfp",
                        "lra",
                        decListCode,
                        visit(node.exp),
                        "stm",
                        popDecList,
                        "sra",
                        "pop",
                        popParList,
                        "sfp",
                        "ltm",
                        "lra",
                        "js"
                )
        );
        return null;
    }

    @Override
    public String visitNode(ClassCallNode node) {
        if (print) {
            printNode(node, node.objId + "." + node.methId);
        }

        String argumentsCode = null;
        for (int i = node.argList.size() - 1; i >= 0; i--) {
            argumentsCode = nlJoin(argumentsCode, visit(node.argList.get(i)));
        }

        String getActivationRecordCode = null;
        for (int i = 0; i < node.nl - node.entry.nl; i++) {
            getActivationRecordCode = nlJoin(getActivationRecordCode, "lw");
        }

        return nlJoin(
                "lfp",
                argumentsCode,
                "lfp",
                getActivationRecordCode,
                "push " + node.entry.offset,
                "add",
                "lw",
                "stm",
                "ltm",
                "ltm",
                "lw",
                "push " + node.methodEntry.offset,
                "add",
                "lw",
                "js"
        );
    }

    @Override
    public String visitNode(NewNode node) {
        if (print) {
            printNode(node, node.id);
        }

        String putArgumentsOnStack = null;
        for (Node argument : node.argList) {
            putArgumentsOnStack = nlJoin(putArgumentsOnStack, visit(argument));
        }

        String loadArgumentsOnHeap = null;
        for (int i = 0; i < node.argList.size(); i++) {
            loadArgumentsOnHeap = nlJoin(
                    loadArgumentsOnHeap,
                    "lhp",
                    "sw",
                    "lhp",
                    "push 1",
                    "add",
                    "shp"
            );
        }

        return nlJoin(
                putArgumentsOnStack,
                loadArgumentsOnHeap,
                "push " + ExecuteVM.MEMSIZE,
                "push " + node.entry.offset,
                "add",
                "lw",
                "lhp",
                "sw",
                "lhp",
                "lhp",
                "push 1",
                "add",
                "shp"
        );
    }

    @Override
    public String visitNode(BoolNode node) {
        if (print) {
            printNode(node, node.val.toString());
        }
        return "push " + (node.val ? 1 : 0);
    }

    @Override
    public String visitNode(IntNode node) {
        if (print) {
            printNode(node, node.val.toString());
        }
        return "push " + node.val;
    }

    @Override
    public String visitNode(EmptyNode node) {
        if (print) {
            printNode(node);
        }
        return "push -1";
    }
}