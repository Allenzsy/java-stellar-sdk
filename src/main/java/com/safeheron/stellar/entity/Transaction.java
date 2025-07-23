package com.safeheron.stellar.entity;

import com.safeheron.stellar.util.MemoUtil;
import lombok.Getter;
import lombok.NonNull;
import org.stellar.sdk.*;
import org.stellar.sdk.Memo;
import org.stellar.sdk.TimeBounds;
import org.stellar.sdk.exception.UnexpectedException;
import org.stellar.sdk.operations.*;
import org.stellar.sdk.operations.Operation;
import org.stellar.sdk.xdr.*;
import org.stellar.sdk.xdr.MuxedAccount;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents <a
 * href="https://developers.stellar.org/docs/learn/fundamentals/transactions/operations-and-transactions#transactions"
 * target="_blank">Transaction</a> in Stellar network.
 */
public class Transaction extends AbstractTransaction {
  /** Max fee paid for transaction in stroops (1 stroop = 0.0000001 XLM). */
  @Getter private final long fee;

  /** The source account for this transaction. */
  @Getter @NonNull private final String sourceAccount;

  /** The sequence number of the account creating this transaction. */
  @Getter private final long sequenceNumber;

  /** Operations included in the transaction. */
  @Getter @NonNull private final Operation[] operations;

  /** Memo attached to the transaction. */
  @Getter @NonNull private final Memo memo;

  /** The preconditions for the transaction. */
  @Getter private final TransactionPreconditions preconditions;

  /** The Soroban data for the transaction. */
  @Getter private final SorobanTransactionData sorobanData;

  private EnvelopeType envelopeType = EnvelopeType.ENVELOPE_TYPE_TX;

  /**
   * Creates a new transaction.
   *
   * <p>In general, we suggest you use {@link TransactionBuilder} to build transactions.
   *
   * @param sourceAccount The source account for the transaction.
   * @param fee The fee paid for the transaction in stroops (1 stroop = 0.0000001 XLM).
   * @param sequenceNumber The sequence number of the account creating this transaction.
   * @param operations The operations to include in the transaction.
   * @param memo The memo for the transaction.
   * @param preconditions The preconditions for the transaction.
   * @param sorobanData The Soroban data for the transaction.
   * @param network The network that the transaction is to be submitted to.
   */
  public Transaction(
      @NonNull String sourceAccount,
      long fee,
      long sequenceNumber,
      @NonNull Operation[] operations,
      Memo memo,
      TransactionPreconditions preconditions,
      SorobanTransactionData sorobanData,
      Network network) {
    super(network);
    this.sourceAccount = sourceAccount;
    this.sequenceNumber = sequenceNumber;
    this.operations = operations;
    if (operations.length == 0) {
      throw new IllegalArgumentException("At least one operation required");
    }
    this.preconditions = preconditions;
    this.fee = fee;
    this.memo = memo != null ? memo : Memo.none();
    this.sorobanData = sorobanData != null ? new SorobanDataBuilder(sorobanData).build() : null;
  }

  // setEnvelopeType is only used in tests which is why this method is package
  // protected
  void setEnvelopeType(EnvelopeType envelopeType) {
    this.envelopeType = envelopeType;
  }

  @Override
  public byte[] signatureBase() {
    TransactionSignaturePayload.TransactionSignaturePayloadTaggedTransaction taggedTransaction =
        new TransactionSignaturePayload.TransactionSignaturePayloadTaggedTransaction();
    taggedTransaction.setDiscriminant(EnvelopeType.ENVELOPE_TYPE_TX);
    taggedTransaction.setTx(this.toV1Xdr());
    return getTransactionSignatureBase(taggedTransaction, network);
  }

  /**
   * Gets the time bounds defined for the transaction.
   *
   * @return TimeBounds
   */
  public TimeBounds getTimeBounds() {
    return preconditions.getTimeBounds();
  }

