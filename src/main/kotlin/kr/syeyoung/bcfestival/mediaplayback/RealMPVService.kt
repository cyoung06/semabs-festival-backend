package kr.syeyoung.bcfestival.mediaplayback

import io.github.macfja.mpv.Service
import io.github.macfja.mpv.communication.handling.MessageHandlerInterface
import java.io.IOException
import java.util.function.Consumer

class RealMPVService(socketPath: String) : Service() {
    init {
        val prev = ioCommunication.messageHandlers
        ioCommunication = MPVCommunicationInterface(socketPath, this)
        prev.forEach(Consumer { messageHandler: MessageHandlerInterface? ->
            ioCommunication.addMessageHandler(
                messageHandler
            )
        })
        ioCommunication.setExitOnClose(false)
        ioCommunication.setSocketPath(socketPath)
    }

    override fun initialize() {
        try {
            ioCommunication.open()
        } catch (var2: IOException) {
            logger.error("Unable to start communication", var2)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        ioCommunication.close()
    }
}