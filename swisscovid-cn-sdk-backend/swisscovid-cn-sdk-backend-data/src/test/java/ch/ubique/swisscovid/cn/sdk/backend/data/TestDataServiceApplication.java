package ch.ubique.swisscovid.cn.sdk.backend.data;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {
      "ch.ubique.swisscovid.cn.sdk.backend.data.config",
      "ch.admin.bag.covidcertificate.log",
      "ch.admin.bag.covidcertificate.rest"
    })
public class TestDataServiceApplication {}