  /**
   * Returns the claimable balance ID for the CreateClaimableBalanceOperation at the given index
   * within the transaction.
   *
   * @param index The index of the CreateClaimableBalanceOperation within the transaction, starting
   *     at 0
   * @return The claimable balance ID for the CreateClaimableBalanceOperation at the given index
   */
  public String getClaimableBalanceId(int index) {
    if (index < 0 || index >= operations.length) {
      throw new IllegalArgumentException(
          "index: " + index + " is outside the bounds of the operations within this transaction");
    }
    if (!(operations[index] instanceof CreateClaimableBalanceOperation)) {
      throw new IllegalArgumentException(
          "operation at index "
              + index
              + " is not of type CreateClaimableBalanceOperation: "
              + operations[index].getClass());
    }

    // We mimic the relevant code from Stellar Core
    // https://github.com/stellar/stellar-core/blob/9f3cc04e6ec02c38974c42545a86cdc79809252b/src/test/TestAccount.cpp#L285
    //
    // Note that the source account must be *unmuxed* for this to work.

    Uint32 opIndex = new Uint32(new XdrUnsignedInteger(index));
    SequenceNumber sequenceNumber = new SequenceNumber(new Int64(getSequenceNumber()));
    MuxedAccount sourceMuxedAccount = StrKey.decodeMuxedAccount(getSourceAccount());
    byte[] rawPublicKey;
    if (sourceMuxedAccount.getDiscriminant().equals(CryptoKeyType.KEY_TYPE_ED25519)) {
      rawPublicKey = sourceMuxedAccount.getEd25519().getUint256();
    } else {
      rawPublicKey = sourceMuxedAccount.getMed25519().getEd25519().getUint256();
    }
    AccountID sourceAccount = KeyPair.fromPublicKey(rawPublicKey).getXdrAccountId();
    HashIDPreimage.HashIDPreimageOperationID operationID =
        HashIDPreimage.HashIDPreimageOperationID.builder()
            .opNum(opIndex)
            .seqNum(sequenceNumber)
            .sourceAccount(sourceAccount)
            .build();
    HashIDPreimage hashIDPreimage =
        HashIDPreimage.builder()
            .discriminant(EnvelopeType.ENVELOPE_TYPE_OP_ID)
            .operationID(operationID)
            .build();
    try {
      Hash operationIDHash = new Hash(Util.hash(hashIDPreimage.toXdrByteArray()));
      ClaimableBalanceID claimableBalanceID =
          ClaimableBalanceID.builder()
              .v0(operationIDHash)
              .discriminant(ClaimableBalanceIDType.CLAIMABLE_BALANCE_ID_TYPE_V0)
              .build();
      return Util.bytesToHex(claimableBalanceID.toXdrByteArray()).toLowerCase();
    } catch (IOException e) {
      throw new UnexpectedException(e);
    }
  }

  /** Generates Transaction XDR object. */
  private TransactionV0 toXdr() {
    // fee
    Uint32 fee = new Uint32();
    fee.setUint32(new XdrUnsignedInteger(this.fee));
    // sequenceNumber
    Int64 sequenceNumberUint = new Int64();
    sequenceNumberUint.setInt64(sequenceNumber);
    SequenceNumber sequenceNumber = new SequenceNumber();
    sequenceNumber.setSequenceNumber(sequenceNumberUint);
    // operations
    org.stellar.sdk.xdr.Operation[] operations =
        new org.stellar.sdk.xdr.Operation[this.operations.length];
    for (int i = 0; i < this.operations.length; i++) {
      operations[i] = this.operations[i].toXdr();
    }
    // ext
    TransactionV0.TransactionV0Ext ext = new TransactionV0.TransactionV0Ext();
    ext.setDiscriminant(0);

    TransactionV0 transaction = new TransactionV0();
    transaction.setFee(fee);
    transaction.setSeqNum(sequenceNumber);
    transaction.setSourceAccountEd25519(
        StrKey.encodeToXDRAccountId(this.sourceAccount).getAccountID().getEd25519());
    transaction.setOperations(operations);
    transaction.setMemo(MemoUtil.toXdr(memo));
    transaction.setTimeBounds(getTimeBounds() == null ? null : getTimeBounds().toXdr());
    transaction.setExt(ext);
    return transaction;
  }

