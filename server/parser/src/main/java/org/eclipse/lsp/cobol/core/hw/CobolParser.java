/*
 * Copyright (c) 2024 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *    DAF Trucks NV – implementation of DaCo COBOL statements
 *    and DAF development standards
 *
 */
package org.eclipse.lsp.cobol.core.hw;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.eclipse.lsp.cobol.core.cst.*;
import org.eclipse.lsp.cobol.core.cst.IdentificationDivision;
import org.eclipse.lsp.cobol.core.cst.base.CstNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Cobol parser
 */
@Slf4j
public class CobolParser {
  private final ParsingContext ctx;
  private final List<Diagnostic> diagnostics = new ArrayList<>();
  private final JaroWinklerSimilarity sim = new JaroWinklerSimilarity();
  private static final double SIMILARITY_THRESHOLD = 0.85;

  public CobolParser(CobolLexer lexer) {
    ctx = new ParsingContext(lexer);
  }

  /**
   * Create the CST
   * @return Concrete Syntax Tree
   */
  public ParseResult parse() {
    try {
      parseSourceUnit();
    } catch (ParseError error) {
      // TODO: unhandled error
      log.error(error.getMessage(), error);
    }
    return new ParseResult((SourceUnit) ctx.pop(), diagnostics);
  }

  private void parseSourceUnit() {
    ctx.push(new SourceUnit());
    while (ctx.getLexer().hasMore()) {
      try {
        spaces();
        if (match("ID", "IDENTIFICATION")) {
          parseProgram();
        } else {
          Token token = ctx.getLexer().forward(GrammarRule.SourceUnit).get(0);
          skip(token);
          error(token, "Unknown input: '" + token.getLexeme() + "'");
        }
        spaces();
      } catch (ParseError error) {
        synchronize();
      }
    }
  }

  private void skip(Token token) {
    if (ctx.peek().getChildren().isEmpty()) {
      Skipped s = new Skipped();
      s.getChildren().add(token);
      ctx.peek().getChildren().add(s);
      return;
    }

    CstNode lastChild = ctx.peek().getChildren().get(ctx.peek().getChildren().size() - 1);
    if (lastChild instanceof Skipped) {
      lastChild.getChildren().add(token);
    } else {
      Skipped s = new Skipped();
      s.getChildren().add(token);
      ctx.peek().getChildren().add(s);
    }
  }

  private void synchronize() {
    CobolLexer lexer = ctx.getLexer();
    GrammarRule rule = ctx.peek().getRule();
    Token t = lexer.forward(rule).get(0);
    while (!isEOF(t) && !isContinuationSymbol(t)) {
      skip(t);
      t = lexer.forward(rule).get(0);
    }
    if (!isEOF(t)) {
      skip(t);
    }
  }

  private boolean isContinuationSymbol(Token t) {
    return Objects.equals(t.getLexeme(), ".");
  }

  private static boolean isEOF(Token t) {
    return t.getType() == TokenType.EOF;
  }

  private boolean match(String... lexemes) {
    Token token = ctx.getLexer().peek(ctx.peek().getRule()).get(0);
    for (String l : lexemes) {
      if (sameLexeme(token, l)) {
        return true;
      }
    }
    return false;
  }


  private void parseProgram() {
    try {
      ProgramUnit programUnit = new ProgramUnit();
      ctx.push(programUnit);
      try {
        ctx.push(new IdentificationDivision());
        or("ID", "IDENTIFICATION");
        spaces();
        consume("DIVISION");
        consume(".");
        spaces();
        consume("PROGRAM-ID");
        optional(".");
        spaces();

        List<Token> programName = ctx.getLexer().peek(GrammarRule.ProgramUnit);
        programUnit.setName(programName.get(0).getLexeme());
        consume();
        spaces();
        if (match("IS")) {
          consume("IS");
          spaces();
          or("RECURSIVE", "INITIAL");
          spaces();
          optional("PROGRAM");
        } else if (match("RECURSIVE", "INITIAL")) {
          spaces();
          optional("PROGRAM");
        }
        spaces();
        optional(".");
        identificationDivisionContent();
      } finally {
        ctx.popAndAttach();
      }

      if (matchSeq("ENVIRONMENT", "DIVISION", ".")) {
        environmentDivisionContent();
      }

      if (matchSeq("DATA", "DIVISION", ".")) {
        dataDivisionContent();
      }

      if (matchSeq("PROCEDURE", "DIVISION")) {
        procedureDivisionContent();
      }

      if (matchSeq("ID", "DIVISION", ".")
              || matchSeq("IDENTIFICATION", "DIVISION", ".")) {
        spaces();
        parseProgram();
      }

      if (matchSeq("END", "PROGRAM")) {
        spaces();
        consume("END");
        spaces();
        consume("PROGRAM");
        spaces();
        consume(); // programId
        consume(".");
        spaces();
        ctx.popAndAttach();
        return;
      }

      ctx.popAndAttach();
    } catch (ParseError error) {
      CstNode node = ctx.pop();
      Skipped s = new Skipped();
      s.getChildren().addAll(node.getChildren());
      ctx.peek().getChildren().add(s);
      skipToken();
      throw error;
    }
  }

  private void skipToken() {
    skip(ctx.getLexer().forward(ctx.peek().getRule()).get(0));
  }

