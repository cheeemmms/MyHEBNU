package com.myhebnu.data.remote.sso

import java.math.BigInteger

/**
 * 学校 CAS 使用的「正方 RSAUtils」加密算法。
 *
 * 这**不是**标准 `RSA/ECB/PKCS1Padding`，而是正方 `security.js` 里那套自定义封装：
 * 把字符按「16 位小端字」打包成一个大整数，高位补 0 到块长的整数倍，再对每块做
 * `modPow(e, n)`，最后逐块输出**不带前导零的小写 hex**，多块之间用空格连接。
 *
 * 与标准 PKCS#1 的区别（这也是最初登录一直提示密码错误的原因）：
 * - 没有 `00 02 FF..` 填充头，服务端解出来就是原始字符序列；
 * - 字节不是大端连续排列，而是每 2 个字符拼成一个 16 位小端字；
 * - 密码要先**整串反转**再进算法（网页 `checkForm()` 里的
 *   `password.split("").reverse().join("")`）。
 *
 * 该实现已用抓包样本做过逐字节校验：给定抓到的 modulus 与明文，
 * 输出与浏览器提交的 `password` 字段**完全一致**。
 */
object ZfsoftRsa {

    /**
     * 加密一段文本。
     *
     * @param plainText 明文（内部会先做整串反转，调用方不要自己反转）
     * @param modulusHex `getPubKey` 返回的 modulus（hex，长度会变，512/768/1024 位都见过）
     * @param exponentHex `getPubKey` 返回的 exponent（通常是 `10001`）
     * @return hex 字符串；密文过长时以空格分隔的多块
     */
    fun encrypt(plainText: String, modulusHex: String, exponentHex: String): String {
        val modulus = BigInteger(modulusHex.trim(), 16)
        val exponent = BigInteger(exponentHex.trim().ifBlank { "10001" }, 16)

        // 块长：与 JS 的 chunkSize = 2 * biHighIndex(modulus) 等价（以字符计）
        val digitCount = (modulus.bitLength() + 15) / 16
        val chunkSize = 2 * (digitCount - 1)
        require(chunkSize > 1) { "公钥长度异常: ${modulus.bitLength()} bit" }

        // 明文先整串反转，再按块长补零（与 JS 的 a[i++] = 0 一致）
        val chars = plainText.reversed().toCharArray()
        val padded = chars.copyOf(((chars.size + chunkSize - 1) / chunkSize) * chunkSize)

        val blocks = ArrayList<String>(padded.size / chunkSize)
        var offset = 0
        while (offset < padded.size) {
            var value = BigInteger.ZERO
            for (j in 0 until chunkSize / 2) {
                val low = padded[offset + 2 * j].code
                val high = padded[offset + 2 * j + 1].code
                value = value.add(
                    BigInteger.valueOf((low + (high shl 8)).toLong()).shiftLeft(16 * j)
                )
            }
            blocks.add(value.modPow(exponent, modulus).toString(16))
            offset += chunkSize
        }
        return blocks.joinToString(" ")
    }
}
