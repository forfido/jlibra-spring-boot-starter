package dev.jlibra.spring.action;

import com.google.protobuf.ByteString;
import dev.jlibra.AccountAddress;
import dev.jlibra.JLibra;
import dev.jlibra.KeyUtils;
import dev.jlibra.admissioncontrol.query.AccountResource;
import dev.jlibra.admissioncontrol.query.ImmutableGetAccountState;
import dev.jlibra.admissioncontrol.query.ImmutableQuery;
import dev.jlibra.admissioncontrol.query.UpdateToLatestLedgerResult;
import dev.jlibra.admissioncontrol.transaction.*;
import dev.jlibra.admissioncontrol.transaction.result.LibraTransactionException;
import dev.jlibra.admissioncontrol.transaction.result.SubmitTransactionResult;
import dev.jlibra.move.Move;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import static java.time.Instant.now;
import static java.util.Arrays.asList;

@Component
public class PeerToPeerTransfer {

    @Autowired
    private JLibra jLibra;

    public SubmitTransactionResult transferFunds(String toAddress, long amountInMicroLibras, PublicKey publicKey,
                                                   PrivateKey privateKey, long gasUnitPrice, long maxGasAmount) throws LibraTransactionException {
        U64Argument amountArgument = new U64Argument(amountInMicroLibras);
        AccountAddressArgument addressArgument = new AccountAddressArgument(Hex.decode(toAddress));

        long validMaxGasAmount = (maxGasAmount == -1) ? jLibra.getMaxGasAmount() : maxGasAmount;
        long validGasUnitPrice = (gasUnitPrice == -1) ? jLibra.getGasUnitPrice() : gasUnitPrice;

        Transaction transaction = ImmutableTransaction.builder()
                .sequenceNumber(maybeFindSequenceNumber(AccountAddress.ofHexString(KeyUtils.toHexStringLibraAddress(publicKey.getEncoded()))))
                .maxGasAmount(validMaxGasAmount)
                .gasUnitPrice(validGasUnitPrice)
                .senderAccount(AccountAddress.ofPublicKey(publicKey))
                .expirationTime(now().getEpochSecond() + 10000L)
                .payload(ImmutableScript.builder()
                        .code(ByteString.copyFrom(Move.peerToPeerTransferAsBytes()))
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

        //SubmitTransactionResult result = jLibra.getAdmissionControl().submitTransaction(signedTransaction);
        return jLibra.getAdmissionControl().submitTransaction(signedTransaction);
    }

    protected long maybeFindSequenceNumber(AccountAddress forAddress) {
        UpdateToLatestLedgerResult result = jLibra.getAdmissionControl().updateToLatestLedger(
                ImmutableQuery.builder().accountStateQueries(asList(
                        ImmutableGetAccountState.builder().address(forAddress).build())).build());

        return result.getAccountResources()
                .stream()
                .filter(accountState -> Arrays.equals(
                        accountState.getAuthenticationKey(),
                        forAddress.asByteArray()))
                .map(AccountResource::getSequenceNumber)
                .findFirst()
                .orElse(0);
    }

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