  private void procedureDivisionContent() {
    spaces();
    ctx.push(new ProcedureDivision());
    consume("PROCEDURE");
    spaces();
    consume("DIVISION");
    optional(".");
    spaces();
    try {
      while (!isNextDivisionEofOrEop()) {
        consume();
      }
    } finally {
      ctx.popAndAttach();
    }
  }

  private void dataDivisionContent() {
    spaces();
    ctx.push(new DataDivision());
    consume("DATA");
    spaces();
    consume("DIVISION");
    consume(".");
    spaces();
    try {
      while (!isNextDivisionEofOrEop()) {
        consume();
      }
    } finally {
      ctx.popAndAttach();
    }
  }

  private void environmentDivisionContent() {
    spaces();
    ctx.push(new EnvironmentDivision());
    consume("ENVIRONMENT");
    spaces();
    consume("DIVISION");
    consume(".");
    spaces();
    try {
      while (!isNextDivisionEofOrEop()) {
        consume();
      }
    } finally {
      ctx.popAndAttach();
    }
  }

  private void identificationDivisionContent() {
    while (!isNextDivisionEofOrEop()) {
      consume();
    }
  }

  private boolean isNextDivisionEofOrEop() {
    return matchSeq("ENVIRONMENT", "DIVISION", ".")
            || matchSeq("IDENTIFICATION", "DIVISION", ".")
            || matchSeq("ID", "DIVISION", ".")
            || matchSeq("DATA", "DIVISION", ".")
            || matchSeq("PROCEDURE", "DIVISION")
            || matchSeq("END", "PROGRAM")
            || isEOF(ctx.getLexer().peek(ctx.peek().getRule()).get(0));
  }

  private boolean matchSeq(String... lexemes) {
    List<Token> tokens = ctx.getLexer()
            .peekSeq(ctx.peek().getRule(), lexemes.length, t -> t.getType() == TokenType.WHITESPACE);
    if (tokens.size() != lexemes.length) {
      return false;
    }
    for (int i = 0; i < tokens.size(); i++) {
      if (!sameLexeme(tokens.get(i), lexemes[i])) {
        return false;
      }
    }
    return true;
  }

  private void optional(String lexeme) {
    GrammarRule rule = ctx.peek().getRule();
    if (match(lexeme)) {
      List<Token> forward = ctx.getLexer().forward(rule);
      ctx.peek().getChildren().add(forward.get(0));
    }
  }

  private void consume(String expectedLexeme) {
    GrammarRule rule = ctx.peek().getRule();
    Token token = ctx.getLexer().peek(rule).get(0);
    if (!sameLexeme(token, expectedLexeme)) {
      error(token, "Unexpected token: '" + token.getLexeme() + "'. Expect: '" + expectedLexeme + "'");
    }
    List<Token> forward = ctx.getLexer().forward(rule);
    ctx.peek().getChildren().add(forward.get(0));
  }

  private void consume() {
    List<Token> forward = ctx.getLexer().forward(ctx.peek().getRule());
    ctx.peek().getChildren().add(forward.get(0));
  }

  private void or(String... expectedTokens) {
    List<Token> next = ctx.getLexer().peek(ctx.peek().getRule());
    if (next.isEmpty()) {
      return;
    }
    for (String content : expectedTokens) {
      if (sameLexeme(next.get(0), content)) {
        List<Token> id = ctx.getLexer().forward(ctx.peek().getRule());
        ctx.peek().getChildren().add(id.get(0));
        return;
      }
    }
    error(next.get(0), "Unexpected token: '" + next.get(0).getLexeme() + "'. Expected: " + String.join(",", expectedTokens));
  }

  boolean sameLexeme(Token lexemeToken, String expectedLexeme) {
    if (lexemeToken == null || lexemeToken.getLexeme() == null || expectedLexeme == null) {
      return false;
    }
    Double apply = sim.apply(lexemeToken.getLexeme().toUpperCase(), expectedLexeme.toUpperCase());
    if (apply == 1) {
      return true;
    }
    if (apply > SIMILARITY_THRESHOLD) {
      diagnostics.add(getSimilarKeywordPassedSyntaxError(lexemeToken, expectedLexeme));
      return true;
    }
    return false;
  }

  private Diagnostic getSimilarKeywordPassedSyntaxError(Token lexemeToken, String expectedLexeme) {
    return new Diagnostic(getTokenRange(lexemeToken),
            String.format("provided %s but expected %s", lexemeToken.getLexeme(), expectedLexeme));
  }

  private void spaces() {
    Token token = ctx.getLexer().peek(ctx.peek().getRule()).get(0);
    while (token.getType() != TokenType.EOF
            && (token.getType() == TokenType.WHITESPACE || token.getType() == TokenType.NEW_LINE)) {
      ctx.peek().getChildren().add(ctx.getLexer().forward(ctx.peek().getRule()).get(0));
      token = ctx.getLexer().peek(ctx.peek().getRule()).get(0);
    }
  }

  private void error(Token token, String message) {
    Diagnostic syntaxError = new Diagnostic(getTokenRange(token), message);
    diagnostics.add(syntaxError);
    throw new ParseError(message);
  }

  private static Range getTokenRange(Token token) {
    return new Range(new Position(token.getLine(), token.getStartPositionInLine()), new Position(token.getLine(),
            token.getStartPositionInLine() + token.getLexeme().length()));
  }
}