  private org.stellar.sdk.xdr.Transaction toV1Xdr() {
    // fee
    Uint32 fee = new Uint32();
    fee.setUint32((new XdrUnsignedInteger(this.fee)));
    // sequenceNumber
    Int64 sequenceNumberUint = new Int64();
    sequenceNumberUint.setInt64(sequenceNumber);
    SequenceNumber sequenceNumber = new SequenceNumber();
    sequenceNumber.setSequenceNumber(sequenceNumberUint);
    // operations
    org.stellar.sdk.xdr.Operation[] operations =
        new org.stellar.sdk.xdr.Operation[this.operations.length];
    for (int i = 0; i < this.operations.length; i++) {
      operations[i] = this.operations[i].toXdr();
    }
    // ext
    org.stellar.sdk.xdr.Transaction.TransactionExt ext =
        new org.stellar.sdk.xdr.Transaction.TransactionExt();
    if (this.sorobanData != null) {
      ext.setDiscriminant(1);
      ext.setSorobanData(this.sorobanData);
    } else {
      ext.setDiscriminant(0);
    }

    org.stellar.sdk.xdr.Transaction v1Tx = new org.stellar.sdk.xdr.Transaction();
    v1Tx.setFee(fee);
    v1Tx.setSeqNum(sequenceNumber);
    v1Tx.setSourceAccount(StrKey.encodeToXDRMuxedAccount(sourceAccount));
    v1Tx.setOperations(operations);
    v1Tx.setMemo(MemoUtil.toXdr(memo));
    v1Tx.setCond(preconditions.toXdr());
    v1Tx.setExt(ext);

    return v1Tx;
  }

  public static Transaction fromV0EnvelopeXdr(TransactionV0Envelope envelope, Network network) {
    long mFee = envelope.getTx().getFee().getUint32().getNumber();
    Long mSequenceNumber = envelope.getTx().getSeqNum().getSequenceNumber().getInt64();
    Memo mMemo = Memo.fromXdr(envelope.getTx().getMemo());
    TimeBounds mTimeBounds = TimeBounds.fromXdr(envelope.getTx().getTimeBounds());

    Operation[] mOperations = new Operation[envelope.getTx().getOperations().length];
    for (int i = 0; i < envelope.getTx().getOperations().length; i++) {
      mOperations[i] = Operation.fromXdr(envelope.getTx().getOperations()[i]);
    }

    Transaction transaction =
        new Transaction(
            StrKey.encodeEd25519PublicKey(envelope.getTx().getSourceAccountEd25519().getUint256()),
            mFee,
            mSequenceNumber,
            mOperations,
            mMemo,
            TransactionPreconditions.builder().timeBounds(mTimeBounds).build(),
            null,
            network);
    transaction.setEnvelopeType(EnvelopeType.ENVELOPE_TYPE_TX_V0);

    transaction.signatures.addAll(Arrays.asList(envelope.getSignatures()));

    return transaction;
  }

  public static Transaction fromV1EnvelopeXdr(TransactionV1Envelope envelope, Network network) {
    long mFee = envelope.getTx().getFee().getUint32().getNumber();
    Long mSequenceNumber = envelope.getTx().getSeqNum().getSequenceNumber().getInt64();
    Memo mMemo = Memo.fromXdr(envelope.getTx().getMemo());

    Operation[] mOperations = new Operation[envelope.getTx().getOperations().length];
    for (int i = 0; i < envelope.getTx().getOperations().length; i++) {
      mOperations[i] = Operation.fromXdr(envelope.getTx().getOperations()[i]);
    }

    SorobanTransactionData sorobanData = envelope.getTx().getExt().getSorobanData();

    Transaction transaction =
        new Transaction(
            StrKey.encodeMuxedAccount(envelope.getTx().getSourceAccount()),
            mFee,
            mSequenceNumber,
            mOperations,
            mMemo,
            TransactionPreconditions.fromXdr(envelope.getTx().getCond()),
            sorobanData,
            network);

    transaction.signatures.addAll(Arrays.asList(envelope.getSignatures()));

    return transaction;
  }

