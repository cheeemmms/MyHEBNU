package com.myhebnu.data.remote.opac

import com.google.gson.annotations.SerializedName

/**
 * 汇文 OPAC 接口的响应模型。
 *
 * 说明：这里的字段**全部声明为可空**，因为服务端会对不同分馆/不同权限的读者返回不同子集；
 * 用非空类型 + 默认值会在字段缺失时被 Gson 静默填成 null 并在取值处抛 NPE。
 * 展示层统一在 ViewModel 里做 `?:` 兜底。
 */

/** 统一响应包：`{"code":0,"msg":null,"data":...}`；失败时 msg 直接是中文提示。 */
data class OpacEnvelope<T>(
    val code: Int? = null,
    val msg: String? = null,
    val data: T? = null
)

/** 分页数据（借阅、罚金、预约等通用）。 */
data class OpacPage<T>(
    val currentPage: Int? = null,
    val pageSize: Int? = null,
    val total: Int? = null,
    val totalPage: Int? = null,
    val items: List<T>? = null
)

// ---------------------------------------------------------------------------
// 读者
// ---------------------------------------------------------------------------

data class OpacUserInfo(
    val uid: Int? = null,
    val userId: String? = null,
    val displayName: String? = null,
    val dept: String? = null,
    val department: String? = null,
    val userClass: String? = null,
    val userGroupDesc: String? = null,
    val occupation: String? = null,
    val grade: String? = null,
    val genderDesc: String? = null,
    val credit: Int? = null,
    val idCard: String? = null,
    val idStatusDesc: String? = null,
    val idExpireDate: String? = null,
    val expireDate: String? = null,
    val regDate: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val libCodeDesc: String? = null,
    val userStatusDesc: String? = null
)

data class OpacStats(
    val loanCount: Int? = null,
    val expireCount: Int? = null,
    val fineSum: Double? = null,
    val requestCount: Int? = null,
    val booklistCount: Int? = null,
    val deptRange: Int? = null,
    val range: Int? = null,
    val unprocessedCount: Int? = null
)

// ---------------------------------------------------------------------------
// 借阅
// ---------------------------------------------------------------------------

data class OpacLoan(
    val bibId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val dueDate: String? = null,
    val loanDate: String? = null,
    val renewTimes: Int? = null,
    val loanStatus: String? = null,
    val loanTypeDesc: String? = null,
    val location: String? = null,
    val loanId: String? = null,
    val barCode: String? = null,
    val bibAttrs: OpacBibAttrs? = null,
    val item: OpacLoanItem? = null
)

data class OpacLoanItem(
    val barCode: String? = null,
    val callNo: String? = null,
    val attachment: String? = null
)

/** 借阅条目内嵌的书目属性（索书号、ISBN、出版社等）。 */
data class OpacBibAttrs(
    val isbn: String? = null,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val pub_year: String? = null,
    val callno: String? = null,
    val classno: String? = null,
    val price: String? = null,
    val abstract: String? = null,
    val edition: String? = null,
    val pageNum: String? = null,
    val publish_place: String? = null
)

/** 借阅分类分布项；`class` 是 Kotlin 关键字，映射为 [category]。 */
data class OpacDistribution(
    @SerializedName("class") val category: String? = null,
    val count: Int? = null
)

// ---------------------------------------------------------------------------
// 检索与书目
// ---------------------------------------------------------------------------

/** 检索结果单条（`search/`、`new/books` 共用字段的超集）。 */
data class OpacSearchItem(
    val bibId: String? = null,
    val groupId: String? = null,
    val bibNo: String? = null,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val pub_year: String? = null,
    val isbn: String? = null,
    val classno: String? = null,
    val callno: List<String>? = null,
    val abstract: String? = null,
    val docType: String? = null,
    val docTypeDesc: String? = null,
    val itemCount: Int? = null,
    val circCount: Int? = null,
    val thumb: String? = null,
    val holdingTypes: List<String>? = null,
    val onSelfDate: String? = null,
    val inBooklist: Int? = null,
    val isOpen: Int? = null,
    /** **字符串化 JSON**，需二次解析为 `List<OpacHolding>`。 */
    val holdings: String? = null,
    val eholdings: String? = null,
    val tags: List<String>? = null
)

data class OpacSearchPage(
    val total: Int? = null,
    val actualTotal: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val tookTime: Double? = null,
    val dataList: List<OpacSearchItem>? = null,
    val facetList: List<OpacFacet>? = null
)

/** 批量封面请求：`{"isbns":[...],"bibIds":[...]}`，两者都可只给其一。 */
data class OpacCoversRequest(
    val isbns: List<String> = emptyList(),
    val bibIds: List<String> = emptyList()
)

