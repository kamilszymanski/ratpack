/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.handling;

import org.ratpackframework.file.internal.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileSystemContextHandler;
import org.ratpackframework.file.internal.TargetFileStaticAssetRequestHandler;
import org.ratpackframework.handling.internal.*;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.path.PathBinder;
import org.ratpackframework.path.internal.PathHandler;
import org.ratpackframework.path.internal.TokenPathBinder;
import org.ratpackframework.util.Action;

import java.io.File;
import java.util.Arrays;

/**
 * Factory methods for certain types of handlers.
 * <p>
 * Typically used by {@link Chain} implementations to build a handler chain.
 * <pre class="groovyTestCase">
 * import static org.ratpackframework.handling.Handlers.*;
 * import org.ratpackframework.handling.Handler;
 * import org.ratpackframework.handling.Chain;
 * import org.ratpackframework.handling.Exchange;
 * import org.ratpackframework.util.Action;
 *
 * class ExampleHandler implements Handler {
 *   void handle(Exchange exchange) {
 *     // implementation omitted
 *   }
 * }
 *
 * class ChainBuilder implements Action&lt;Chain&gt; {
 *   void execute(Chain chain) {
 *     chain.add(assets("public"));
 *     chain.add(get("info", new ExampleHandler()));
 *     chain.add(path("api", new Action&lt;Chain&gt;() {
 *       void execute(Chain apiChain) {
 *         apiChain.add(get("version", new ExampleHandler()));
 *         apiChain.add(get("log", new ExampleHandler()));
 *       }
 *     }));
 *   }
 * }
 *
 * Handler handler = chain(new ChainBuilder());
 * </pre>
 */
public abstract class Handlers {

  public static Handler context(final Object context, Action<? super Chain> builder) {
    return context(context, chain(builder));
  }

  public static <T> Handler context(Class<? super T> type, T object, Action<? super Chain> builder) {
    return context(type, object, chain(builder));
  }

  public static Handler context(Object object, final Handler handler) {
    return new ContextInsertingHandler(object, handler);
  }

  public static <T> Handler context(Class<? super T> type, T object, final Handler handler) {
    return new ContextInsertingHandler(type, object, handler);
  }

  public static Handler chain(Action<? super Chain> action) {
    return ChainBuilder.INSTANCE.build(ChainActionTransformer.INSTANCE, action);
  }

  public static Handler fileSystem(String path, Handler handler) {
    return new FileSystemContextHandler(new File(path), handler);
  }

  public static Handler fileSystem(String path, Action<? super Chain> builder) {
    return fileSystem(path, chain(builder));
  }

  public static Handler assets(String path, Handler notFound) {
    return assets(path, new String[0], notFound);
  }

  public static Handler assets(String path, String... indexFiles) {
    return assets(path, indexFiles, next());
  }

  public static Handler assets(String path, String[] indexFiles, final Handler notFound) {
    Handler fileHandler = FileStaticAssetRequestHandler.INSTANCE;
    Handler directoryHandler = new DirectoryStaticAssetRequestHandler(Arrays.asList(indexFiles), fileHandler);
    Handler contextSetter = new TargetFileStaticAssetRequestHandler(directoryHandler);

    return fileSystem(path, chain(contextSetter, notFound));
  }

  public static Handler assetsPath(String uriPath, String fsPath, Handler notFound) {
    return path(uriPath, assets(fsPath, notFound));
  }

  public static Handler assetsPath(String uriPath, String fsPath, String... indexFiles) {
    return path(uriPath, assets(fsPath, indexFiles));
  }

  public static Handler assetsPath(String uriPath, String fsPath, String[] indexFiles, final Handler notFound) {
    return path(uriPath, assets(fsPath, indexFiles, notFound));
  }

  public static Handler chain(final Handler... handlers) {
    return new ChainHandler(Arrays.asList(handlers));
  }

  public static Handler next() {
    return NextHandler.INSTANCE;
  }

  public static Handler get(String path, Handler handler) {
    return path(path, chain(MethodHandler.GET, handler));
  }

  public static Handler get(Handler handler) {
    return path("", chain(MethodHandler.GET, handler));
  }

  public static Handler post(String path, Handler handler) {
    return path(path, chain(MethodHandler.POST, handler));
  }

  public static Handler path(String path, Action<? super Chain> builder) {
    return path(path, chain(builder));
  }

  public static Handler path(String path, Handler handler) {
    return pathBinding(new TokenPathBinder(path, false), handler);
  }

  public static Handler handler(String path, Handler handler) {
    return pathBinding(new TokenPathBinder(path, true), handler);
  }

  public static Handler pathBinding(PathBinder pathBinder, Handler handler) {
    return new PathHandler(pathBinder, handler);
  }

}
