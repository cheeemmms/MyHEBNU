package com.myhebnu.data.remote.opac

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 汇文 OPAC 移动端接口。
 *
 * 所有路径都相对于 `https://hwdw.hebtu.edu.cn/meta-local/opac/`。
 * 只保留当前产品需要、且已通过抓包确认过响应结构的端点。
 */
interface OpacApi {

    // ---------------- 读者 ----------------

    @GET("users/info")
    suspend fun userInfo(): OpacEnvelope<OpacUserInfo>

    @GET("users/stats")
    suspend fun stats(): OpacEnvelope<OpacStats>

    /** 借阅额度，返回形如 `"3 / 20"` 的字符串。 */
    @GET("users/loan_info")
    suspend fun loanInfo(): OpacEnvelope<String>

    /** 动态借书码：14 位（`EC` + 12 位大写字母数字），每次请求都会变。 */
    @GET("users/elec")
    suspend fun elecCode(): OpacEnvelope<String>

    // ---------------- 借阅 ----------------

    @GET("users/loans")
    suspend fun loans(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): OpacEnvelope<OpacPage<OpacLoan>>

    @GET("users/overdue_soon_page")
    suspend fun overdueSoon(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): OpacEnvelope<OpacPage<OpacLoan>>

    @GET("users/fines")
    suspend fun fines(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): OpacEnvelope<OpacPage<OpacRequest>>

    /** 近 12 个月的借阅量 `{"2026-10": 7, ...}`。 */
    @GET("users/loan_range_dist")
    suspend fun loanRangeDistribution(): OpacEnvelope<Map<String, Int>>

    /** 借阅分类分布。 */
    @GET("users/loan_dcp_dist")
    suspend fun loanCategoryDistribution(): OpacEnvelope<List<OpacDistribution>>

    // ---------------- 预约 / 荐购 ----------------

    @GET("users/requests/list")
    suspend fun requestList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("requestFlagStatus") flagStatus: String = "all",
        @Query("processStatus") processStatus: Int = 0
    ): OpacEnvelope<OpacPage<OpacRequest>>

    @GET("users/requests")
    suspend fun suggestions(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("type") type: String = "req.acq.suggest"
    ): OpacEnvelope<OpacPage<OpacRequest>>

    // ---------------- 检索 ----------------

    @POST("search/")
    suspend fun search(@Body body: OpacSearchRequest): OpacEnvelope<OpacSearchPage>

    @GET("search/_suggest")
    suspend fun suggest(
        @Query("fieldName") fieldName: String = "all",
        @Query("query") query: String,
        @Query("size") size: Int = 8
    ): OpacEnvelope<List<String>>

    @GET("search_hist/user")
    suspend fun searchHistory(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 8,
        @Query("validLimit") validLimit: Int = 1,
        @Query("isMobile") isMobile: Int = 1
    ): OpacEnvelope<OpacPage<OpacSearchHistory>>

    /**
     * 批量取封面。
     *
     * 检索结果的 `thumb` 恒为空串，官方前端是在拿到结果后再打这个接口补封面；
     * 返回结构是以 bibId 为键的映射，值是该书在豆瓣/汇文图床上的封面列表。
     */
    @POST("search/extend3")
    suspend fun covers(
        @Query("coversOnly") coversOnly: Boolean = true,
        @Body body: OpacCoversRequest
    ): OpacEnvelope<Map<String, List<OpacCoverRef>>>

    // ---------------- 书目 ----------------

    @GET("bibs/{bibId}/infos")
    suspend fun bibInfo(
        @Path("bibId") bibId: String,
        @Query("detail") detail: Boolean = true,
        @Query("isMobile") isMobile: Int = 1
    ): OpacEnvelope<OpacBibDetail>

    @GET("bibs/{bibId}/holdings")
    suspend fun bibHoldings(
        @Path("bibId") bibId: String,
        @Query("isMobile") isMobile: Int = 1,
        @Query("relateStat") relateStat: Int = 1
    ): OpacEnvelope<Map<String, String>>

    /** 豆瓣副数据；`isbn` 可能带连字符，原样传入。 */
    @GET("third_api/douban/{isbn}/{bibId}/info")
    suspend fun doubanInfo(
        @Path("isbn") isbn: String,
        @Path("bibId") bibId: String,
        @Query("title") title: String? = null
    ): OpacEnvelope<OpacDouban>

    // ---------------- 推荐 ----------------

    @GET("new/books")
    suspend fun newBooks(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 5
    ): OpacEnvelope<OpacSearchPage>

    @GET("commend/hot1")
    suspend fun hotBooks(@Query("count") count: Int = 5): OpacEnvelope<List<OpacHotBook>>
}
