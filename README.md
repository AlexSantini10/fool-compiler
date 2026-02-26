# FOOL Compiler

A compiler for **FOOL** (Functional Object-Oriented Language), developed for the course *Linguaggi Compilatori e Modelli Computazionali* — MSc in Computer Science and Engineering, University of Bologna (UNIBO).

## Authors

- [Alex Santini](https://github.com/AlexSantini10)
- [Roberto Pisu](https://github.com/robertop03)
- [Alessandro Torelli](https://github.com/aleToro7)

## About the Language

FOOL is a small functional and object-oriented language supporting:

- **Primitive types**: `int`, `bool`
- **Variables** and **functions** with local declarations (`let ... in`)
- **Classes** with single inheritance (`class ... extends ...`)
- **Methods** and **method dispatch** via dot notation (`obj.method(...)`)
- **Object instantiation** (`new ClassName(...)`)
- **Null** reference and null-equality checks
- **Control flow**: `if ... then { ... } else { ... }`
- **Arithmetic and logical operators**: `+`, `-`, `*`, `/`, `&&`, `||`, `!`
- **Comparison operators**: `==`, `>=`, `<=`
- **Print** statement

## Project Structure

```
.
├── compiler/           # Front-end and code generation
│   ├── FOOL.g4         # ANTLR4 grammar for FOOL
│   ├── AST.java        # Abstract Syntax Tree node definitions
│   ├── ASTGenerationSTVisitor.java    # ST → AST visitor
│   ├── SymbolTableASTVisitor.java     # Symbol table construction
│   ├── TypeCheckEASTVisitor.java      # Type checker
│   ├── PrintEASTVisitor.java          # AST pretty-printer
│   ├── CodeGenerationASTVisitor.java  # Code generator (SVM assembly)
│   ├── TypeRels.java   # Type relation helpers (subtyping)
│   ├── STentry.java    # Symbol table entry
│   ├── Test.java       # Main entry point
│   ├── exc/            # Compiler exceptions
│   └── lib/            # Base visitor classes and utilities
├── svm/                # Stack Virtual Machine
│   ├── SVM.g4          # ANTLR4 grammar for SVM assembly
│   └── ExecuteVM.java  # SVM interpreter
├── lib/
│   └── antlr-4.13.1-complete.jar   # ANTLR4 runtime
└── prova.fool          # Example FOOL program
```

## Compilation Pipeline

1. **Lexing & Parsing** — ANTLR4 tokenises and parses the source file into a parse tree using `FOOL.g4`.
2. **AST Generation** — `ASTGenerationSTVisitor` converts the parse tree into an AST.
3. **Symbol Table Analysis** — `SymbolTableASTVisitor` resolves identifiers and builds the enriched AST (EAST).
4. **Type Checking** — `TypeCheckEASTVisitor` verifies type correctness, supporting subtyping and OOP dispatch rules.
5. **Code Generation** — `CodeGenerationASTVisitor` emits assembly code for the Stack Virtual Machine.
6. **Assembly & Execution** — The SVM assembles and executes the generated `.asm` file.

## Requirements

- Java 11 or higher
- ANTLR 4.13.1 (included in `lib/antlr-4.13.1-complete.jar`)

## How to Run

### 1. Compile the project

```bash
javac -cp lib/antlr-4.13.1-complete.jar compiler/*.java compiler/lib/*.java compiler/exc/*.java svm/*.java
```

### 2. Run the compiler on a `.fool` file

Place your source file (e.g. `prova.fool`) in the project root, then run:

```bash
java -cp .:lib/antlr-4.13.1-complete.jar compiler.Test
```

On Windows, replace `:` with `;` in the classpath:

```bash
java -cp ".;lib/antlr-4.13.1-complete.jar" compiler.Test
```

The compiler will:
- Report any lexical, syntax, symbol-table, and type errors.
- If the front-end is error-free, emit `prova.fool.asm` and immediately run it on the SVM.

## Example

The included `prova.fool` demonstrates class inheritance and method overriding with a `BankLoan` / `MyBankLoan` hierarchy:

```fool
let

  class Account (money:int) {
    fun getMon:int () money;
  }

  class TradingAcc extends Account (invested:int) {
    fun getInv:int () invested;
  }

  ...

  var bl:BankLoan = new MyBankLoan(new TradingAcc(50000,40000));
  var myLoan:Account = bl.openLoan(myTradingAcc);

in print(if (myLoan==null) then {0} else {myLoan.getMon()});
```
