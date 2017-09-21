import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

class BPMProcessor : AudioProcessor {
    private val audio: MutableList<Float> = ArrayList<Float>()
    var bpm: Float = 0f

    override fun processingFinished() {
        bpm = BeatCounter.getBPM(audio.toFloatArray())
    }

    override fun process(audioEvent: AudioEvent?): Boolean {
        audio.addAll(audioEvent!!.floatBuffer.toList())
        return true
    }
}