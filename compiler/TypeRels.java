package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Classe di supporto per il type checking.
 * Utile quando il compilatore deve decidere: 
 * - posso assegnare un valore di tipo A a una variabile di tipo B?
 * - un metodo che override a un altro ha un tipo compatibile?
 * - posso passare questo argomento a quel parametro?
 * - in un if, se il ramo then ha un tipo e il ramo else un altro, che tipo ha l’intera espressione?
 */
public class TypeRels {
    // Mappa la gerarchia tra classi:
    // superType.get(C) = B significa che la classe C estende B.
    public static Map<String, String> superType = new HashMap<>();

    /**
     * Verifica se a è sottotipo di b.
     */
    public static boolean isSubtype(TypeNode a, TypeNode b) {

        /**
         * Sottotipizzazione tra tipi riferimento a classi.
         * a è sottotipo di b se a coincide con b oppure se, risalendo
         * la catena delle superclassi di a, si incontra b.
         */
        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String t = ((RefTypeNode) a).id;
            String target = ((RefTypeNode) b).id;

            while (t != null && !t.equals(target)) {
                t = superType.get(t);
            }
            return ((RefTypeNode) a).id.equals(target) || t != null;
        }

        /**
         * Sottotipizzazione tra tipi funzione.
         * - covarianza sul tipo di ritorno.
         * - controvarianza sui parametri
         * Se a = (A1,...,An)->R1 e b = (B1,...,Bn)->R2, allora a <: b se:
         *      - R1 <: R2
         *      - per ogni i, Bi <: Ai
         */
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

        /**
         * stessi tipi primitivi / stessi nodi di tipo concreti.
         * Es: IntTypeNode <: IntTypeNode, BoolTypeNode <: BoolTypeNode.
         */
        if (a.getClass().equals(b.getClass())) {
            return true;
        }

        /**
         *  Nel linguaggio Bool è considerato sottotipo di Int.
         */
        if (a instanceof BoolTypeNode && b instanceof IntTypeNode) {
            return true;
        }

        /**
         * EmptyTypeNode rappresenta il valore null / empty. È compatibile con se stesso e con tutti i tipi riferimento.
         */
        if (a instanceof EmptyTypeNode) {
            return b instanceof EmptyTypeNode || b instanceof RefTypeNode;
        }

        return false;
    }

    /**
     * Calcola il lowest common ancestor (LCA) tra due tipi.
     * Trova quindi il tipo comune più vicino tra 2 espressioni, per esempio nei rami then/else di un if.
     */
    public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {

        // Se uno dei due è EmptyTypeNode, il tipo comune è l'altro.
        if (a instanceof EmptyTypeNode) {
            return b;
        }
        if (b instanceof EmptyTypeNode) {
            return a;
        }

         /*
         * Entrambi sono tipi riferimento a classi.
         * Si risale la gerarchia di a finché non si trova il primo antenato che sia supertipo anche di b.
         */
        if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
            String ida = ((RefTypeNode) a).id;
            String idb = ((RefTypeNode) b).id;

            // Se coincidono, il loro antenato comune minimo è il tipo stesso.
            if (ida.equals(idb)) {
                return a;
            }

            
            String t = ida;
            while (t != null) {
                RefTypeNode cand = new RefTypeNode(t);
                // Se b è sottotipo del candidato, allora cand è un supertipo comune.
                if (isSubtype(b, cand)) {
                    return cand;
                }
                t = superType.get(t);
            }
            return null;
        }

          /*
         * Entrambi sono tipi primitivi bool/int.
         * Poiché Bool <: Int:
         * - LCA(bool, bool) = bool
         * - LCA(bool, int) = int
         * - LCA(int, int) = int
         */
        if ((a instanceof BoolTypeNode || a instanceof IntTypeNode)
                && (b instanceof BoolTypeNode || b instanceof IntTypeNode)) {
            if (a instanceof IntTypeNode || b instanceof IntTypeNode) {
                return new IntTypeNode();
            }
            return new BoolTypeNode();
        }

        // Nessun antenato comune significativo.
        return null;
    }
}
