package dev.jlibra.spring.action;

import dev.jlibra.AccountAddress;
import dev.jlibra.JLibra;
import dev.jlibra.admissioncontrol.transaction.*;
import dev.jlibra.admissioncontrol.transaction.result.LibraTransactionException;
import dev.jlibra.admissioncontrol.transaction.result.SubmitTransactionResult;
import dev.jlibra.move.Move;
import dev.jlibra.serialization.ByteSequence;
import dev.jlibra.util.JLibraUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;

import static java.time.Instant.now;

@Component
public class PeerToPeerTransfer {

    @Autowired
    private JLibra jLibra;

    @Autowired
    private JLibraUtil jLibraUtil;

    public SubmitTransactionResult transferFunds(String toAddress, long amountInMicroLibras, PublicKey publicKey,
                                                   PrivateKey privateKey, long gasUnitPrice, long maxGasAmount) throws LibraTransactionException {

        Long sequenceNumber = jLibraUtil.maybeFindSequenceNumber(AccountAddress.ofPublicKey(publicKey));

        U64Argument amountArgument = new U64Argument(amountInMicroLibras);
        AccountAddressArgument addressArgument = new AccountAddressArgument(ByteSequence.from(toAddress));

        long validMaxGasAmount = (maxGasAmount == -1) ? jLibra.getMaxGasAmount() : maxGasAmount;
        long validGasUnitPrice = (gasUnitPrice == -1) ? jLibra.getGasUnitPrice() : gasUnitPrice;

        Transaction transaction = ImmutableTransaction.builder()
                .sequenceNumber(sequenceNumber)
                .maxGasAmount(validMaxGasAmount)
                .gasUnitPrice(validGasUnitPrice)
                .senderAccount(AccountAddress.ofPublicKey(publicKey))
                .expirationTime(now().getEpochSecond() + 1000)
                .payload(ImmutableScript.builder()
                        .code(Move.peerToPeerTransferAsBytes())
                        .addArguments(addressArgument, amountArgument)
                        .build())
                .build();

        SignedTransaction signedTransaction = ImmutableSignedTransaction.builder()
                .publicKey(publicKey)
                .transaction(transaction)
                .signature(ImmutableSignature.builder()
                        .privateKey(privateKey)
                        .transaction(transaction)
                        .build())
                .build();

        return jLibra.getAdmissionControl().submitTransaction(signedTransaction);
    }

//    protected long maybeFindSequenceNumber(AccountAddress forAddress) {
//        UpdateToLatestLedgerResult result = jLibra.getAdmissionControl().updateToLatestLedger(
//                ImmutableQuery.builder().accountStateQueries(asList(
//                        ImmutableGetAccountState.builder().address(forAddress).build())).build());
//
//        return result.getAccountStateQueryResults()
//                .stream()
//                .filter(accountState ->
//                        accountState.getAuthenticationKey()
//                            .equals(forAddress.getByteSequence()))
//                .map(AccountResource::getSequenceNumber)
//                .findFirst()
//                .orElse(0);
//    }

    public static class PeerToPeerTransferReceipt {

        public enum Status {
            OK, FAIL
        }

        private Status status;

        private PeerToPeerTransferReceipt(SubmitTransactionResult result) {
//            if (result.getAdmissionControlStatus().getCode() == AdmissionControlOuterClass.AdmissionControlStatusCode.Accepted) {
//                this.status = Status.OK;
//            } else {
//                this.status = Status.FAIL;
//            }

            this.status = Status.FAIL;
        }

        public Status getStatus() {
            return status;
        }

    }
}