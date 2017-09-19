import javax.sound.sampled.AudioFormat

interface FileReader {
    fun readFile(path: String): Array<FloatArray>

    fun getAudioFormat(): AudioFormat
}
