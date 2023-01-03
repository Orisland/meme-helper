package xyz.cssxsh.mirai.meme.face

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import net.mamoe.mirai.internal.*
import net.mamoe.mirai.internal.network.*
import net.mamoe.mirai.internal.message.data.*
import net.mamoe.mirai.internal.network.protocol.data.proto.*
import xyz.cssxsh.mirai.meme.*
import java.util.*

@MiraiExperimentalApi
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public object MarketFaceHelper {
    @PublishedApi
    internal val json: Json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }
    @PublishedApi
    internal val authors: MutableMap<Long, AuthorDetail> = WeakHashMap()
    @PublishedApi
    internal val relations: MutableMap<Int, RelationIdInfo> = WeakHashMap()
    @PublishedApi
    internal val faces: MutableMap<Int, MarketFaceData> = WeakHashMap()

    public suspend fun queryAuthorDetail(authorId: Long): AuthorDetail {
        val cache = authors[authorId]
        if (cache != null) return cache

        val bot = Bot.instances.randomOrNull() ?: throw IllegalStateException("No Bot Instance")
        bot as QQAndroidBot
        val text = http.get("https://open.vip.qq.com/open/getAuthorDetail") {
            parameter("authorId", authorId)
            parameter("g_tk", bot.client.wLoginSigInfo.bkn)

            headers {
                // ktor bug
                append(
                    "cookie",
                    "uin=o${bot.id}; skey=${bot.sKey}; p_uin=o${bot.id}; p_skey=${bot.psKey("vip.qq.com")};"
                )
            }
        }.bodyAsText()

        val result = Json.decodeFromString(Restful.serializer(), text)

        check(result.ret == 0) { result.msg }

        val detail = json.decodeFromJsonElement(AuthorDetail.serializer(), result.data)
        authors[authorId] = detail
        return detail
    }

    public suspend fun queryRelationId(itemId: Int): RelationIdInfo {
        val cache = relations[itemId]
        if (cache != null) return cache

        val bot = Bot.instances.randomOrNull() ?: throw IllegalStateException("No Bot Instance")
        bot as QQAndroidBot
        val text = http.get("https://open.vip.qq.com/open/getRelationId") {
            parameter("appId", "1")
            parameter("adminItemId", itemId)
            parameter("g_tk", bot.client.wLoginSigInfo.bkn)

            headers {
                // ktor bug
                append(
                    "cookie",
                    "uin=o${bot.id}; skey=${bot.sKey}; p_uin=o${bot.id}; p_skey=${bot.psKey("vip.qq.com")};"
                )
            }
        }.bodyAsText()

        val result = Json.decodeFromString(Restful.serializer(), text)

        check(result.ret == 0) { result.msg }
        val info = json.decodeFromJsonElement(RelationIdInfo.serializer(), result.data)
        relations[itemId] = info
        return info
    }

    public suspend fun queryRelationId(face: MarketFace): RelationIdInfo = queryRelationId(itemId = face.id)

    public suspend fun queryFaceDetail(itemId: Int): MarketFaceData {
        val cache = faces[itemId]
        if (cache != null) return cache

        val text = http.get("https://gxh.vip.qq.com/qqshow/admindata/comdata/vipEmoji_item_209583/xydata.json") {
            url {
                encodedPath = encodedPath.replace("209583", itemId.toString())
            }
        }.bodyAsText()

        val data = json.decodeFromString(MarketFaceData.serializer(), text)
        faces[itemId] = data
        return data
    }

    public suspend fun queryFaceDetail(face: MarketFace): MarketFaceData = queryFaceDetail(itemId = face.id)

    public suspend fun queryItemData(appId: Int, itemId: Int): String {
        val bot = Bot.instances.randomOrNull() ?: throw IllegalStateException("No Bot Instance")
        bot as QQAndroidBot
        val text = http.get("https://zb.vip.qq.com/v2/home/cgi/getItemData") {
            parameter("bid", appId)
            parameter("id", itemId)
            parameter("g_tk", bot.client.wLoginSigInfo.bkn)

            headers {
                // ktor bug
                append(
                    "cookie",
                    "uin=o${bot.id}; skey=${bot.sKey}; p_uin=o${bot.id}; p_skey=${bot.psKey("vip.qq.com")};"
                )
            }
        }.bodyAsText()

        return text
    }

    public fun build(itemId: Int, data: MarketFaceData): List<MarketFace> {
        val info = data.detail.base.single()
        val timestamp = requireNotNull(data.timestamp) { "Not Found Timestamp" } / 1_000
        val key = timestamp.toString().md5()
            .toUHexString("")
            .lowercase().substring(0, 16)
            .toByteArray()
        val default = "0A 06 08 AC 02 10 AC 02 0A 06 08 C8 01 10 C8 01 40 01".hexToBytes()

        return data.detail.md5.map { (md5, name) ->
            val delegate = ImMsgBody.MarketFace(
                faceName = "[$name]".toByteArray(),
                itemType = 6,
                faceInfo = info.feeType,
                faceId = md5.hexToBytes(),
                tabId = itemId,
                subType = info.type,
                key = key,
                imageWidth = 200,
                imageHeight = 200,
                pbReserve = default
            )

            MarketFaceImpl(delegate = delegate)
        }
    }
}