  /** Generates TransactionEnvelope XDR object. */
  @Override
  public TransactionEnvelope toEnvelopeXdr() {
    TransactionEnvelope xdr = new TransactionEnvelope();
    DecoratedSignature[] signatures = new DecoratedSignature[this.signatures.size()];
    signatures = this.signatures.toArray(signatures);

    if (this.envelopeType == EnvelopeType.ENVELOPE_TYPE_TX) {
      TransactionV1Envelope v1Envelope = new TransactionV1Envelope();
      xdr.setDiscriminant(EnvelopeType.ENVELOPE_TYPE_TX);
      v1Envelope.setTx(this.toV1Xdr());
      v1Envelope.setSignatures(signatures);
      xdr.setV1(v1Envelope);
    } else if (this.envelopeType == EnvelopeType.ENVELOPE_TYPE_TX_V0) {
      TransactionV0Envelope v0Envelope = new TransactionV0Envelope();
      xdr.setDiscriminant(EnvelopeType.ENVELOPE_TYPE_TX_V0);
      v0Envelope.setTx(this.toXdr());
      v0Envelope.setSignatures(signatures);
      xdr.setV0(v0Envelope);
    } else {
      throw new IllegalStateException("invalid envelope type: " + this.envelopeType);
    }

    return xdr;
  }

  /**
   * Returns true if this transaction is a Soroban transaction.
   *
   * @return true if this transaction is a Soroban transaction.
   */
  public boolean isSorobanTransaction() {
    if (operations.length != 1) {
      return false;
    }

    Operation op = operations[0];
    return op instanceof InvokeHostFunctionOperation
        || op instanceof ExtendFootprintTTLOperation
        || op instanceof RestoreFootprintOperation;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    Transaction that = (Transaction) object;
    return Arrays.equals(signatureBase(), that.signatureBase());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(signatureBase());
  }

  /**
   * 获取未签名交易
   * @param publicKeys 签名signer对应的公钥，Hex格式，若涉及多签则list中存入多个
   * @return 未签名交易的序列化字符串, 待签名的txHash字符串, 签名signer对应的公钥
   * @throws IOException
   */
  public NeedSignTransactionDTO getUnsignedTransaction(List<String> publicKeys) throws IOException {
    byte[] xdrByteArray = toEnvelopeXdr().toXdrByteArray();
    List<NeedSignSignatureDTO> unsignedSigs = publicKeys.stream()
            .map(e -> new NeedSignSignatureDTO(this.hashHex(), e))
            .collect(Collectors.toList());
    return new NeedSignTransactionDTO(Util.bytesToHex(xdrByteArray), unsignedSigs);
  }

  /**
   *
   * @param unsignedTransaction 未签名交易序列化, Hex格式
   * @param signedSigDTOs 已签名的txHash和对应的公钥
   * @param network 测试网或公网
   * @return SignedTransactionDTO 包含离线计算的txHash和可提交上链的已签名序列化交易
   * @throws IOException
   */
  public static SignedTransactionDTO getSignedTransaction(String unsignedTransaction, List<SignedSignatureDTO> signedSigDTOs, Network network) throws IOException {
    TransactionEnvelope transactionEnvelope = null;
    try {
      transactionEnvelope = TransactionEnvelope.fromXdrByteArray(Util.hexToBytes(unsignedTransaction));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
    // 还原Transaction对象
    Transaction transaction = (Transaction) AbstractTransaction.fromEnvelopeXdr(transactionEnvelope, network);
    // 添加已签名txHash
    for (SignedSignatureDTO signedSig : signedSigDTOs) {
      byte[] signatureBytes = Util.hexToBytes(signedSig.getSignedTxHash()); // 外部签名结果
      byte[] signerPublicKey = Util.hexToBytes(signedSig.getPublickKey()); // 32字节的 Ed25519 公钥

      // 创建 Signature 对象
      Signature signature = new Signature();
      signature.setSignature(signatureBytes);

      // 生成 SignatureHint（signer 公钥的后 4 个字节）
      byte[] hintBytes = Arrays.copyOfRange(signerPublicKey, signerPublicKey.length - 4, signerPublicKey.length);
      SignatureHint signatureHint = new SignatureHint();
      signatureHint.setSignatureHint(hintBytes);

      // 构造 DecoratedSignature
      DecoratedSignature decoratedSignature = new DecoratedSignature();
      decoratedSignature.setHint(signatureHint);
      decoratedSignature.setSignature(signature);

      // 添加到交易
      transaction.addSignature(decoratedSignature);
    }
    SignedTransactionDTO signedTransactionDTO = new SignedTransactionDTO();
    signedTransactionDTO.setTxHash(transaction.hashHex());
    signedTransactionDTO.setSignedTransaction(transaction.toEnvelopeXdrBase64());
    return signedTransactionDTO;
  }


}
