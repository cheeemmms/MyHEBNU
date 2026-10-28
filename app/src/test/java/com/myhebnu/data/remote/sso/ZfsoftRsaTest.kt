package com.myhebnu.data.remote.sso

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 校 CAS 的 RSA 封装是正方自定义格式，不能用标准 `RSA/ECB/PKCS1Padding` 顶替。
 * 这里用固定公钥 + 固定明文的「已知答案」把算法钉住——期望值由按
 * `security.js` 逐行复刻的参考实现算出。任何一次「顺手改成 PKCS#1」都会被这组测试拦下。
 */
class ZfsoftRsaTest {

    private val modulus =
        "c975ca4c0ecda58657a7212712edf4470c13932e90fce234f7166869d195f69a0" +
            "87e1ca3ecd555d7c41ec58da88bcb739ce9b9acf69898f8ca7531a70d27a391"

    @Test
    fun `加密与参考实现逐字节一致`() {
        assertEquals(
            "bb79fc7a7f87454945b11a93a93082c818b08d7e16896a1e65bb20a99085bb5a" +
                "f5e4b859f6d553ef1f55b6e0fc2e021dcdab243a40d15c98607ac0848b9d8e34",
            ZfsoftRsa.encrypt("Zfsoft#2024test", modulus, "10001")
        )
    }

    @Test
    fun `短明文也要补齐到整块`() {
        assertEquals(
            "ad4bd45e7005d7366eaf5b02fe1252ac6b6ba7ddfdfd5b248859c91b1d1f7ab" +
                "bc5cb48d79e499c3b4e8f3d457c79e97afab433b3bf789caf09ff3f5107d3e6cd",
            ZfsoftRsa.encrypt("a", modulus, "10001")
        )
    }

    @Test
    fun `大小写与首尾空格不应影响结果`() {
        val upper = modulus.uppercase()
        assertEquals(
            ZfsoftRsa.encrypt("Zfsoft#2024test", modulus, "10001"),
            ZfsoftRsa.encrypt("Zfsoft#2024test", " $upper ", " 10001 ")
        )
    }
}
