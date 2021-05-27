/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.model.tracekey;

import ch.ubique.swisscovid.cn.sdk.backend.model.util.UrlBase64StringDeserializer;
import ch.ubique.swisscovid.cn.sdk.backend.model.util.UrlBase64StringSerializer;
import ch.ubique.openapi.docannotations.Documentation;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import javax.validation.constraints.NotNull;

public class TraceKey {

  private Integer id;

  @NotNull private int version;

  @JsonSerialize(using = UrlBase64StringSerializer.class)
  @JsonDeserialize(using = UrlBase64StringDeserializer.class)
  @NotNull
  @Documentation(description = "base64 url encoded bytes")
  private byte[] identity;

  @JsonSerialize(using = UrlBase64StringSerializer.class)
  @JsonDeserialize(using = UrlBase64StringDeserializer.class)
  @NotNull
  @Documentation(description = "base64 url encoded bytes")
  private byte[] secretKeyForIdentity;

  @NotNull private Instant day;

  private Instant createdAt;

  @JsonSerialize(using = UrlBase64StringSerializer.class)
  @JsonDeserialize(using = UrlBase64StringDeserializer.class)
  @Documentation(description = "base64 url encoded bytes")
  private byte[] encryptedAssociatedData;

  @JsonSerialize(using = UrlBase64StringSerializer.class)
  @JsonDeserialize(using = UrlBase64StringDeserializer.class)
  @Documentation(description = "base64 url encoded bytes")
  private byte[] cipherTextNonce;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public byte[] getIdentity() {
    return identity;
  }

  public void setIdentity(byte[] identity) {
    this.identity = identity;
  }

  public byte[] getSecretKeyForIdentity() {
    return secretKeyForIdentity;
  }

  public void setSecretKeyForIdentity(byte[] secretKeyForIdentity) {
    this.secretKeyForIdentity = secretKeyForIdentity;
  }

  public Instant getDay() {
    return day;
  }

  public void setDay(Instant day) {
    this.day = day;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public byte[] getEncryptedAssociatedData() {
    return encryptedAssociatedData;
  }

  public void setEncryptedAssociatedData(byte[] encryptedAssociatedData) {
    this.encryptedAssociatedData = encryptedAssociatedData;
  }

  public byte[] getCipherTextNonce() {
    return cipherTextNonce;
  }

  public void setCipherTextNonce(byte[] cipherTextNonce) {
    this.cipherTextNonce = cipherTextNonce;
  }
}
