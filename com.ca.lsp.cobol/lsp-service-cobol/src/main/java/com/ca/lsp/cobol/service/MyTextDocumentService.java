/*
 * Copyright (c) 2019 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.cobol.service;

import com.ca.lsp.cobol.service.delegates.communications.Communications;
import com.ca.lsp.cobol.service.delegates.completions.Completions;
import com.ca.lsp.cobol.service.delegates.formations.Formations;
import com.ca.lsp.cobol.service.delegates.references.Highlights;
import com.ca.lsp.cobol.service.delegates.references.References;
import com.ca.lsp.cobol.service.delegates.validations.AnalysisResult;
import com.ca.lsp.cobol.service.delegates.validations.LanguageEngineFacade;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * This class is a set of end-points to apply text operations for COBOL documents. All the requests
 * that start with "textDocument" go here. The current implementation contains only supported
 * language features. For more details, please, see the specification:
 * https://microsoft.github.io//language-server-protocol/specifications/specification-3-14/
 *
 * <p>For the maintainers: Please, add logging for exceptions if you run any asynchronous operation.
 * Also, you you perform any communication with the client, do it a using {@link Communications}
 * instance.
 */
@Slf4j
@Singleton
public class MyTextDocumentService implements TextDocumentService {
  private static final List<String> COBOL_IDS = Arrays.asList("cobol", "cbl", "cob", "COBOL");

  private final Map<String, MyDocumentModel> docs = new ConcurrentHashMap<>();

  private Communications communications;
  private LanguageEngineFacade engine;
  private Formations formations;
  private Completions completions;

  @Inject
  public MyTextDocumentService(
      Communications communications,
      LanguageEngineFacade engine,
      Formations formations,
      Completions completions) {
    this.communications = communications;
    this.engine = engine;
    this.formations = formations;
    this.completions = completions;
  }

  Map<String, MyDocumentModel> getDocs() {
    return new HashMap<>(docs);
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams params) {
    String uri = params.getTextDocument().getUri();
    return CompletableFuture.<Either<List<CompletionItem>, CompletionList>>supplyAsync(
            () -> Either.forRight(completions.collectFor(docs.get(uri), params)))
        .whenComplete(
            reportExceptionIfThrown(createDescriptiveErrorMessage("completion lookup", uri)));
  }

  @Override
  public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
    return CompletableFuture.supplyAsync(() -> completions.resolveDocumentationFor(unresolved))
        .whenComplete(
            reportExceptionIfThrown(
                createDescriptiveErrorMessage("completion resolving", unresolved.getLabel())));
  }

  @Override
  public CompletableFuture<List<? extends Location>> definition(
      TextDocumentPositionParams position) {
    String uri = position.getTextDocument().getUri();
    return CompletableFuture.<List<? extends Location>>supplyAsync(
            () -> References.findDefinition(docs.get(uri), position))
        .whenComplete(
            reportExceptionIfThrown(createDescriptiveErrorMessage("definitions resolving", uri)));
  }

  @Override
  public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
    String uri = params.getTextDocument().getUri();
    return CompletableFuture.<List<? extends Location>>supplyAsync(
            () -> References.findReferences(docs.get(uri), params, params.getContext()))
        .whenComplete(
            reportExceptionIfThrown(createDescriptiveErrorMessage("references resolving", uri)));
  }

  @Override
  public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
      TextDocumentPositionParams position) {
    String uri = position.getTextDocument().getUri();
    return CompletableFuture.<List<? extends DocumentHighlight>>supplyAsync(
            () -> Highlights.findHighlights(docs.get(uri), position))
        .whenComplete(
            reportExceptionIfThrown(createDescriptiveErrorMessage("document highlighting", uri)));
  }

  @Override
  public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
    String uri = params.getTextDocument().getUri();
    MyDocumentModel model = docs.get(uri);
    return CompletableFuture.<List<? extends TextEdit>>supplyAsync(() -> formations.format(model))
        .whenComplete(reportExceptionIfThrown(createDescriptiveErrorMessage("formatting", uri)));
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getTextDocument().getText();
    String langId = params.getTextDocument().getLanguageId();
    registerDocument(uri, new MyDocumentModel(text, AnalysisResult.empty()));
    registerEngineAndAnalyze(uri, langId, text);
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    String text = params.getContentChanges().get(0).getText();

    analyzeChanges(uri, text);
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    String uri = params.getTextDocument().getUri();
    log.info("Document closing invoked");
    docs.remove(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    log.info("Document saved...");
  }

  private void registerEngineAndAnalyze(String uri, String languageType, String text) {
    String fileExtension = extractExtension(uri);
    if (fileExtension != null && !isCobolFile(fileExtension)) {
      communications.notifyThatExtensionIsUnsupported(fileExtension);
    } else if (isCobolFile(languageType)) {
      communications.notifyThatLoadingInProgress(uri);
      analyzeDocumentFirstTime(uri, text);
    } else {
      communications.notifyThatEngineNotFound(languageType);
    }
  }

  private boolean isCobolFile(String identifier) {
    return COBOL_IDS.contains(identifier);
  }

  private String extractExtension(String uri) {
    return Optional.ofNullable(uri)
        .filter(it -> it.indexOf('.') > -1)
        .map(it -> it.substring(it.lastIndexOf('.') + 1))
        .orElse(null);
  }

  private void analyzeDocumentFirstTime(String uri, String text) {
    CompletableFuture.runAsync(
            () -> {
              AnalysisResult result = engine.analyze(text);
              docs.get(uri).setAnalysisResult(result);
              publishResult(uri, result);
            })
        .whenComplete(reportExceptionIfThrown(createDescriptiveErrorMessage("analysis", uri)));
  }

  private void analyzeChanges(String uri, String text) {
    CompletableFuture.runAsync(
            () -> {
              AnalysisResult result = engine.analyze(text);
              registerDocument(uri, new MyDocumentModel(text, result));
              communications.publishDiagnostics(uri, result.getDiagnostics());
            })
        .whenComplete(reportExceptionIfThrown(createDescriptiveErrorMessage("analysis", uri)));
  }

  private void publishResult(String uri, AnalysisResult result) {
    communications.cancelProgressNotification(uri);
    communications.publishDiagnostics(uri, result.getDiagnostics());
    if (result.getDiagnostics().isEmpty()) communications.notifyThatDocumentAnalysed(uri);
  }

  private void registerDocument(String uri, MyDocumentModel document) {
    docs.put(uri, document);
  }

  private String createDescriptiveErrorMessage(String action, String uri) {
    return String.format("An exception was thrown while applying %s for %s:", action, uri);
  }

  private BiConsumer<Object, Throwable> reportExceptionIfThrown(String message) {
    return (res, ex) -> Optional.ofNullable(ex).ifPresent(it -> log.error(message, it));
  }
}
