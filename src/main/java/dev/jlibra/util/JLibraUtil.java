package dev.jlibra.util;

import dev.jlibra.AccountAddress;
import dev.jlibra.JLibra;
import dev.jlibra.admissioncontrol.query.AccountResource;
import dev.jlibra.admissioncontrol.query.ImmutableGetAccountState;
import dev.jlibra.admissioncontrol.query.ImmutableQuery;
import dev.jlibra.admissioncontrol.query.UpdateToLatestLedgerResult;
import dev.jlibra.serialization.ByteSequence;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.Arrays.asList;

public class JLibraUtil {

    private JLibra jlibra;

    private static final Logger logger = LogManager.getLogger(JLibraUtil.class);

    private JLibraUtil() {
        // no instantiation
    }

    public JLibraUtil(JLibra jlibra) {
        this.jlibra = jlibra;
    }

    public long findBalance(String address) {

        AccountAddress accountAddress = AccountAddress.ofByteSequence(ByteSequence.from(address));

        UpdateToLatestLedgerResult result = jlibra.getAdmissionControl().updateToLatestLedger(
                ImmutableQuery.builder().accountStateQueries(
                        asList(ImmutableGetAccountState.builder().address(accountAddress).build()))
                        .build());

        long balance = result.getAccountStateQueryResults()
                .stream()
                .filter(accountState -> accountState.getAuthenticationKey()
                            .equals(accountAddress.getByteSequence()))
                .map(AccountResource::getBalanceInMicroLibras)
                .findFirst()
                .orElse(0L);

        return balance;
    }

    public long maybeFindSequenceNumber(AccountAddress forAddress) {
        UpdateToLatestLedgerResult result = jlibra.getAdmissionControl().updateToLatestLedger(
                ImmutableQuery.builder().accountStateQueries(asList(
                        ImmutableGetAccountState.builder().address(forAddress).build())).build());

        return result.getAccountStateQueryResults()
                .stream()
                .filter(accountState ->
                        accountState.getAuthenticationKey()
                                .equals(forAddress.getByteSequence()))
                .map(AccountResource::getSequenceNumber)
                .findFirst()
                .orElse(0);
    }

    public long mint(String address, long amount) {
        long amountInMicroLibras = amount * 1000_000;

        HttpResponse<String> response = Unirest.post(jlibra.getFaucetHost() + ":" + jlibra.getFaucetPort())
                .queryString("amount", amountInMicroLibras)
                .queryString("address", address)
                .asString();

        if (response.getStatus() != 200) {
            throw new IllegalStateException(String.format("Minting %d for %s at faucet %s:%d failed.", amount, address, jlibra.getFaucetHost(), jlibra.getFaucetPort()));
        }

        Long balance = findBalance(address);

        logger.info("Balance for {} is {}", address, balance);

        return amountInMicroLibras;
    }

}
