/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Map;
import javax.sql.DataSource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public abstract class WSCloudBaseConfig extends WSBaseConfig {

    @Value("${datasource.maximumPoolSize:5}")
    int dataSourceMaximumPoolSize;

    @Value("${datasource.connectionTimeout:30000}")
    int dataSourceConnectionTimeout;

    @Value("${datasource.leakDetectionThreshold:0}")
    int dataSourceLeakDetectionThreshold;

    abstract String getSignaturePublicKey();

    abstract String getSignaturePrivateKey();

    @Bean
    @Override
    public DataSource dataSource() {
        PoolConfig poolConfig =
                new PoolConfig(dataSourceMaximumPoolSize, dataSourceConnectionTimeout);
        DataSourceConfig dbConfig =
                new DataSourceConfig(
                        poolConfig,
                        null,
                        null,
                        Map.of("leakDetectionThreshold", dataSourceLeakDetectionThreshold));
        CloudFactory factory = new CloudFactory();
        return factory.getCloud().getSingletonServiceConnector(DataSource.class, dbConfig);
    }

    @Bean
    @Override
    public Flyway flyway() {
        Flyway flyWay =
                Flyway.configure()
                        .dataSource(dataSource())
                        .locations("classpath:/db/migration/pgsql_cluster")
                        .load();
        flyWay.migrate();
        return flyWay;
    }

    @Override
    public String getDbType() {
        return "pgsql";
    }

    protected KeyPair getSignatureKeyPair()
            throws CertificateException, IOException, NoSuchAlgorithmException,
                    InvalidKeySpecException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        Security.setProperty("crypto.policy", "unlimited");
        return new KeyPair(loadPublicKeyFromString(), loadPrivateKeyFromString());
    }

    private PrivateKey loadPrivateKeyFromString()
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException,
                    InvalidKeySpecException {
        String privateKey = getSignaturePrivateKey();
        Reader reader = new StringReader(privateKey);
        PemReader readerPem = new PemReader(reader);
        PemObject obj = readerPem.readPemObject();
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(obj.getContent());
        KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
        return kf.generatePrivate(pkcs8KeySpec);
    }

    private PublicKey loadPublicKeyFromString() throws CertificateException {
        return CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(getSignaturePublicKey().getBytes()))
                .getPublicKey();
    }
}
