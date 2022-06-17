package xyz.cssxsh.mirai.meme.impl

import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.cssxsh.mirai.meme.download
import xyz.cssxsh.mirai.meme.service.*
import xyz.cssxsh.mirai.weibo.data.*
import java.io.File
import java.util.*

public class MemeWeiboEmoticon : MemeService {
    override val name: String = "Weibo Emoticon"
    override val id: String = "weibo"
    override val description: String = "获取微博表情包"
    override var loaded: Boolean = true
    override var regex: Regex = """^#微博表情""".toRegex()
        private set
    override val properties: Properties = Properties().apply { put("regex", regex.pattern) }
    override var permission: Permission = Permission.getRootPermission()
        private set
    private var folder: File = File(System.getProperty("user.dir") ?: ".").resolve(".weibo")

    override fun load(folder: File, permission: Permission) {
        this.folder = folder
        this.permission = permission
        when (val re = properties["regex"]) {
            is String -> regex = re.toRegex()
            is Regex -> regex = re
            else -> {}
        }
        loaded = try {
            WeiboEmoticonData
            true
        } catch (_: Throwable) {
            false
        }

    }

    override fun enable() {}

    override fun disable() {}

    override suspend fun MessageEvent.replier(match: MatchResult): MessageChain? {
        val content = message.content
        val emoticons = buildList {
            for ((phrase, emoticon) in WeiboEmoticonData.emoticons) {
                if (phrase in content || "#${emoticon.category}" in content) add(emoticon)
            }
        }
        if (emoticons.isEmpty()) return null
        return buildMessageChain {
            for (emoticon in emoticons) {
                val file = folder.resolve(emoticon.url.substringAfterLast('/')).takeIf { it.exists() }
                        ?: download(urlString = emoticon.url, folder = folder)
                +file.uploadAsImage(contact = subject)
            }
        }
    }
}