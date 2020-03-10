package com.redhat.summit.test;

import com.redhat.summit.model.CreditCardAccount;
import com.redhat.summit.model.Merchant;
import com.redhat.summit.util.KafkaUtils;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@QuarkusTest
public class JsonAccountParserTest {

    @Inject
    KafkaUtils kafkaUtils;

    /**
     * Test
     * @throws Exception
     */
    @Test
    public void testJsonAccountParser() throws Exception {
        List<CreditCardAccount> creditCardAccounts = kafkaUtils.getDummyCreditCardAccounts();
        assertNotNull(creditCardAccounts);
        assertEquals(100, creditCardAccounts.size());
    }

    @Test
    public void testJsonMerchantParser() throws Exception {
        List<Merchant> merchants = kafkaUtils.getMerchants();
        assertNotNull(merchants);
        assertEquals(50, merchants.size());
    }
}
