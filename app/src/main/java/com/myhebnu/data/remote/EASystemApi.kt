package com.myhebnu.data.remote

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

/**
 * 新方正教务管理系统 API 接口
 *
 * 所有接口均使用 FormUrlEncoded 方式传参，响应为 JSON。
 * 基础 URL: http://jwgl.hebtu.edu.cn/
 */
interface EASystemApi {

    // === 权限门控：注册菜单点击 ===
    @POST("/xtgl/index_cxBczjsygnmk.html")
    @FormUrlEncoded
    suspend fun registerMenuClick(
        @Field("gndm") moduleCode: String,
        @Query("gnmkdm") gnCode: String = "index"
    ): Response<okhttp3.ResponseBody>

    // === 课表 ===

    @GET("/kbcx/xskbcx_cxXskbcxIndex.html")
    suspend fun loadSchedulePage(
        @Query("gnmkdm") moduleCode: String = "N2151",
        @Query("layout") layout: String = "default"
    ): Response<okhttp3.ResponseBody>

    @POST("/kbcx/xskbcx_cxXsgrkb.html")
    @FormUrlEncoded
    suspend fun getSchedule(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("kzlx") type: String = "ck",
        @Field("xsdm") studentDept: String = "",
        @Field("kclbdm") courseCategory: String = "",
        @Field("kclxdm") courseType: String = "",
        @Query("gnmkdm") moduleCode: String = "N2151"
    ): Response<JsonObject>

    @POST("/kbcx/xskbcx_cxRjc.html")
    @FormUrlEncoded
    suspend fun getPeriodList(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("xqh_id") campusId: String,
        @Query("gnmkdm") moduleCode: String = "N2151"
    ): Response<List<JsonObject>>

    // === 成绩 ===

    @POST("/cjcx/cjcx_cxXsKcList.html")
    @FormUrlEncoded
    suspend fun getGradeList(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Query("gnmkdm") moduleCode: String = "N305007"
    ): Response<JsonObject>

    @POST("/cjcx/cjcx_cxXsXmcjList.html")
    @FormUrlEncoded
    suspend fun getGradeDetail(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("jxb_id") classId: String,
        @Query("gnmkdm") moduleCode: String = "N305007"
    ): Response<JsonObject>

    // === 空教室 ===

    @GET("/cdjy/cdjy_cxKxcdlb.html")
    suspend fun loadRoomPage(
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<okhttp3.ResponseBody>

    @GET("/cdjy/cdjy_cxXqjc.html")
    suspend fun getCampusBuildingInfo(
        @Query("xqh_id") campusId: String,
        @Query("xnm") year: String,
        @Query("xqm") semester: String,
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<okhttp3.ResponseBody>

    @POST("/cdjy/cdjy_cxKxcdlb.html")
    @FormUrlEncoded
    suspend fun getEmptyRooms(
        @Field("xqh_id") campusId: String,
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("cdlb_id") venueType: String = "02",
        @Field("lh") building: String,
        @Field("zcd") weekBitmask: String,
        @Field("xqj") dayOfWeek: String,
        @Field("jcd") periodBitmask: String,
        @Field("cdejlb_id") subVenueType: String = "",
        @Field("qszws") minSeats: String = "",
        @Field("jszws") maxSeats: String = "",
        @Field("cdmc") roomName: String = "",
        @Field("jyfs") usageMode: String = "0",
        @Field("cdjylx") usageType: String = "",
        @Field("sfbhkc") includeExam: String = "",
        @Query("doType") doType: String = "query",
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<okhttp3.ResponseBody>

    @POST("/pkglcommon/common_cxZcdesc.html")
    @FormUrlEncoded
    suspend fun getWeekDescription(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("zcd") weekBitmask: String,
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<String>

    @POST("/pkglcommon/common_cxJcdesc.html")
    @FormUrlEncoded
    suspend fun getPeriodDescription(
        @Field("jc") periodBitmask: String,
        @Query("gnmkdm") moduleCode: String = "N2155"
    ): Response<String>

    // === 周次课表 (N2154) ===

    @GET("/kbcx/xskbcxZccx_cxXskbcxIndex.html")
    suspend fun loadWeekSchedulePage(
        @Query("gnmkdm") moduleCode: String = "N2154",
        @Query("layout") layout: String = "default"
    ): Response<okhttp3.ResponseBody>

    @POST("/kbcx/xskbcxZccx_cxZcByXnxq.html")
    @FormUrlEncoded
    suspend fun getWeeksBySemester(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Query("gnmkdm") moduleCode: String = "N2154"
    ): Response<okhttp3.ResponseBody>

    // === 考试 ===

    @POST("/kwgl/kscx_cxXsksxxIndex.html")
    @FormUrlEncoded
    suspend fun getExams(
        @Field("xnm") year: String,
        @Field("xqm") semester: String,
        @Field("ksmcdmb_id") examTypeId: String = "",
        @Field("kch") courseCode: String = "",
        @Field("kc") courseName: String = "",
        @Field("ksrq") examDate: String = "",
        @Field("kkbm_id") departmentId: String = "",
        @Query("doType") doType: String = "query",
        @Query("gnmkdm") moduleCode: String = "N358105"
    ): Response<JsonObject>
}
