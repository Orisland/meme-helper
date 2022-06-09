package xyz.cssxsh.mirai.meme.impl

import kotlinx.coroutines.*
import net.mamoe.mirai.console.permission.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import xyz.cssxsh.mirai.meme.*
import xyz.cssxsh.mirai.meme.service.*
import xyz.cssxsh.skia.*
import java.io.File
import java.util.*

public class MemeDear : MemeService {
    override val name: String = "Dear"
    override val id: String = "dear"
    override val description: String = "亲亲 生成器"
    override var loaded: Boolean = false
        private set
    override var regex: Regex = """^#dear\s*(\d+)?""".toRegex()
        private set
    override val properties: Properties = Properties().apply { put("regex", regex.pattern) }
    override var permission: Permission = Permission.getRootPermission()
        private set

    override fun load(folder: File, permission: Permission) {
        this.permission = permission
        when (val re = properties["regex"]) {
            is String -> regex = re.toRegex()
            is Regex -> regex = re
            else -> {}
        }
        MemeService.launch {
            val dear = folder.resolve("dear.gif")
            if (dear.exists().not()) {
                download(urlString = "https://tva3.sinaimg.cn/large/003MWcpMly8gv4s019bzsg606o06o40902.gif", folder)
                    .renameTo(dear)
            }
            System.setProperty(DEAR_ORIGIN, dear.absolutePath)

            loaded = true
        }
    }

    override fun enable() {}

    override fun disable() {}

    override suspend fun MessageEvent.replier(match: MatchResult): Image {
        val id = match.groups[1]?.value?.toLongOrNull()
            ?: message.findIsInstance<At>()?.target
            ?: sender.id
        val image = message.findIsInstance<Image>()
        val face = when {
            image != null -> cache(image = image)
            else -> avatar(id = id)
        }

        return dear(face).uploadAsImage(subject)
    }
}