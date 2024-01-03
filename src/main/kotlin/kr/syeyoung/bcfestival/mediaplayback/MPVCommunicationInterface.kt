package kr.syeyoung.bcfestival.mediaplayback

import com.alibaba.fastjson.JSONObject
import io.github.macfja.mpv.communication.Communication
import io.github.macfja.mpv.communication.MessagesListener
import io.github.macfja.mpv.communication.handling.MessageHandlerInterface
import io.github.macfja.mpv.communication.handling.PropertyObserver
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.Serializable
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.util.*

class MPVCommunicationInterface(path: String, val realMPVService: RealMPVService) : Communication() {
    private var channel: SocketChannel? = null
    private var exitOnClose = true
    private var socketPath: String? = null
    protected var logger = LoggerFactory.getLogger(this.javaClass)
    private var messagesListener: MessagesListener
    override fun setExitOnClose(exitOnClose: Boolean) {
        this.exitOnClose = exitOnClose
    }

    override fun setSocketPath(socketPath: String) {
        this.socketPath = socketPath
    }

    override fun addMessageHandler(messageHandler: MessageHandlerInterface) {
        this.messagesListener.addMessageHandler(messageHandler)
    }

    init {
        this.messagesListener = MessagesListener(this.logger)
        setSocketPath(path)
    }

    override fun removeMessageHandler(messageHandler: MessageHandlerInterface) {
        this.messagesListener.removeMessageHandler(messageHandler)
    }

    override fun getMessageHandlers(): List<MessageHandlerInterface> {
        return this.messagesListener.messageHandlers
    }

    override fun clearMessageHandlers() {
        this.messagesListener.clearMessageHandlers()
    }

    @Throws(IOException::class)
    private fun ensureIoReady() {
        if (channel == null || !this.messagesListener.isRunning || channel?.isOpen != true) {
            open()
        }
    }

    @Throws(IOException::class)
    override fun write(command: String, arguments: List<Serializable?>?): Int {
        ensureIoReady()
        val parameters = ArrayList<Any?>()
        parameters.add(command)
        parameters.addAll(arguments ?: Collections.EMPTY_LIST)
        val json = JSONObject()
        json["command"] = parameters
        val requestId = Math.ceil(Math.random() * 1000.0).toInt()
        json["request_id"] = requestId
        this.logger.debug("Send: " + json.toJSONString())
        val buf = ByteBuffer.wrap(
            """${json.toJSONString()}
""".toByteArray()
        )
        channel!!.write(buf)
        return requestId
    }

    override fun simulateMessage(message: JSONObject) {
        this.messagesListener.handleLine(message)
    }

    @Throws(IOException::class)
    override fun open() {
        this.logger.info("Starting processes")
        try {
            if (channel == null || !channel!!.isOpen || !this.messagesListener.isRunning) {
                this.logger.info("Start MPV communication")
                val socketAddress = UnixDomainSocketAddress.of(socketPath)
                channel = SocketChannel
                    .open(StandardProtocolFamily.UNIX)
                channel?.connect(socketAddress)
                Thread.sleep(500L)
            }
            if (!this.messagesListener.isRunning) {
                this.logger.info("Start MPV reader")
                val mhi = this.messagesListener.messageHandlers;

                this.messagesListener = MessagesListener(this.logger);
                this.messagesListener.start(Channels.newInputStream(channel))
                mhi.forEach { this.messagesListener.addMessageHandler(it) }
                mhi.filterIsInstance<PropertyObserver>().forEach {
                    this.realMPVService.sendCommand("observe_property", listOf(it.id, it.propertyName))
                }

            }
        } catch (var2: IOException) {
            this.logger.error("Unable to start communication", var2)
            channel = null
            throw var2
        } catch (var3: InterruptedException) {
            this.logger.warn("Sleeping interrupted", var3)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        try {
            if (this.exitOnClose) {
                write("exit",null)
            }
            if (channel != null) {
                channel!!.close()
            }
        } catch (var5: Exception) {
        }
    }
}