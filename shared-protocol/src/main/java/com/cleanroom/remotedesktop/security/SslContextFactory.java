package com.cleanroom.remotedesktop.security;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public final class SslContextFactory {
    private static final Logger log = LoggerFactory.getLogger(SslContextFactory.class);

    private SslContextFactory() {}

    public static SslContext createServerContext() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate("remotedesktop.cleanroom");
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    public static SslContext createClientContext() throws SSLException {
        return SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE) // For self-signed dev/testing
                .build();
    }
}

