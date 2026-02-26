package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class TypeRels {

    public static Map<String, String> superType = new HashMap<>();

    public static boolean isSubtype(TypeNode a, TypeNode b) {

        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String t = ((RefTypeNode) a).id;
            String target = ((RefTypeNode) b).id;

            while (t != null && !t.equals(target)) {
                t = superType.get(t);
            }
            return ((RefTypeNode) a).id.equals(target) || t != null;
        }

        if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode) {
            ArrowTypeNode atA = (ArrowTypeNode) a;
            ArrowTypeNode atB = (ArrowTypeNode) b;

            if (atA.parList.size() != atB.parList.size()) {
                return false;
            }

            return isSubtype(atA.retType, atB.retType)
                    && IntStream.range(0, atA.parList.size())
                    .allMatch(i -> isSubtype(atB.parList.get(i), atA.parList.get(i)));
        }

        if (a.getClass().equals(b.getClass())) {
            return true;
        }

        if (a instanceof BoolTypeNode && b instanceof IntTypeNode) {
            return true;
        }

        if (a instanceof EmptyTypeNode) {
            return b instanceof EmptyTypeNode || b instanceof RefTypeNode;
        }

        return false;
    }

    public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {

        if (a instanceof EmptyTypeNode) {
            return b;
        }
        if (b instanceof EmptyTypeNode) {
            return a;
        }

        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String ida = ((RefTypeNode) a).id;
            String idb = ((RefTypeNode) b).id;

            if (ida.equals(idb)) {
                return a;
            }

            String t = ida;
            while (t != null) {
                RefTypeNode cand = new RefTypeNode(t);
                if (isSubtype(b, cand)) {
                    return cand;
                }
                t = superType.get(t);
            }
            return null;
        }

        if ((a instanceof BoolTypeNode || a instanceof IntTypeNode)
                && (b instanceof BoolTypeNode || b instanceof IntTypeNode)) {
            if (a instanceof IntTypeNode || b instanceof IntTypeNode) {
                return new IntTypeNode();
            }
            return new BoolTypeNode();
        }

        return null;
    }
}
