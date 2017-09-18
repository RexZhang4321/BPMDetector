import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import net.sourceforge.jaad.mp4.MP4Container
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AACFileReader : FileReader {
    private var isBigEndian: Boolean = false
    private val MAX_LEN: Int = 2880000 * 4

    override fun readFile(path: String): Array<FloatArray> {
        val file = RandomAccessFile(path, "r")
        val output = decodeAudio(file)
        return getTwoChannels(output)
    }

    private fun decodeAudio(file: RandomAccessFile): ByteArray {
        val container = MP4Container(file)
        val track = container.movie.tracks[0]
        val decoder = Decoder(track.decoderSpecificInfo)
        val buffer = SampleBuffer()
        val byteArrayOutputStream = ByteArrayOutputStream()
        var maxLen = 0
        while (track.hasMoreFrames() && maxLen <= this.MAX_LEN) {
            decoder.decodeFrame(track.readNextFrame().data, buffer)
            byteArrayOutputStream.write(buffer.data)
            maxLen += buffer.data.size
        }
        this.isBigEndian = buffer.isBigEndian
        return byteArrayOutputStream.toByteArray()
    }

    private fun getTwoChannels(byteArray: ByteArray): Array<FloatArray> {
        val maxLen = minOf(this.MAX_LEN, byteArray.size)
        val left = ByteArray(maxLen / 2)
        val right = ByteArray(maxLen / 2)
        for (i in 0 until maxLen / 2 step 2) {
            left[i] = byteArray[2 * i]
            left[i + 1] = byteArray[2 * i + 1]
            right[i] = byteArray[2 * i + 2]
            right[i + 1] = byteArray[2 * i + 3]
        }
        val byteOrder = if (this.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN
        var sbuf = ByteBuffer.wrap(left).order(byteOrder).asShortBuffer()
        val audioShorts = ShortArray(sbuf.capacity())
        sbuf.get(audioShorts)
        val audioFloats = Array(2) { FloatArray(audioShorts.size) }
        for (i in audioShorts.indices) {
            audioFloats[0][i] = audioShorts[i].toFloat() / 0x8000
        }
        sbuf = ByteBuffer.wrap(right).order(byteOrder).asShortBuffer()
        sbuf.get(audioShorts)
        for (i in audioShorts.indices) {
            audioFloats[1][i] = audioShorts[i].toFloat() / 0x8000
        }
        return audioFloats
    }
}