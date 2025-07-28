package com.safeheron.stellar.entity;

import com.safeheron.stellar.enums.TransactionStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.stellar.sdk.operations.PaymentOperation;

import java.util.List;

/**
 * @Author Allenzsy
 * @Date 2025/7/21 17:27
 * @Description:
 */
@Getter
@Setter
@ToString
public class TransactionVO {


    /**
     * 交易状态
     * ref: @see <a href="https://developers.stellar.org/docs/data/rpc/api-reference/methods/getTransaction" target="_blank">getTransaction documentation</a>
     */
    TransactionStatus status;

    /** 交易 Hash */
    String txHash;

    /** Stellar RPC 处理本次请求时的最新账本序号 */
    Long latestLedger;

    /** Stellar RPC 处理本次请求时的最新账本的关闭时间 unix timestamp */
    Long latestLedgerCloseTime;

    /** Stellar RPC 处理本次请求时的最早账本序号 */
    Long oldestLedger;

    /** Stellar RPC 处理本次请求时的最早账本的关闭时间 unix timestamp */
    Long oldestLedgerCloseTime;

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

    /** 手续费 */
    Long gasFee;

    /** 链上执行失败的错误信息, 从 resultXdr 解析得到*/
    String chainMsg;

    /** 解析出 from 地址 */
    String fromAddress;

    /** 如果包含 payment 操作, 解析出 PaymentOperation 包含目的地址, 交易币种和金额 */
    List<PaymentOperation> payments;

    ///** 解析出 to 地址 */
    //String toAddress;
    ///** 金额，最小单位 */
    //String value;
    ///**  批量转账相同txHash 有不一样的index */
    //String transactionIdex;




}
