package dev.jlibra.spring.action;

import dev.jlibra.AccountAddress;
import dev.jlibra.JLibra;
import dev.jlibra.admissioncontrol.query.ImmutableGetAccountState;
import dev.jlibra.admissioncontrol.query.ImmutableGetAccountTransactionBySequenceNumber;
import dev.jlibra.admissioncontrol.query.ImmutableQuery;
import dev.jlibra.admissioncontrol.query.UpdateToLatestLedgerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Arrays.asList;

@Component
public class AccountStateQuery {

    @Autowired
    private JLibra jLibra;

    public UpdateToLatestLedgerResult queryBalance(String address) {
        return jLibra.getAdmissionControl()
                .updateToLatestLedger(ImmutableQuery.builder()
                        .accountStateQueries(asList(
                                ImmutableGetAccountState.builder()
                                    .address(AccountAddress.ofHexString(address))
                                    .build()))
                        .build());
    }

    public UpdateToLatestLedgerResult queryTransactionsBySequenceNumber(String address, long sequenceNumber) {
        return jLibra.getAdmissionControl().updateToLatestLedger(ImmutableQuery.builder()
                        .accountTransactionBySequenceNumberQueries(
                                asList(ImmutableGetAccountTransactionBySequenceNumber.builder()
                                    .accountAddress(AccountAddress.ofHexString(address))
                                    .sequenceNumber(sequenceNumber)
                                    .build()))
                        .build());
    }

}
