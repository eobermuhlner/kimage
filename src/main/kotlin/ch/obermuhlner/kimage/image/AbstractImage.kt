package ch.obermuhlner.kimage.image

abstract class AbstractImage(
    override val width: Int,
    override val height: Int,
    override val channels: List<Channel>
) : Image {
    private val channelToIndex = IntArray(Channel.values().size) {
        channels.indexOf(Channel.values()[it])
    }

    override fun channelIndex(channel: Channel) = channelToIndex[channel.ordinal]
}