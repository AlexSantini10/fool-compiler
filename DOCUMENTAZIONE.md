# Documentazione del Compilatore

> Documentazione tecnica completa del compilatore **FOOL** (*Functional Object-Oriented Language*).  
> Generata a partire dal codice sorgente del progetto.

---

## Indice

- [1. Panoramica](#1-panoramica)
- [2. Architettura del compilatore](#2-architettura-del-compilatore)
- [3. Linguaggio FOOL](#3-linguaggio-fool)
  - [3.1 Struttura del programma](#31-struttura-del-programma)
  - [3.2 Dichiarazioni](#32-dichiarazioni)
  - [3.3 Espressioni](#33-espressioni)
  - [3.4 Funzioni](#34-funzioni)
  - [3.5 Classi e metodi](#35-classi-e-metodi)
  - [3.6 Creazione di oggetti](#36-creazione-di-oggetti)
- [4. Parsing](#4-parsing)
  - [4.1 Lexer ANTLR](#41-lexer-antlr)
  - [4.2 Parser ANTLR](#42-parser-antlr)
  - [4.3 Conversione Parse Tree → AST](#43-conversione-parse-tree--ast)
- [5. Struttura dell'AST](#5-struttura-dellast)
  - [5.1 Nodi programma](#51-nodi-programma)
  - [5.2 Nodi dichiarazione](#52-nodi-dichiarazione)
  - [5.3 Nodi espressione](#53-nodi-espressione)
  - [5.4 Nodi tipo](#54-nodi-tipo)
  - [5.5 Nodi object-oriented](#55-nodi-object-oriented)
- [6. Symbol Table](#6-symbol-table)
  - [6.1 Struttura degli scope](#61-struttura-degli-scope)
  - [6.2 STentry](#62-stentry)
  - [6.3 Gestione degli offset](#63-gestione-degli-offset)
  - [6.4 Algoritmo di lookup](#64-algoritmo-di-lookup)
  - [6.5 SymbolTableASTVisitor](#65-symboltableastvisitor)
- [7. Sistema di tipi](#7-sistema-di-tipi)
- [8. Sottotipaggio](#8-sottotipaggio)
  - [8.1 Regole di sottotipo](#81-regole-di-sottotipo)
  - [8.2 Covarianza e controvarianza](#82-covarianza-e-controvarianza)
  - [8.3 Lowest Common Ancestor](#83-lowest-common-ancestor)
  - [8.4 Gestione di null](#84-gestione-di-null)
- [9. Type Checking](#9-type-checking)
  - [9.1 Dichiarazioni di variabili e funzioni](#91-dichiarazioni-di-variabili-e-funzioni)
  - [9.2 Operatori](#92-operatori)
  - [9.3 Chiamate di funzione e metodo](#93-chiamate-di-funzione-e-metodo)
  - [9.4 Creazione oggetti ed ereditarietà](#94-creazione-oggetti-ed-ereditarietà)
- [10. Estensione Object-Oriented](#10-estensione-object-oriented)
  - [10.1 Dichiarazione di classi](#101-dichiarazione-di-classi)
  - [10.2 Ereditarietà e overriding](#102-ereditarietà-e-overriding)
  - [10.3 Layout dei campi e dei metodi](#103-layout-dei-campi-e-dei-metodi)
  - [10.4 Dispatch table](#104-dispatch-table)
- [11. Code Generation](#11-code-generation)
  - [11.1 Struttura del code generator](#111-struttura-del-code-generator)
  - [11.2 Funzioni e metodi](#112-funzioni-e-metodi)
  - [11.3 Chiamate di funzione](#113-chiamate-di-funzione)
  - [11.4 Chiamate di metodo](#114-chiamate-di-metodo)
  - [11.5 Oggetti e dispatch](#115-oggetti-e-dispatch)
  - [11.6 Operatori e controllo di flusso](#116-operatori-e-controllo-di-flusso)
- [12. Modello di runtime](#12-modello-di-runtime)
- [13. Layout della memoria](#13-layout-della-memoria)
  - [13.1 Activation Record](#131-activation-record)
  - [13.2 Oggetto nello heap](#132-oggetto-nello-heap)
  - [13.3 Dispatch table](#133-dispatch-table)
- [14. Stack Virtual Machine](#14-stack-virtual-machine)
  - [14.1 Registri](#141-registri)
  - [14.2 Set di istruzioni](#142-set-di-istruzioni)
  - [14.3 Ciclo fetch-decode-execute](#143-ciclo-fetch-decode-execute)
- [15. Esecuzione del programma](#15-esecuzione-del-programma)
  - [15.1 Chiamata di funzione](#151-chiamata-di-funzione)
  - [15.2 Chiamata di metodo e dynamic dispatch](#152-chiamata-di-metodo-e-dynamic-dispatch)
  - [15.3 Allocazione di oggetti](#153-allocazione-di-oggetti)
- [16. Visitor Pattern](#16-visitor-pattern)
  - [16.1 BaseASTVisitor](#161-baseastvisitor)
  - [16.2 BaseEASTVisitor](#162-baseeastvisitor)
  - [16.3 Double dispatch](#163-double-dispatch)
  - [16.4 Visitor implementati](#164-visitor-implementati)

---

## 1. Panoramica

Il progetto implementa un compilatore completo per **FOOL** (*Functional Object-Oriented Language*), un linguaggio di programmazione che unisce il paradigma **funzionale** con un'**estensione object-oriented** con ereditarietà singola.

Il compilatore traduce sorgenti FOOL in codice eseguibile su una **Stack Virtual Machine** (SVM) implementata in Java. Il progetto è stato sviluppato nell'ambito del corso *Linguaggi Compilatori e Modelli Computazionali* — LM Ingegneria e Scienze Informatiche, Università di Bologna.

Le funzionalità principali del linguaggio compilato includono:

| Categoria | Funzionalità |
|---|---|
| **Tipi primitivi** | `int`, `bool` |
| **Variabili** | dichiarazione con `var id : tipo = espressione` |
| **Funzioni** | dichiarazione, parametri tipati, scope lessicale |
| **Classi** | ereditarietà singola, campi, metodi |
| **Oggetti** | istanziazione con `new`, invocazione metodi con `.` |
| **Polimorfismo** | subtyping, dynamic dispatch via dispatch table |
| **Espressioni** | aritmetica, logica, confronto, if-then-else |
| **Null** | riferimento nullo compatibile con tutti i tipi classe |

---

## 2. Architettura del compilatore

Il compilatore è organizzato in una **pipeline sequenziale di sei fasi**, ciascuna implementata da una classe Java dedicata. Il punto di ingresso è la classe `Test`, che orchestra l'intera catena di elaborazione leggendo il sorgente da `prova.fool`.

```
prova.fool (sorgente FOOL)
        │
        ▼
┌───────────────────────┐
│  Lexer + Parser       │  FOOLLexer, FOOLParser   (generati da ANTLR su FOOL.g4)
│  (tokenizzazione e    │
│   parsing sintattico) │
└───────────┬───────────┘
            │  Parse Tree (ANTLR ParseTree)
            ▼
┌───────────────────────┐
│  Costruzione AST      │  ASTGenerationSTVisitor
│  (da parse tree a AST)│
└───────────┬───────────┘
            │  AST (albero di Node)
            ▼
┌───────────────────────┐
│  Symbol Table         │  SymbolTableASTVisitor
│  (risoluzione nomi,   │
│   costruzione EAST)   │
└───────────┬───────────┘
            │  EAST (Enriched AST, con STentry)
            ▼
┌───────────────────────┐
│  Type Checking        │  TypeCheckEASTVisitor
│  (verifica dei tipi,  │
│   sottotipaggio)      │
└───────────┬───────────┘
            │  EAST type-checked
            ▼
┌───────────────────────┐
│  Code Generation      │  CodeGenerationASTVisitor
│  (emissione assembly) │
└───────────┬───────────┘
            │  prova.fool.asm (codice SVM assembly)
            ▼
┌───────────────────────┐
│  Assembly + Esecuzione│  SVMLexer, SVMParser, ExecuteVM
│  (SVM interpreta il   │
│   codice generato)    │
└───────────────────────┘
```

### Riepilogo delle classi per fase

| Fase | Classe principale | File |
|---|---|---|
| Lessicale + Sintattica | `FOOLLexer`, `FOOLParser` | generati da `FOOL.g4` |
| Costruzione AST | `ASTGenerationSTVisitor` | `compiler/ASTGenerationSTVisitor.java` |
| Symbol Table | `SymbolTableASTVisitor` | `compiler/SymbolTableASTVisitor.java` |
| Type Checking | `TypeCheckEASTVisitor` | `compiler/TypeCheckEASTVisitor.java` |
| Code Generation | `CodeGenerationASTVisitor` | `compiler/CodeGenerationASTVisitor.java` |
| Esecuzione | `ExecuteVM` | `svm/ExecuteVM.java` |

### Gestione degli errori

Il compilatore raccoglie errori da ogni fase e li riporta con contatori specifici:

- `lexer.lexicalErrors` — errori lessicali
- `parser.getNumberOfSyntaxErrors()` — errori sintattici
- `symtableVisitor.stErrors` — errori di symbol table
- `FOOLlib.typeErrors` — errori di type checking

Se la somma `frontEndErrors > 0`, l'esecuzione termina con `System.exit(1)` senza procedere alla code generation.

---

## 3. Linguaggio FOOL

La sintassi del linguaggio è definita nella grammatica ANTLR `compiler/FOOL.g4`.

### 3.1 Struttura del programma

Un programma FOOL è costituito da un'unica **produzione radice** `prog`:

```antlr
prog : progbody EOF ;

progbody : LET ( cldec+ dec* | dec+ ) IN exp SEMIC  #letInProg
         | exp SEMIC                                  #noDecProg
         ;
```

Esistono due forme:

- **`letInProg`**: uno o più blocchi di dichiarazioni (`cldec` per le classi, `dec` per funzioni/variabili), seguiti da `in exp`. Le classi devono precedere le altre dichiarazioni.
- **`noDecProg`**: una singola espressione senza dichiarazioni.

**Esempio minimo:**

```fool
42;
```

**Esempio con dichiarazioni:**

```fool
let
  var x : int = 10;
  fun doppio : int (n : int) n + n;
in
  print(doppio(x));
```

### 3.2 Dichiarazioni

Le dichiarazioni (`dec`) possono essere di due tipi:

```antlr
dec : VAR ID COLON type ASS exp SEMIC   #vardec
    | FUN ID COLON type
          LPAR (ID COLON type (COMMA ID COLON type)*)? RPAR
               (LET dec+ IN)? exp
          SEMIC                          #fundec
    ;
```

- **`vardec`**: dichiara una variabile con tipo esplicito e valore iniziale.
- **`fundec`**: dichiara una funzione con tipo di ritorno, parametri tipati e corpo. Il corpo può contenere dichiarazioni locali (`let dec+ in`).

### 3.3 Espressioni

Il linguaggio supporta le seguenti categorie di espressioni:

```antlr
exp : exp (TIMES | DIV) exp           #timesDiv
    | exp (PLUS | MINUS) exp          #plusMinus
    | exp (EQ | GE | LE) exp          #comp
    | exp (AND | OR) exp              #andOr
    | NOT exp                          #not
    | LPAR exp RPAR                    #pars
    | MINUS? NUM                       #integer
    | TRUE                             #true
    | FALSE                            #false
    | NULL                             #null
    | NEW ID LPAR (exp (COMMA exp)*)? RPAR  #new
    | IF exp THEN CLPAR exp CRPAR ELSE CLPAR exp CRPAR  #if
    | PRINT LPAR exp RPAR              #print
    | ID                               #id
    | ID LPAR (exp (COMMA exp)*)? RPAR #call
    | ID DOT ID LPAR (exp (COMMA exp)*)? RPAR #dotCall
    ;
```

| Categoria | Operatori/Forme |
|---|---|
| Aritmetici | `+`, `-`, `*`, `/` |
| Relazionali | `==`, `>=`, `<=` |
| Logici | `&&`, `||`, `!` |
| Letterali | interi (con segno opzionale), `true`, `false`, `null` |
| Controllo flusso | `if exp then { exp } else { exp }` |
| Chiamata funzione | `id(arg1, arg2, ...)` |
| Chiamata metodo | `id.metodo(arg1, arg2, ...)` |
| Creazione oggetto | `new ClassName(arg1, arg2, ...)` |
| Stampa | `print(exp)` |

### 3.4 Funzioni

Le funzioni sono **first-class** nel senso che possono essere dichiarate in qualsiasi scope lessicale, compreso quello locale di un'altra funzione. Le funzioni supportano la **ricorsione** e la **chiusura lessicale** tramite la catena degli access link a runtime.

```fool
fun fattoriale : int (n : int)
  if n == 0
  then { 1 }
  else { n * fattoriale(n - 1) };
```

### 3.5 Classi e metodi

Le classi si dichiarano nel blocco `cldec`, che precede obbligatoriamente le altre dichiarazioni:

```antlr
cldec : CLASS ID (EXTENDS ID)?
            LPAR (ID COLON type (COMMA ID COLON type)*)? RPAR
            CLPAR methdec* CRPAR ;

methdec : FUN ID COLON type
              LPAR (ID COLON type (COMMA ID COLON type)*)? RPAR
                   (LET dec+ IN)? exp
              SEMIC ;
```

- I campi sono dichiarati nella lista tra parentesi tonde.
- I metodi sono dichiarati nel blocco tra parentesi graffe con la keyword `fun`.
- L'ereditarietà singola si indica con `extends NomeClasse`.

**Esempio:**

```fool
class Animale (nome : int) {
  fun verso : int () 0;
}

class Cane extends Animale () {
  fun verso : int () 1;
}
```

### 3.6 Creazione di oggetti

Gli oggetti si istanziano con `new`, passando i valori iniziali dei campi nell'ordine di dichiarazione:

```fool
var fido : Cane = new Cane(42);
```

L'invocazione di metodi avviene tramite notazione puntata:

```fool
fido.verso()
```

---

## 4. Parsing

### 4.1 Lexer ANTLR

Il lexer è definito nella sezione *LEXER RULES* di `FOOL.g4`. Riconosce:

- **Keyword**: `class`, `extends`, `new`, `if`, `then`, `else`, `let`, `in`, `fun`, `var`, `true`, `false`, `null`, `int`, `bool`, `print`
- **Operatori**: `+`, `-`, `*`, `/`, `==`, `>=`, `<=`, `&&`, `||`, `!`, `=`
- **Delimitatori**: `(`, `)`, `{`, `}`, `;`, `:`, `,`, `.`
- **Numeri**: `NUM` — zero oppure sequenza di cifre con prima cifra non zero
- **Identificatori**: `ID` — sequenza di lettere e cifre, inizia con lettera
- **Whitespace e commenti**: inviati al canale `HIDDEN` (ignorati dal parser)
- **Caratteri non validi**: regola `ERR` — incrementa `lexicalErrors` e scarta il carattere

Il lexer è generato da ANTLR come classe `FOOLLexer` in `compiler/FOOLLexer.java`.

### 4.2 Parser ANTLR

Il parser è generato come classe `FOOLParser` in `compiler/FOOLParser.java`. Costruisce un **parse tree** (albero concreto) seguendo le regole grammaticali di `FOOL.g4`.

Ogni alternativa delle regole grammaticali ha un'**etichetta** (es. `#letInProg`, `#vardec`, `#timesDiv`) che ANTLR utilizza per generare metodi distinti nell'interfaccia visitor `FOOLVisitor`.

### 4.3 Conversione Parse Tree → AST

La classe `ASTGenerationSTVisitor` implementa il visitor ANTLR (`FOOLBaseVisitor<Node>`) e trasforma il parse tree in un AST. Il visitor viene invocato con:

```java
ASTGenerationSTVisitor visitor = new ASTGenerationSTVisitor();
Node ast = visitor.visit(st); // st è il parse tree
```

Per ogni nodo del parse tree, il visitor costruisce il corrispondente nodo AST (tutte le classi sono definite in `AST.java`). I nodi AST contengono solo le informazioni semanticamente rilevanti, eliminando la ridondanza sintattica del parse tree (parentesi, keyword, separatori).

**Esempio — conversione di una dichiarazione variabile:**

```
Parse tree: VardecContext
  ├── VAR token
  ├── ID token ("x")
  ├── COLON token
  ├── type subtree → IntTypeNode
  ├── ASS token
  └── exp subtree → IntNode(10)

AST: VarNode
  ├── id = "x"
  ├── type = IntTypeNode
  └── exp = IntNode(10)
```

---

## 5. Struttura dell'AST

Tutte le classi dei nodi AST sono definite in `compiler/AST.java`. La gerarchia di base è:

```
Visitable (interfaccia)
  └── Node (classe base per tutti i nodi)
        ├── TypeNode (nodi tipo)
        └── DecNode (nodi dichiarazione, aggiunge campo type)
```

### 5.1 Nodi programma

#### `ProgNode`
Rappresenta un programma senza dichiarazioni (solo un'espressione).

```java
class ProgNode extends Node {
    Node exp;   // espressione principale
}
```

#### `ProgLetInNode`
Rappresenta un programma con dichiarazioni globali seguito da un'espressione.

```java
class ProgLetInNode extends Node {
    List<DecNode> decList;  // dichiarazioni globali (classi, funzioni, variabili)
    Node exp;               // espressione principale
}
```

### 5.2 Nodi dichiarazione

#### `VarNode`
Dichiarazione di variabile locale o globale.

```java
class VarNode extends DecNode {
    String id;      // nome della variabile
    Node exp;       // espressione di inizializzazione
    TypeNode type;  // tipo dichiarato (ereditato da DecNode)
}
```

#### `FunNode`
Dichiarazione di funzione.

```java
class FunNode extends DecNode {
    String id;              // nome della funzione
    TypeNode retType;       // tipo di ritorno
    List<ParNode> parList;  // lista parametri
    List<DecNode> decList;  // dichiarazioni locali
    Node exp;               // corpo della funzione
}
```

#### `ParNode`
Parametro formale di una funzione o metodo.

```java
class ParNode extends DecNode {
    String id;      // nome del parametro
    // tipo ereditato da DecNode
}
```

### 5.3 Nodi espressione

| Nodo | Campi | Descrizione |
|---|---|---|
| `IdNode` | `id`, `entry` (STentry), `nl` | Riferimento a variabile/parametro |
| `CallNode` | `id`, `argList`, `entry`, `nl` | Chiamata di funzione |
| `IfNode` | `cond`, `th`, `el` | Espressione condizionale |
| `PrintNode` | `exp` | Stampa su stdout |
| `IntNode` | `val` (Integer) | Costante intera |
| `BoolNode` | `val` (Boolean) | Costante booleana |
| `EmptyNode` | — | Rappresenta `null` |
| `PlusNode` | `l`, `r` | Addizione |
| `MinusNode` | `l`, `r` | Sottrazione |
| `TimesNode` | `l`, `r` | Moltiplicazione |
| `DivNode` | `l`, `r` | Divisione |
| `EqualNode` | `l`, `r` | Uguaglianza (`==`) |
| `GreaterEqualNode` | `l`, `r` | Maggiore o uguale (`>=`) |
| `LessEqualNode` | `l`, `r` | Minore o uguale (`<=`) |
| `AndNode` | `l`, `r` | And logico (`&&`) |
| `OrNode` | `l`, `r` | Or logico (`||`) |
| `NotNode` | `exp` | Negazione logica (`!`) |

#### `CallNode` e risoluzione dei nomi

Dopo la fase di symbol table, i nodi `IdNode` e `CallNode` vengono **arricchiti** con un riferimento all'`STentry` corrispondente e con il nesting level (`nl`) al punto d'uso, fondamentale per la generazione della catena degli access link.

### 5.4 Nodi tipo

| Nodo | Descrizione |
|---|---|
| `IntTypeNode` | Tipo `int` |
| `BoolTypeNode` | Tipo `bool` |
| `ArrowTypeNode` | Tipo funzione: `(T1, ..., Tn) → R` |
| `RefTypeNode` | Tipo riferimento a classe: contiene `id` (nome classe) |
| `ClassTypeNode` | Tipo completo di classe: `allFields`, `allMethods` |
| `EmptyTypeNode` | Tipo di `null` |

### 5.5 Nodi object-oriented

#### `ClassNode`
Rappresenta la dichiarazione di una classe.

```java
class ClassNode extends DecNode {
    String id;                // nome della classe
    String superID;           // nome della superclasse (null se assente)
    List<FieldNode> fields;   // campi diretti (non ereditati)
    List<MethodNode> methods; // metodi diretti (non ereditati)
    STentry superEntry;       // entry della superclasse (risolto dalla ST)
    ClassTypeNode type;       // tipo completo con tutti i campi/metodi
}
```

#### `FieldNode`
Campo di una classe.

```java
class FieldNode extends DecNode {
    String id;      // nome del campo
    int offset;     // offset negativo nello heap (-1, -2, ...)
    // tipo ereditato da DecNode
}
```

#### `MethodNode`
Metodo di una classe.

```java
class MethodNode extends DecNode {
    String id;              // nome del metodo
    TypeNode retType;       // tipo di ritorno
    List<ParNode> parList;  // parametri
    List<DecNode> decList;  // dichiarazioni locali
    Node exp;               // corpo
    int offset;             // offset nella dispatch table (0, 1, 2, ...)
    String label;           // label del codice (assegnata durante code gen)
}
```

#### `NewNode`
Creazione di un oggetto.

```java
class NewNode extends Node {
    String id;            // nome della classe
    List<Node> argList;   // inizializzatori dei campi
    STentry entry;        // entry della classe nella symbol table
}
```

#### `ClassCallNode`
Invocazione di un metodo su un oggetto.

```java
class ClassCallNode extends Node {
    String objId;          // nome della variabile oggetto
    String methId;         // nome del metodo
    List<Node> argList;    // argomenti
    STentry entry;         // entry della variabile oggetto
    STentry methodEntry;   // entry del metodo
    int nl;                // nesting level al punto d'uso
}
```

---

## 6. Symbol Table

La symbol table è costruita e gestita da `SymbolTableASTVisitor`, che estende `BaseASTVisitor<Void, VoidException>` e visita l'AST arricchendone i nodi con i riferimenti alle entry.

### 6.1 Struttura degli scope

La symbol table è implementata come **stack di scope**, dove ogni scope è una mappa da nome identificatore a `STentry`:

```java
private final List<Map<String, STentry>> symTable = new ArrayList<>();
```

- `symTable.get(0)` — scope globale (classi, funzioni globali, variabili globali)
- `symTable.get(1)` — primo livello di annidamento (corpo funzione/metodo)
- `symTable.get(k)` — k-esimo livello di annidamento

Per le classi esiste una struttura separata, la **class table**, che mappa ogni nome di classe alla sua *virtual table* (mappa da nome campo/metodo a `STentry`):

```java
private final Map<String, Map<String, STentry>> classTable = new HashMap<>();
```

### 6.2 STentry

La classe `STentry` (in `compiler/STentry.java`) rappresenta l'entry di un identificatore nella symbol table:

```java
public class STentry implements Visitable {
    final int nl;        // nesting level in cui è dichiarato
    final TypeNode type; // tipo dell'identificatore
    final int offset;    // offset in memoria (vedi sezione offset)
}
```

- Per le **variabili** e le **dichiarazioni locali**: `offset` è negativo (`-2`, `-3`, …)
- Per i **parametri**: `offset` è positivo (`+1`, `+2`, …)
- Per i **metodi**: `offset` è l'indice nella dispatch table (`0`, `1`, `2`, …)
- Per le **classi**: `type` è `ClassTypeNode`; `offset` è la posizione nella heap in cui inizia la dispatch table

### 6.3 Gestione degli offset

Il visitor mantiene contatori per gli offset:

```java
private int nestingLevel = 0;    // livello di scope corrente
private int decOffset = -2;       // offset corrente per variabili/dichiarazioni
private int currentFieldOffset;   // offset corrente per campi di classe
private int currentMethodOffset;  // offset corrente per metodi nella dispatch table
```

Gli **offset dei parametri** sono assegnati in ordine positivo crescente (`+1`, `+2`, …).  
Gli **offset delle variabili locali** sono assegnati in ordine negativo decrescente (`-2`, `-3`, …).  
Gli **offset dei campi** di una classe sono negativi (`-1`, `-2`, …) — corrispondono all'indirizzo relativo rispetto al puntatore dell'oggetto nello heap.  
Gli **offset dei metodi** sono non negativi e indicano la posizione nella dispatch table.

### 6.4 Algoritmo di lookup

```java
private STentry stLookup(String id) {
    // Scorre gli scope dal più interno al più esterno
    for (int i = nestingLevel; i >= 0; i--) {
        STentry entry = symTable.get(i).get(id);
        if (entry != null) return entry;
    }
    return null;
}
```

L'algoritmo implementa lo **scoping lessicale statico**: la risoluzione dei nomi parte dallo scope più interno (corrente) e risale fino allo scope globale, restituendo la prima corrispondenza trovata.

### 6.5 SymbolTableASTVisitor

I metodi principali del visitor gestiscono:

- **`visitNode(ProgLetInNode)`** — crea lo scope globale, visita tutte le dichiarazioni e l'espressione principale.
- **`visitNode(FunNode)`** — aggiunge la funzione allo scope corrente con tipo `ArrowTypeNode`; crea un nuovo scope per il corpo; assegna offset positivi ai parametri e negativi alle variabili locali.
- **`visitNode(VarNode)`** — visita l'espressione di inizializzazione *prima* di aggiungere la variabile allo scope (previene auto-riferimento nell'init); assegna offset negativo.
- **`visitNode(ClassNode)`** — costruisce il `ClassTypeNode` completo; se c'è ereditarietà, copia i campi/metodi della superclasse; gestisce l'overriding mantenendo l'offset del metodo padre; aggiorna la class table.
- **`visitNode(MethodNode)`** — simile a `FunNode` ma gestisce la dispatch table; in caso di override riutilizza l'offset del metodo padre, altrimenti assegna il successivo.
- **`visitNode(IdNode)`** — risolve il nome chiamando `stLookup`; salva l'`STentry` e il nesting level corrente nel nodo.
- **`visitNode(CallNode)`** — risolve il nome della funzione; salva `STentry` e nesting level.
- **`visitNode(ClassCallNode)`** — risolve la variabile oggetto; poi cerca il metodo nella class table corrispondente; verifica che l'offset sia ≥ 0 (altrimenti è un campo, non un metodo).

---

## 7. Sistema di tipi

Il sistema di tipi è basato su una gerarchia di classi che estendono `TypeNode` (in `compiler/lib/TypeNode.java`). `TypeNode` estende `Node`, quindi tutti i tipi sono visitabili.

### Gerarchia dei tipi

```
TypeNode
  ├── IntTypeNode      — tipo int
  ├── BoolTypeNode     — tipo bool
  ├── ArrowTypeNode    — tipo funzione (T1,...,Tn) → R
  ├── RefTypeNode      — riferimento a classe: id (nome)
  ├── ClassTypeNode    — tipo completo di classe
  └── EmptyTypeNode    — tipo di null
```

### Descrizione dei tipi

#### `IntTypeNode`
Tipo primitivo intero. Non ha campi aggiuntivi.

#### `BoolTypeNode`
Tipo primitivo booleano. In FOOL, `bool` è **sottotipo** di `int` (i valori booleani possono essere usati dove è atteso un intero: `true` → 1, `false` → 0).

#### `ArrowTypeNode`
Tipo funzione.

```java
class ArrowTypeNode extends TypeNode {
    List<TypeNode> parList;  // tipi dei parametri
    TypeNode retType;        // tipo di ritorno
}
```

#### `RefTypeNode`
Tipo riferimento a classe.

```java
class RefTypeNode extends TypeNode {
    String id;  // nome della classe
}
```

#### `ClassTypeNode`
Tipo completo di una classe, che include *tutti* i campi e i metodi (inclusi quelli ereditati). Viene costruito dal `SymbolTableASTVisitor` durante la visita di `ClassNode`.

```java
class ClassTypeNode extends TypeNode {
    List<TypeNode> allFields;           // tutti i tipi dei campi (ordine: prima ereditati, poi propri)
    List<ArrowTypeNode> allMethods;     // tutti i tipi dei metodi (posizione = offset dispatch table)
}
```

Il `ClassTypeNode` è usato:
- Come tipo dell'`STentry` associata al nome della classe nello scope globale.
- Per la verifica del type checking durante l'override e l'istanziazione.

#### `EmptyTypeNode`
Tipo del valore `null`. È compatibile con qualsiasi `RefTypeNode` tramite la regola di sottotipo.

---

## 8. Sottotipaggio

Il sistema di sottotipaggio è implementato nella classe `TypeRels` (`compiler/TypeRels.java`).

### 8.1 Regole di sottotipo

La mappa `superType` traccia la gerarchia di ereditarietà:

```java
public static Map<String, String> superType = new HashMap<>();
// superType.get("Cane") = "Animale"  →  class Cane extends Animale
```

Il metodo `isSubtype(TypeNode a, TypeNode b): boolean` verifica se `a <: b`:

```
1. Tipi riferimento (RefTypeNode):
   a <: b  sse  a.id == b.id
             OR  risalendo la catena superType da a.id si incontra b.id

2. Tipi funzione (ArrowTypeNode):
   (A1,...,An)→R1  <:  (B1,...,Bn)→R2
     sse  R1 <: R2  (covarianza sul ritorno)
      AND  ∀i: Bi <: Ai  (controvarianza sui parametri)

3. Stesso nodo concreto:
   T <: T  (riflessività)

4. BoolTypeNode <: IntTypeNode

5. EmptyTypeNode <: EmptyTypeNode
   EmptyTypeNode <: RefTypeNode  (null compatibile con tutti i tipi classe)
```

### 8.2 Covarianza e controvarianza

Il sottotipaggio dei tipi funzione segue le regole standard:

- **Covarianza sul tipo di ritorno**: se un metodo ritorna `Cane`, può essere usato dove è atteso un metodo che ritorna `Animale`, poiché `Cane <: Animale`.
- **Controvarianza sui parametri**: se un metodo accetta `Animale`, può essere usato dove è atteso un metodo che accetta `Cane`, poiché il chiamante garantisce almeno un `Cane` (che è un `Animale`).

```
Esempio:
  funA : (Cane) → Animale
  funB : (Animale) → Cane

  funB <: funA
  perché: Cane <: Animale (covarianza ritorno) AND Animale <: Animale? No.
  
  In realtà:
  funB = (Animale) → Cane
  funA = (Cane) → Animale
  
  funB <: funA sse Cane <: Animale AND Cane <: Animale → TRUE
```

### 8.3 Lowest Common Ancestor

`lowestCommonAncestor(TypeNode a, TypeNode b): TypeNode` trova il tipo più specifico che è supertipo sia di `a` che di `b`. Viene usato per determinare il tipo di un'espressione `if-then-else`.

```
Regole:
1. Se a è EmptyTypeNode → ritorna b
2. Se b è EmptyTypeNode → ritorna a
3. Se entrambi RefTypeNode:
   - risale la gerarchia di a finché trova un antenato che sia anche supertipo di b
4. Se entrambi primitivi (int/bool):
   - LCA(bool, bool) = bool
   - LCA(bool, int) = int
   - LCA(int, int) = int
5. Altrimenti → null (tipi incompatibili)
```

### 8.4 Gestione di null

Il valore `null` ha tipo `EmptyTypeNode`. Le regole di sottotipaggio garantiscono che:

- `null` può essere assegnato a qualsiasi variabile di tipo `RefTypeNode`.
- In un `if-then-else`, se un ramo è `null` e l'altro è `RefTypeNode(C)`, il tipo dell'intera espressione è `RefTypeNode(C)`.

---

## 9. Type Checking

Il type checking è implementato in `TypeCheckEASTVisitor` (`compiler/TypeCheckEASTVisitor.java`), che estende `BaseEASTVisitor<TypeNode, TypeException>`. Il visitor visita l'EAST e ritorna il `TypeNode` dell'espressione verificata.

### 9.1 Dichiarazioni di variabili e funzioni

**`VarNode`:**
1. Visita l'espressione di inizializzazione → ottiene `exprType`.
2. Verifica: `exprType <: declaredType` (tramite `TypeRels.isSubtype`).
3. Se la verifica fallisce, lancia `TypeException`.

**`FunNode`:**
1. Visita le dichiarazioni locali (raccogliendo gli errori senza interrompere).
2. Visita il corpo → ottiene `bodyType`.
3. Verifica: `bodyType <: retType`.

**`MethodNode`:**  
Identico a `FunNode`, ma opera nel contesto di una classe.

**`IfNode`:**
1. Visita la condizione → verifica che sia `<: BoolTypeNode`.
2. Visita il ramo `then` → `thenType`.
3. Visita il ramo `else` → `elseType`.
4. Ritorna `lowestCommonAncestor(thenType, elseType)`.
5. Se l'LCA è `null`, lancia `TypeException` (tipi incompatibili tra i rami).

### 9.2 Operatori

| Operatore | Verifica | Tipo ritornato |
|---|---|---|
| `+`, `-`, `*`, `/` | entrambi gli operandi `<: int` | `IntTypeNode` |
| `==` | i due operandi sono comparabili (uno `<:` dell'altro) | `BoolTypeNode` |
| `>=`, `<=` | i due operandi sono comparabili | `BoolTypeNode` |
| `&&`, `||` | entrambi `<: bool` | `BoolTypeNode` |
| `!` | operando `<: bool` | `BoolTypeNode` |

### 9.3 Chiamate di funzione e metodo

**`CallNode`:**
1. Se `entry == null` → lancia `IncomplException` (simbolo non risolto).
2. Ottiene il tipo tramite `visitSTentry(entry)` → deve essere `ArrowTypeNode`.
3. Verifica che il numero di argomenti corrisponda al numero di parametri.
4. Per ogni argomento `i`: verifica `argType[i] <: parType[i]`.
5. Ritorna `retType` della funzione.

**`ClassCallNode`:**
1. Verifica che l'entry dell'oggetto e del metodo non siano null.
2. Ottiene il tipo del metodo → deve essere `ArrowTypeNode`.
3. Verifica argomenti come per `CallNode`.
4. Ritorna `retType` del metodo.

**`IdNode`:**
1. Ottiene il tipo dall'entry.
2. Verifica che **non** sia `ArrowTypeNode` (non si può usare una funzione come valore senza chiamarla).
3. Verifica che **non** sia `ClassTypeNode` (non si può usare il nome di una classe come valore).
4. Ritorna il tipo.

### 9.4 Creazione oggetti ed ereditarietà

**`NewNode`:**
1. Verifica che l'entry non sia null.
2. Verifica che `entry.type` sia `ClassTypeNode`.
3. Verifica che il numero di argomenti corrisponda al numero di campi (`allFields.size()`).
4. Per ogni argomento `i`: verifica `argType[i] <: allFields.get(i)`.
5. Ritorna `RefTypeNode(className)`.

**`ClassNode`:**
1. Visita tutti i metodi (raccogliendo errori).
2. Se ha superclasse:
   - Aggiunge la coppia a `TypeRels.superType`.
   - Per ogni campo ereditato sovrapposto: verifica che il tipo del campo nella sottoclasse sia `<:` del tipo nella superclasse.
   - Per ogni metodo in override: verifica che la firma nella sottoclasse sia `<:` della firma nella superclasse.

---

## 10. Estensione Object-Oriented

### 10.1 Dichiarazione di classi

Quando `SymbolTableASTVisitor` visita un `ClassNode`:

1. Crea un `ClassTypeNode` per la classe.
2. Se la classe estende un'altra:
   - Recupera l'`STentry` della superclasse.
   - Copia i campi e i metodi dalla superclasse nel nuovo `ClassTypeNode` (in `allFields` e `allMethods`).
   - Inizializza `currentFieldOffset` come continuazione degli offset della superclasse.
   - Inizializza `currentMethodOffset` come continuazione degli indici della superclasse.
3. Aggiunge i nuovi campi (con offset negativi) e i nuovi metodi (con offset di dispatch).
4. Aggiunge la classe alla symbol table globale con tipo `ClassTypeNode`.
5. Aggiorna la class table con la virtual table della classe.

### 10.2 Ereditarietà e overriding

L'overriding dei metodi è gestito durante la visita di `MethodNode` all'interno di una classe:

1. Il visitor cerca il metodo nella class table della superclasse.
2. Se trovato → **override**: il nuovo metodo assume lo **stesso offset** del metodo padre nella dispatch table.
3. Se non trovato → **nuovo metodo**: viene assegnato l'offset `currentMethodOffset++`.

L'overriding di campi usa la stessa logica: se un campo esiste già nella superclasse, il suo offset viene **riutilizzato**.

**Importante**: il type checker verifica che i tipi dell'override siano compatibili: la firma del metodo in override deve essere sottotipo della firma del metodo padre.

### 10.3 Layout dei campi e dei metodi

I campi di un oggetto sono memorizzati nello **heap** con offset negativi rispetto al puntatore all'oggetto:

```
Puntatore oggetto op:
  memory[op]     = puntatore alla dispatch table
  memory[op - 1] = campo 1
  memory[op - 2] = campo 2
  ...
```

I metodi sono memorizzati nella **dispatch table** con offset non negativi:

```
Puntatore dispatch table dp:
  memory[dp]     = label metodo 0
  memory[dp + 1] = label metodo 1
  ...
```

### 10.4 Dispatch table

La `CodeGenerationASTVisitor` mantiene una lista di dispatch table:

```java
private final List<List<String>> dispatchTables = new ArrayList<>();
```

Ogni elemento è una lista di label (una per metodo). La lista di dispatch table cresce con ogni `ClassNode` visitato.

Durante la visita di `ClassNode`:
1. Si crea una nuova dispatch table (copiando quella della superclasse se presente).
2. Per ogni metodo, si aggiunge la sua label nella posizione corretta.
3. Si materializza la dispatch table nello heap emettendo istruzioni `push label / lhp / sw / lhp / push 1 / add / shp` per ogni entry.
4. L'indirizzo base della dispatch table nello heap viene salvato: è la posizione `MEMSIZE + offset_classe` della memoria, dove `offset_classe` è l'offset dell'`STentry` della classe (negativo, usato come indice relativo a MEMSIZE).

---

## 11. Code Generation

Il code generator è implementato in `CodeGenerationASTVisitor` (`compiler/CodeGenerationASTVisitor.java`), che estende `BaseASTVisitor<String, VoidException>`. Ogni `visitNode` ritorna una `String` contenente le istruzioni SVM assembly per il nodo visitato.

Le istruzioni sono concatenate con `nlJoin(...)` (utility in `FOOLlib`), che unisce le stringhe non-null con newline.

### 11.1 Struttura del code generator

La classe `FOOLlib` fornisce utilità globali:

```java
String freshFunLabel()  // genera label univoca "function0", "function1", ...
String freshLabel()     // genera label univoca "label0", "label1", ...
void putCode(String c)  // accumula codice funzioni/metodi in un buffer
String getCode()        // restituisce tutto il codice accumulato
```

Il codice delle funzioni e dei metodi **non** viene emesso inline, ma accumulato con `putCode` e aggiunto in coda al programma principale. Questo permette di separare il codice del programma principale dal codice delle funzioni.

**Struttura del codice generato per `ProgLetInNode`:**

```asm
push 0               ; inizializza il frame globale
<codice dichiarazioni>
<codice espressione principale>
halt
<codice funzioni e metodi (da putCode)>
```

### 11.2 Funzioni e metodi

Per `FunNode`, il code generator:
1. Genera una label univoca (`freshFunLabel()`).
2. Emette il codice della funzione nel buffer `putCode`:

```asm
functionN:
  cfp          ; fp = sp (crea nuovo frame)
  lra          ; push return address sullo stack
  <codice dichiarazioni locali>
  <codice corpo>
  stm          ; salva valore di ritorno in tm
  <pop per ogni dichiarazione locale>
  sra          ; pop → return address
  pop          ; rimuove access link
  <pop per ogni parametro>
  sfp          ; ripristina fp (control link)
  ltm          ; ricarica valore di ritorno
  lra          ; ricarica return address
  js           ; salta a return address
```

3. Ritorna `push functionN` (il puntatore alla funzione viene lasciato sullo stack).

`MethodNode` ha lo stesso comportamento di `FunNode`, ma ritorna `null` (la label viene memorizzata nella dispatch table, non sullo stack).

### 11.3 Chiamate di funzione

Per `CallNode`, si distinguono due casi:

**Caso 1: chiamata di funzione normale** (`entry.offset < 0`)

```asm
lfp                    ; control link (frame pointer corrente)
<eval argomenti da destra a sinistra>
lfp
<catena degli access link: lw ripetuto (nl - entry.nl) volte>
stm                    ; salva access link in tm
ltm                    ; duplica (access link sullo stack)
ltm
push entry.offset
add
lw                     ; carica puntatore funzione
js                     ; salta alla funzione
```

**Caso 2: chiamata di metodo nell'ambito di una classe** (`entry.offset >= 0`)

```asm
lfp                    ; control link
<eval argomenti>
lfp
<catena degli access link>
stm
ltm                    ; access link (= puntatore all'oggetto)
ltm
lw                     ; carica puntatore dispatch table
push entry.offset
add
lw                     ; carica label del metodo
js
```

### 11.4 Chiamate di metodo

Per `ClassCallNode`:

```asm
<eval argomenti>
lfp
<catena degli access link fino alla variabile oggetto>
push entry.offset      ; offset della variabile oggetto nel frame
add
lw                     ; carica puntatore oggetto
stm                    ; salva in tm
ltm                    ; access link (= puntatore oggetto)
ltm
lw                     ; carica puntatore dispatch table dall'oggetto
push methodEntry.offset
add
lw                     ; carica label del metodo
js
```

### 11.5 Oggetti e dispatch

**`NewNode`** — creazione oggetto:

```asm
<eval field1>
lhp / sw / lhp / push 1 / add / shp   ; scrivi field1 nello heap, avanza hp
<eval field2>
lhp / sw / lhp / push 1 / add / shp   ; scrivi field2 nello heap, avanza hp
...
push MEMSIZE
push entry.offset      ; offset della classe (negativo)
add
lw                     ; carica puntatore alla dispatch table
lhp                    ; push hp (sarà il puntatore all'oggetto)
sw                     ; scrivi dispatch pointer nell'oggetto
lhp                    ; l'indirizzo di inizio oggetto (dispatch pointer)
lhp
push 1
add
shp                    ; avanza hp
```

**`IdNode`** — caricamento di variabile con catena degli access link:

```asm
lfp
lw (ripetuto nl - entry.nl volte)   ; segui la catena degli access link
push entry.offset
add
lw                                  ; carica il valore
```

**`EmptyNode`** (null):

```asm
push -1
```

### 11.6 Operatori e controllo di flusso

**Operatori aritmetici** (`PlusNode`, `MinusNode`, `TimesNode`, `DivNode`):

```asm
<eval left>
<eval right>
add / sub / mult / div
```

**`EqualNode`:**

```asm
<eval left>
<eval right>
beq label1
push 0
b label2
label1:
push 1
label2:
```

**`LessEqualNode`:**

```asm
<eval left>
<eval right>
bleq label1
push 0
b label2
label1:
push 1
label2:
```

**`GreaterEqualNode`** (invertendo gli operandi e usando `bleq`):

```asm
<eval right>
<eval left>
bleq label1    ; left <= right equivale a right >= left
push 0
b label2
label1:
push 1
label2:
```

**`IfNode`:**

```asm
<eval condizione>
push 1
beq label1     ; se cond==1 (true), vai al ramo then
<eval ramo else>
b label2
label1:
<eval ramo then>
label2:
```

**`AndNode`** (con short-circuit):

```asm
<eval primo operando>
push 0
beq falseLabel    ; se primo è false, risultato è false
<eval secondo operando>
push 0
beq falseLabel
push 1
b doneLabel
falseLabel:
push 0
doneLabel:
```

**`OrNode`** (con short-circuit):

```asm
<eval primo operando>
push 0
beq evalSecond   ; se primo è false, valuta il secondo
b trueLabel      ; altrimenti è true
evalSecond:
<eval secondo operando>
push 0
beq falseLabel
trueLabel:
push 1
b doneLabel
falseLabel:
push 0
doneLabel:
```

**`NotNode`:**

```asm
<eval exp>
push 0
beq trueLabel
push 0
b doneLabel
trueLabel:
push 1
doneLabel:
```

---

## 12. Modello di runtime

Il modello di esecuzione si basa su una **macchina a stack** con memoria condivisa tra stack e heap.

### Stack

Lo stack cresce **dall'alto verso il basso** (da `MEMSIZE` verso `0`). Contiene:
- Il valore iniziale `0` per il frame globale (creato all'avvio)
- Le variabili globali (dichiarazioni di primo livello)
- Gli **activation record** per ogni chiamata di funzione o metodo

### Heap

Lo heap cresce **dal basso verso l'alto** (da `0` verso `MEMSIZE`). Contiene:
- Le **dispatch table** delle classi (allocate all'inizio, durante la visita dei `ClassNode`)
- Gli **oggetti** allocati con `new`

### Activation Record

Ogni chiamata di funzione/metodo crea un **activation record** sullo stack con la seguente struttura (all'indirizzo `fp`):

| Contenuto | Offset rispetto a fp |
|---|---|
| Parametro n | `+n` |
| ... | ... |
| Parametro 1 | `+1` |
| Access link (puntatore al frame padre) | `0` (fp punta qui) |
| Return address | `fp - 1` (salvato da `lra`) |
| Var locale 1 | `fp - 2` |
| Var locale 2 | `fp - 3` |
| ... | ... |

Il **control link** (puntatore al frame del chiamante) è il primo valore spinto dal chiamante ed è salvato da `sfp` al ritorno.

### Oggetti

Un oggetto nello heap è strutturato così (indirizzo base = puntatore all'oggetto `op`):

| Contenuto | Indirizzo |
|---|---|
| Puntatore alla dispatch table | `op` |
| Campo 1 | `op - 1` |
| Campo 2 | `op - 2` |
| ... | ... |

### Dispatch table

La dispatch table di una classe è memorizzata nella zona bassa della memoria (heap). La sua posizione è calcolata come `MEMSIZE + entry.offset` dove `entry.offset` è l'offset della classe nella symbol table.

---

## 13. Layout della memoria

### 13.1 Activation Record

```
Indirizzi più alti (MEMSIZE)
  ┌─────────────────────────────────┐
  │  ...                            │
  │  Parametro 2       (fp+2)       │
  │  Parametro 1       (fp+1)       │
  ├─────────────────────────────────┤ ← fp (Frame Pointer)
  │  Access Link       (fp+0)       │  punta al frame del padre lessicale
  ├─────────────────────────────────┤
  │  Return Address    (fp-1)       │  salvato con lra/sra
  ├─────────────────────────────────┤
  │  Var locale 1      (fp-2)       │
  │  Var locale 2      (fp-3)       │
  │  ...                            │
  ├─────────────────────────────────┤ ← sp (Stack Pointer)
  │  (area libera)                  │
  │  ...                            │
  │  Oggetti (heap)                 │
  │  Dispatch tables (heap)         │
  └─────────────────────────────────┘
Indirizzi più bassi (0)
```

### 13.2 Oggetto nello heap

```
hp → (prossima cella libera)
  ┌─────────────────────────────────┐
  │  (vuoto)                        │  ← hp punta qui dopo allocazione
  ├─────────────────────────────────┤
  │  Dispatch Pointer  (op+0)       │  ← op (puntatore oggetto ritornato da new)
  ├─────────────────────────────────┤
  │  Campo 1           (op-1)       │
  ├─────────────────────────────────┤
  │  Campo 2           (op-2)       │
  ├─────────────────────────────────┤
  │  ...                            │
  └─────────────────────────────────┘
```

I campi vengono scritti nello heap in ordine crescente *prima* del dispatch pointer. Il dispatch pointer è l'ultimo elemento scritto e punta alla dispatch table della classe.

### 13.3 Dispatch table

```
MEMSIZE + entry.offset (classe C)
  ┌─────────────────────────────────┐
  │  Label metodo 0    (dp+0)       │  indirizzo codice del metodo 0
  ├─────────────────────────────────┤
  │  Label metodo 1    (dp+1)       │  indirizzo codice del metodo 1
  ├─────────────────────────────────┤
  │  ...                            │
  └─────────────────────────────────┘
  ↑ dp = puntatore alla dispatch table (salvato nel campo op+0 dell'oggetto)
```

---

## 14. Stack Virtual Machine

La SVM è implementata in `svm/ExecuteVM.java`. Interpreta direttamente il codice assembly generato dal compilatore, caricato come array di interi dal parser SVM (`SVMParser` su `SVM.g4`).

### 14.1 Registri

| Registro | Campo Java | Descrizione |
|---|---|---|
| `ip` | `int ip = 0` | Instruction Pointer: indice della prossima istruzione in `code[]` |
| `sp` | `int sp = MEMSIZE` | Stack Pointer: prossima cella libera (stack cresce verso il basso) |
| `hp` | `int hp = 0` | Heap Pointer: prossima cella libera nello heap (cresce verso l'alto) |
| `fp` | `int fp = MEMSIZE` | Frame Pointer: base dell'activation record corrente |
| `ra` | `int ra` | Return Address: usato da `js` / `lra` / `sra` |
| `tm` | `int tm` | Temporary: registro ausiliario per il code generator |

Costanti:

```java
public static final int CODESIZE = 10000;   // dimensione massima del programma
public static final int MEMSIZE  = 10000;   // dimensione della memoria dati
```

### 14.2 Set di istruzioni

| Istruzione | Mnemonica SVM | Effetto sullo stack | Operazione |
|---|---|---|---|
| Push costante | `push n` | `→ n` | Spinge il valore `n` sullo stack |
| Push label | `push label` | `→ addr` | Spinge l'indirizzo della label (risolto dall'assembler) |
| Pop | `pop` | `v →` | Scarta il top dello stack |
| Addizione | `add` | `v2 v1 → v2+v1` | Somma i due top |
| Sottrazione | `sub` | `v2 v1 → v2-v1` | Sottrae (ordine: v2-v1) |
| Moltiplicazione | `mult` | `v2 v1 → v2*v1` | Moltiplica |
| Divisione | `div` | `v2 v1 → v2/v1` | Divide (ordine: v2/v1) |
| Store word | `sw` | `addr v →` | `memory[addr] = v` |
| Load word | `lw` | `addr → memory[addr]` | Carica da memoria |
| Branch | `b label` | (nessuno) | Salto incondizionato |
| Branch Equal | `beq label` | `v2 v1 →` | Salta se `v2 == v1` |
| Branch ≤ | `bleq label` | `v2 v1 →` | Salta se `v2 <= v1` |
| Jump to subroutine | `js` | `addr →` | `ra = ip; ip = addr` |
| Load RA | `lra` | `→ ra` | Spinge il return address |
| Store RA | `sra` | `ra →` | Imposta `ra = pop()` |
| Load TM | `ltm` | `→ tm` | Spinge il registro temporaneo |
| Store TM | `stm` | `tm →` | Imposta `tm = pop()` |
| Load FP | `lfp` | `→ fp` | Spinge il frame pointer |
| Store FP | `sfp` | `fp →` | Imposta `fp = pop()` |
| Copy FP | `cfp` | (nessuno) | `fp = sp` (crea nuovo frame) |
| Load HP | `lhp` | `→ hp` | Spinge il heap pointer |
| Store HP | `shp` | `hp →` | Imposta `hp = pop()` |
| Print | `print` | (side effect) | Stampa il top senza rimuoverlo |
| Halt | `halt` | — | Termina l'esecuzione |

### 14.3 Ciclo fetch-decode-execute

```java
public void cpu() {
    while (true) {
        int bytecode = code[ip++];   // FETCH
        switch (bytecode) {           // DECODE & EXECUTE
            case PUSH:   push(code[ip++]); break;
            case ADD:    v1=pop(); v2=pop(); push(v2+v1); break;
            case SUB:    v1=pop(); v2=pop(); push(v2-v1); break;
            case MULT:   v1=pop(); v2=pop(); push(v2*v1); break;
            case DIV:    v1=pop(); v2=pop(); push(v2/v1); break;
            case STOREW: address=pop(); memory[address]=pop(); break;
            case LOADW:  push(memory[pop()]); break;
            case JS:     address=pop(); ra=ip; ip=address; break;
            // ... ecc.
            case HALT:   return;
        }
    }
}
```

Le operazioni di stack sono:

```java
private void push(int v) { memory[--sp] = v; }
private int  pop()       { return memory[sp++]; }
```

---

## 15. Esecuzione del programma

### 15.1 Chiamata di funzione

La sequenza di operazioni per chiamare `f(a, b)`:

**Lato chiamante:**
1. `lfp` — spinge il frame pointer corrente (control link)
2. `<eval b>` — valuta e spinge l'argomento 2
3. `<eval a>` — valuta e spinge l'argomento 1
4. `<catena access link>` — carica il frame lessicale corretto (access link)
5. `stm` / `ltm` / `ltm` — duplica l'access link sullo stack
6. `push offset` / `add` / `lw` — carica il puntatore alla funzione
7. `js` — salta alla funzione, salvando `ip` in `ra`

**Corpo della funzione:**
1. `cfp` — `fp = sp` (nuovo frame)
2. `lra` — spinge `ra` sullo stack (salva return address come variabile locale)
3. `<dichiarazioni locali>` — ogni dichiarazione spinge il suo valore
4. `<corpo>` — valuta il corpo, risultato rimane sullo stack
5. `stm` — salva il risultato in `tm`
6. `<pop per ogni dichiarazione locale>` — libera lo stack
7. `sra` — recupera il return address dallo stack in `ra`
8. `pop` — rimuove l'access link
9. `<pop per ogni parametro>`
10. `sfp` — ripristina il frame pointer del chiamante (control link)
11. `ltm` — rimette il risultato sullo stack
12. `lra` — rimette l'indirizzo di ritorno sullo stack
13. `js` — torna al chiamante

### 15.2 Chiamata di metodo e dynamic dispatch

La chiamata di metodo `obj.metodo(a)` utilizza il **dynamic dispatch**:

1. Si valutano gli argomenti e si carica il puntatore all'oggetto (`obj`).
2. Il puntatore all'oggetto viene usato sia come **access link** (il metodo può accedere ai campi tramite di esso) sia come base per il dispatch.
3. Da `obj` si legge `memory[obj]` → puntatore alla dispatch table della classe reale dell'oggetto.
4. Alla posizione `dispatch_table + methodOffset` si trova l'indirizzo del codice del metodo.
5. Si salta con `js`.

Questo garantisce il polimorfismo: anche se la variabile è dichiarata di tipo `Animale`, se contiene un `Cane`, il metodo `verso()` di `Cane` viene invocato.

```
var a : Animale = new Cane(10);
a.verso()

Runtime:
  a punta a un oggetto Cane nello heap
  memory[a] = puntatore alla dispatch table di Cane
  dispatch_table_cane[0] = address di Cane.verso
  → viene eseguito Cane.verso()
```

### 15.3 Allocazione di oggetti

La creazione di un oggetto con `new C(v1, v2)`:

1. I valori dei campi (`v1`, `v2`) vengono scritti nello heap nell'ordine di dichiarazione:
   ```
   memory[hp] = v1; hp++
   memory[hp] = v2; hp++
   ```
2. Il puntatore alla dispatch table di `C` viene caricato da `MEMSIZE + offset_C`.
3. Il dispatch pointer viene scritto nello heap:
   ```
   memory[hp] = dispatch_pointer_C; hp++
   ```
4. L'indirizzo del dispatch pointer (che è l'indirizzo base dell'oggetto) viene lasciato sullo stack.

Nota: i campi sono scritti **prima** del dispatch pointer. Quindi il dispatch pointer è all'indirizzo più alto occupato dall'oggetto; i campi sono agli indirizzi precedenti (offset −1, −2, …).

---

## 16. Visitor Pattern

Il compilatore usa il **Visitor pattern** per separare le operazioni di attraversamento dell'AST dall'implementazione dei singoli nodi. Ogni fase (symbol table, type check, code gen) è un visitor distinto.

### 16.1 BaseASTVisitor

`compiler/lib/BaseASTVisitor.java` è la classe astratta base per tutti i visitor:

```java
public class BaseASTVisitor<S, E extends Exception> {
    private boolean incomplExc;  // abilita IncomplException per null
    protected boolean print;     // abilita stampa per debug
    protected String indent;     // indentazione per la stampa

    public S visit(Visitable v) throws E { ... }

    // Un metodo visitNode per ogni tipo di nodo AST:
    public S visitNode(ProgLetInNode n) throws E { throw new UnimplException(); }
    public S visitNode(ProgNode n)      throws E { throw new UnimplException(); }
    public S visitNode(FunNode n)       throws E { throw new UnimplException(); }
    // ... tutti i nodi base, operator extension e OO extension
}
```

I parametri generici:
- `S` — tipo di ritorno dei `visitNode` (es. `TypeNode`, `String`, `Void`)
- `E` — tipo di eccezione lanciabile (es. `TypeException`, `VoidException`)

### 16.2 BaseEASTVisitor

`compiler/lib/BaseEASTVisitor.java` estende `BaseASTVisitor` aggiungendo il supporto per le entry della symbol table:

```java
public abstract class BaseEASTVisitor<S, E extends Exception>
        extends BaseASTVisitor<S, E> {

    public S visitSTentry(STentry s) throws E { throw new UnimplException(); }
}
```

Questa estensione è usata da `TypeCheckEASTVisitor` (che deve visitare le `STentry` per determinare il tipo degli identificatori) e da `PrintEASTVisitor`.

### 16.3 Double dispatch

Il Visitor pattern usa il **double dispatch** per selezionare il metodo corretto a runtime. Ogni nodo AST implementa l'interfaccia `Visitable`:

```java
public interface Visitable {
    <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E;
}
```

Ogni nodo implementa `accept` chiamando il metodo `visitNode` specifico del visitor:

```java
class ProgNode extends Node {
    @Override
    public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
        return visitor.visitNode(this);
    }
}
```

**Flusso del double dispatch:**

```
visitor.visit(progNode)
  → progNode.accept(visitor)          // 1° dispatch: tipo di progNode
    → visitor.visitNode(progNode)     // 2° dispatch: tipo del visitor
```

Il primo dispatch seleziona il metodo `accept` del nodo (determinato staticamente — tutti chiamano `visitor.visitNode(this)`). Il secondo dispatch seleziona il `visitNode` del visitor corretto (determinato dal tipo runtime del visitor). Questo permette di aggiungere nuove operazioni sull'AST creando un nuovo visitor, senza modificare le classi dei nodi.

### 16.4 Visitor implementati

| Visitor | Estende | Ritorna | Eccezione | Ruolo |
|---|---|---|---|---|
| `ASTGenerationSTVisitor` | `FOOLBaseVisitor<Node>` (ANTLR) | `Node` | `Exception` | Costruisce l'AST dal parse tree |
| `SymbolTableASTVisitor` | `BaseASTVisitor<Void, VoidException>` | `Void` | `VoidException` | Costruisce la symbol table, arricchisce l'AST |
| `TypeCheckEASTVisitor` | `BaseEASTVisitor<TypeNode, TypeException>` | `TypeNode` | `TypeException` | Verifica i tipi, ritorna il tipo dell'espressione |
| `CodeGenerationASTVisitor` | `BaseASTVisitor<String, VoidException>` | `String` | `VoidException` | Genera il codice SVM assembly |
| `PrintEASTVisitor` | `BaseEASTVisitor<Void, VoidException>` | `Void` | `VoidException` | Stampa l'AST arricchito per debug |

**`SymbolTableASTVisitor`** — visita l'AST mutando i nodi (aggiunge `STentry` a `IdNode`, `CallNode`, ecc.) e costruisce le strutture `symTable` e `classTable`. Non ha valore di ritorno significativo.

**`TypeCheckEASTVisitor`** — visita l'EAST e ritorna il `TypeNode` dell'espressione. Usa `TypeRels.isSubtype` e `TypeRels.lowestCommonAncestor` per le verifiche. Lancia `TypeException` per errori di tipo e `IncomplException` per simboli non risolti.

**`CodeGenerationASTVisitor`** — visita l'EAST e ritorna la `String` con il codice assembly. Usa `FOOLlib.freshLabel()`, `freshFunLabel()`, `putCode()` per gestire label e codice delle funzioni.

---

*Documentazione generata a partire dal codice sorgente del progetto FOOL Compiler.*
