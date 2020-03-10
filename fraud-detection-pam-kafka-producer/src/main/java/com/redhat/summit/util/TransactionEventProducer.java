package com.redhat.summit.util;

import com.redhat.summit.model.CreditCardTransaction;
import com.redhat.summit.model.TransactionEventConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@ApplicationScoped
public class TransactionEventProducer {

    @Inject
    KafkaUtils kafkaUtils;

    /**
     * Builds Custom Transactions for TransactionEventConfiguration
     * and determines producer interval rate
     *
     * @param eventConfig
     * @return
     * @throws Exception
     */
    public TransactionEventConfiguration processEventConfiguration(TransactionEventConfiguration eventConfig) throws Exception {

        log.info("Event Configuration : {}", eventConfig);

        if (eventConfig != null) {
            int txCount = eventConfig.getTotalTransactionCount() <= 0 ? 1 : eventConfig.getTotalTransactionCount();
            long totalGenTxAmount = eventConfig.getTotalGeneratedTxAmount() <= 0 ? 10000l : eventConfig.getTotalGeneratedTxAmount();

            log.info("Generating {} Transactions with a Total TX Amount of : {} (Not including specified Custom TXs)", txCount, totalGenTxAmount);
            List<CreditCardTransaction> transactions = new ArrayList<>();

            // Add Custom Transactions
            if (!CollectionUtils.isEmpty(eventConfig.getCustomTransactions())) {
                log.info("Adding {} Custom Transactions ", eventConfig.getCustomTransactions().size());
                eventConfig.setTotalCustomTxAmount(0);
                eventConfig.getCustomTransactions().stream().forEach(t -> {
                    eventConfig.setTotalCustomTxAmount(eventConfig.getTotalCustomTxAmount() + t.getAmount());
                    transactions.add(kafkaUtils.buildCCTX(eventConfig.getAccount(), t.getAmount()));
                });
            }

            // Generate Transactions
            eventConfig.setTotalAmount(Double.valueOf(totalGenTxAmount) + eventConfig.getTotalCustomTxAmount());
            Long txAmount = totalGenTxAmount / txCount;
            AtomicInteger txAmountApplied = new AtomicInteger(0);
            log.info("Setting each TX with a avg txAmount : {}", txAmount);

            IntStream.range(0, txCount).forEach(idx -> {
                if (idx == txCount - 1) {
                    Long amount = totalGenTxAmount - txAmountApplied.get();
                    log.info("Add txAmount: {} delta to final TX, Total txAmount: {} added", amount, txAmountApplied.get() + amount);
                    transactions.add(kafkaUtils.buildCCTX(eventConfig.getAccount(), amount.doubleValue()));
                } else {
                    txAmountApplied.getAndAdd(txAmount.intValue());
                    log.debug("Adding Transaction Amount as Double : {}", txAmount.doubleValue());
                    transactions.add(kafkaUtils.buildCCTX(eventConfig.getAccount(), txAmount.doubleValue()));
                }
            });

            // Determine Execution Timeframe per Transaction
            if (StringUtils.isNotBlank(eventConfig.getTimeframe())) {

                String var = eventConfig.getTimeframe().trim().toLowerCase();
                log.info("Processing Timeframe value of : {}", var);
                long interval = Long.valueOf(var.substring(0, var.length() - 1)).longValue();
                long timeframe = 0l;
                char c = var.charAt(var.length() - 1);

                // Parse timeframe
                log.info("Interval : {} :: Timeframe Type : {}", interval, c);
                switch (c) {
                    case 'h':
                        log.debug("Setting timeframe type to HOURS");
                        timeframe = TimeUnit.HOURS.toMillis(interval);
                        break;
                    case 'm':
                        log.debug("Setting timeframe type to MINUTES");
                        timeframe = TimeUnit.MINUTES.toMillis(interval);
                        break;
                    case 's':
                        log.debug("Setting timeframe type to SECONDS");
                        timeframe = TimeUnit.SECONDS.toMillis(interval);
                        break;
                    default:
                        log.info("Timeframe unparsable, defaulting to 0");
                }

                // Limit maximum producer time to 10 mins
                if (timeframe > TimeUnit.MINUTES.toMillis(10)) {
                    timeframe = TimeUnit.MINUTES.toMillis(10);
                }

                // Calculate per TX interval
                if (timeframe > 0) {
                    log.info("Timeframe of {} millis calculated, dividing by total transaction count: {} ", timeframe, transactions.size());
                    eventConfig.setIntervalInMs(timeframe / transactions.size());
                }

                log.info("Each Transaction will have an interval of {} millis", eventConfig.getIntervalInMs());
            }

            log.info("Total of {} Transactions Generated for Kafka Events", transactions.size());
            transactions.stream().forEach(t -> log.info("TX: {}", t));
            eventConfig.setCustomTransactions(transactions);
        } else {
            log.error("Config is null, unable to generate TX's");

            // TODO Add proper error handling
            throw new Exception("No config..");
        }

        return eventConfig;
    }
}