/** 批量封面返回项；`_id` 是 ISBN，`imageUrl` 可能是协议相对地址 `//host/..`。 */
data class OpacCoverRef(
    @SerializedName("_id") val id: String? = null,
    val imageUrl: String? = null
)

/** 检索分面（当前 UI 未使用，保留字段以免响应解析失败）。 */
data class OpacFacet(
    val code: String? = null,
    val name: String? = null,
    val facetFields: List<OpacFacetField>? = null
)

data class OpacFacetField(
    val value: String? = null,
    val name: String? = null,
    val count: Int? = null
)

/** 馆藏单册。 */
data class OpacHolding(
    val callNo: String? = null,
    val barCode: String? = null,
    val dueDate: String? = null,
    val location: String? = null,
    val locationName: String? = null,
    val library: String? = null,
    val status: String? = null,
    val itemStatus: String? = null,
    val circStatus: Int? = null,
    val itemsCount: Int? = null,
    val itemsAvailable: Int? = null,
    val totalCircTimes: Int? = null,
    val yearCircTimes: Int? = null,
    val locaCode: String? = null,
    val tempLocation: String? = null
)

/** 书目详情：服务端返回 `{map:{baseInfo:{map:..},detailInfo:{map:..},extraInfo:{map:..}}}`。 */
data class OpacBibDetail(
    val map: OpacBibSections? = null
)

data class OpacBibSections(
    val baseInfo: OpacMapWrapper<OpacBibBase>? = null,
    val detailInfo: OpacMapWrapper<Map<String, String>>? = null,
    val extraInfo: OpacMapWrapper<OpacBibExtra>? = null
)

data class OpacMapWrapper<T>(
    val map: T? = null,
    val empty: Boolean? = null
)

data class OpacBibBase(
    val title: String? = null,
    val author: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val pubYear: String? = null,
    val docType: String? = null,
    val callno: List<String>? = null,
    val tags: List<String>? = null,
    val barCodes: List<String>? = null
)

data class OpacBibExtra(
    val score: Double? = null,
    val _clickCount: Int? = null,
    val isElectric: Boolean? = null,
    val collected: Boolean? = null
)

/** 豆瓣副数据（简介、目录、封面）。`imageUrl` 可能是协议相对地址 `//host/..`。 */
data class OpacDouban(
    val _id: String? = null,
    val title: String? = null,
    val authorIntroduction: String? = null,
    val catalog: String? = null,
    val content: String? = null,
    val imageUrl: String? = null
)

data class OpacHotBook(
    val bibId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val pub_year: String? = null,
    val callno: String? = null,
    val isbn: String? = null,
    val bibNo: String? = null,
    val rank: Double? = null,
    val hotValue: Int? = null
)

data class OpacSearchHistory(
    val _id: String? = null,
    val field: String? = null,
    val query: String? = null,
    val resultCount: Int? = null,
    /** 毫秒时间戳。 */
    val date: Long? = null,
    val isValid: Int? = null,
    val isMobile: Int? = null
)

// ---------------------------------------------------------------------------
// 预约 / 委托 / 荐购
// ---------------------------------------------------------------------------

/** 请求记录（`users/requests/list` 与 `users/requests?type=req.acq.suggest` 的并集）。 */
data class OpacRequest(
    val requestId: Int? = null,
    val requestType: String? = null,
    val requestTypeDesc: String? = null,
    val requestFlag: Int? = null,
    val requestFlagDesc: String? = null,
    val processStatus: String? = null,
    val processStatusDesc: String? = null,
    val objectId: String? = null,
    val objectKey: String? = null,
    val objectName: String? = null,
    val objectType: String? = null,
    val requestDate: String? = null,
    val noticeDate: String? = null,
    val expireDate: String? = null,
    val arrivalDate: String? = null,
    val arrivalExpireDate: String? = null,
    val keepDays: Int? = null,
    val note: String? = null,
    val replyContent: String? = null,
    val replyDate: String? = null,
    val isReplied: Int? = null,
    val received: Int? = null,
    /** **字符串化 JSON**，含 isbn/title/author/publisher 等。 */
    val requestAttrs: String? = null
)

// ---------------------------------------------------------------------------
// 请求体
// ---------------------------------------------------------------------------

data class OpacQueryField(
    val logic: Int = 0,
    val field: String = "all",
    val values: List<String>,
    val operator: String = "*"
)

data class OpacSearchRequest(
    val page: Int,
    val pageSize: Int,
    val indexName: String = "idx.opac",
    val sortField: String = "relevance",
    val sortType: String = "desc",
    val collapseField: String = "groupId",
    val queryFieldList: List<OpacQueryField>,
    val filterFieldList: List<OpacQueryField> = emptyList()
)

/** 接口业务错误；[message] 是服务端中文提示，可直接展示。 */
class OpacApiException(message: String) : Exception(message)
