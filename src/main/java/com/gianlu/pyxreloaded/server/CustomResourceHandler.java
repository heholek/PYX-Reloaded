package com.gianlu.pyxreloaded.server;

import com.gianlu.pyxreloaded.Utils;
import com.gianlu.pyxreloaded.singletons.Preferences;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CustomResourceHandler extends ResourceHandler {
    private final boolean cacheEnabled;

    public CustomResourceHandler(Preferences preferences) {
        super(buildResourceManager(preferences));

        cacheEnabled = preferences.getBoolean("cacheEnabled", true);
        setCachable(value -> cacheEnabled);
    }

    private static ResourceManager buildResourceManager(Preferences preferences) {
        Path root = Paths.get(preferences.getString("webContent", "./WebContent")).toAbsolutePath();
        boolean cacheEnabled = preferences.getBoolean("cacheEnabled", true);

        return PathResourceManager.builder()
                .setBase(root)
                .setAllowResourceChangeListeners(false)
                .setETagFunction(new ETagSupplier(cacheEnabled, Utils.generateAlphanumericString(5)))
                .build();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        super.handleRequest(exchange);

        HeaderMap headers = exchange.getResponseHeaders();
        if (cacheEnabled) headers.add(Headers.CACHE_CONTROL, "private, no-cache");
        else headers.add(Headers.CACHE_CONTROL, "private, no-store, no-cache");
    }

    private static class ETagSupplier implements PathResourceManager.ETagFunction {
        private static final Logger logger = Logger.getLogger(ETagSupplier.class);
        private final boolean cacheEnabled;
        private final String serverTag;

        ETagSupplier(boolean cacheEnabled, String serverTag) {
            this.cacheEnabled = cacheEnabled;
            this.serverTag = serverTag;

            logger.info("ETag supplier: " + serverTag);
        }

        @Override
        @Nullable
        public ETag generate(Path path) {
            if (!cacheEnabled) return null;
            return new ETag(false, serverTag);
        }
    }
}
