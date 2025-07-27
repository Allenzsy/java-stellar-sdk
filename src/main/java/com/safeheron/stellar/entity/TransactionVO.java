package com.safeheron.stellar.entity;

import lombok.Getter;
import lombok.Setter;
import org.stellar.sdk.Transaction;

/**
 * @Author Allenzsy
 * @Date 2025/7/28 1:56
 * @Description:
 */
@Getter
@Setter
public class TransactionVO {

    /**
     * 交易状态
     * ref: @see <a href="https://developers.stellar.org/docs/data/rpc/api-reference/methods/getTransaction" target="_blank">getTransaction documentation</a>
     */
    TransactionReceiptVO.TransactionStatus status;

    /** 交易 Hash */
    String txHash;

    /**
     * 只有 status 为 SUCCESS 或 FAILED 时才有值
     * 该交易在账本所有交易中的索引
     */
    Integer applicationOrder;

    /** 交易手续费是否 fee bumped */
    Boolean feeBump;

    /**
     * base64 编码的已序列化交易
     * The field can be parsed as {@link org.stellar.sdk.xdr.TransactionEnvelope} object.
     */
    String envelopeXdr;

    /**
     * 只有 status 为 SUCCESS 或 FAILED 时才有值
     * The field can be parsed as {@link org.stellar.sdk.xdr.TransactionResult} object.
     */
    String resultXdr;

    /** The field can be parsed as {@link org.stellar.sdk.xdr.TransactionMeta} object. */
    String resultMetaXdr;

    /** 此交易所在的账本序号 */
    Long ledger;

    /** 此交易被写入账本的时间 unix timestamp */
    Long createdAt;


    Transaction transaction;


    /**
     * 手续费
     */
    Long gasFee;

    //String txHash
    //String blockHeight
    //String fromAddress
    //String toAddress
    //String value
    //String token(如果是合约token转账，写入合约地址)
    //String fee(手续费)
    //String nonce （类似）
    //        .. 其他链可能需要的字段


}
